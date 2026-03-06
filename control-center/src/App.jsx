import { useState, useEffect } from 'react';
import './App.css';
import ServerStatus from './components/ServerStatus';
import ConnectedDevices from './components/ConnectedDevices';
import ControllerMonitor from './components/ControllerMonitor';
import NetworkStats from './components/NetworkStats';
import LogsViewer from './components/LogsViewer';
import { FaGamepad } from 'react-icons/fa';

function App() {
  const [serverState, setServerState] = useState(null);
  const [devices, setDevices] = useState([]);
  const [networkStats, setNetworkStats] = useState({ latency_ms: 0, packet_rate_in: 0, packet_loss_pct: 0 });
  const [logs, setLogs] = useState([]);
  const [liveGamepadData, setLiveGamepadData] = useState({});

  // Fetch static/polling data
  useEffect(() => {
    const fetchStatus = async () => {
      try {
        const [statusRes, devicesRes, networkRes, logsRes] = await Promise.all([
          fetch('http://127.0.0.1:8080/status').catch(() => null),
          fetch('http://127.0.0.1:8080/devices').catch(() => null),
          fetch('http://127.0.0.1:8080/network').catch(() => null),
          fetch('http://127.0.0.1:8080/logs').catch(() => null),
        ]);

        if (statusRes?.ok) setServerState(await statusRes.json());
        if (devicesRes?.ok) setDevices(await devicesRes.json());
        if (networkRes?.ok) setNetworkStats(await networkRes.json());
        if (logsRes?.ok) setLogs(await logsRes.json());
      } catch (err) {
        console.error("Polling error:", err);
      }
    };

    fetchStatus();
    const interval = setInterval(fetchStatus, 2000);
    return () => clearInterval(interval);
  }, []);

  // WebSocket for real-time controller updates
  useEffect(() => {
    let ws = null;
    let reconnectTimeout = null;

    const connectWs = () => {
      ws = new WebSocket('ws://127.0.0.1:8080/ws/live');

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          if (data.type === 'input') {
            setLiveGamepadData(prev => ({
              ...prev,
              [data.player]: data.data
            }));
          }
        } catch (e) {
          // ignore parsing error
        }
      };

      ws.onclose = () => {
        reconnectTimeout = setTimeout(connectWs, 3000);
      };
    };

    connectWs();
    return () => {
      if (ws) ws.close();
      if (reconnectTimeout) clearTimeout(reconnectTimeout);
    };
  }, []);

  return (
    <div className="dashboard-grid">
      <header className="dashboard-header">
        <div className="dashboard-title">
          <img src="/kinetix_logo.svg" alt="Kinetix Logo" width="36" height="36" style={{ filter: 'drop-shadow(0 0 8px rgba(233, 69, 96, 0.6))' }} />
          <span>Kinetix Control Center</span>
        </div>
        <div className="dashboard-actions">
          {/* Global actions could go here */}
        </div>
      </header>

      <aside className="dashboard-left">
        <ServerStatus state={serverState} />
        <ConnectedDevices devices={devices} />
      </aside>

      <main className="dashboard-main">
        <ControllerMonitor liveData={liveGamepadData} devices={devices} />
      </main>

      <aside className="dashboard-right">
        <NetworkStats stats={networkStats} />
        <LogsViewer logs={logs} />
      </aside>
    </div>
  );
}

export default App;
