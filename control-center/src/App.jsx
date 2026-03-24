import { useState, useEffect } from 'react';
import anime from 'animejs/lib/anime.js';
import './App.css';
import ConnectionOverlay from './components/ConnectionOverlay';
import ServerStatus from './components/ServerStatus';
import ConnectedDevices from './components/ConnectedDevices';
import ControllerMonitor from './components/ControllerMonitor';
import NetworkStats from './components/NetworkStats';
import LogsViewer from './components/LogsViewer';
import logo from './assets/kinetix_logo.svg';

function App() {
  const [isServerReady, setIsServerReady] = useState(false);
  const [connectionError, setConnectionError] = useState(null);
  const [serverState, setServerState] = useState(null);
  const [devices, setDevices] = useState([]);
  const [networkStats, setNetworkStats] = useState({ latency_ms: 0, packet_rate_in: 0, packet_loss_pct: 0 });
  const [logs, setLogs] = useState([]);
  const [liveGamepadData, setLiveGamepadData] = useState({});

  // Backend readiness overlay (WebSocket test until ready)
  useEffect(() => {
    if (isServerReady) return;

    let cancelled = false;
    let timeoutId = null;

    const testConnection = () => {
      if (cancelled) return;

      const ws = new WebSocket('ws://localhost:8765');

      ws.onopen = () => {
        if (!cancelled) {
          setIsServerReady(true);
          setConnectionError(null);
          ws.close();
        }
      };

      ws.onerror = () => {
        if (!cancelled) {
          setConnectionError('Waiting for Python server (WebSocket)...');
          timeoutId = setTimeout(testConnection, 1000);
        }
      };

      ws.onclose = () => {
        // Handle clean close or connection failure
      };
    };

    testConnection();

    return () => {
      cancelled = true;
      if (timeoutId) clearTimeout(timeoutId);
    };
  }, [isServerReady]);

  // Intro animation
  useEffect(() => {
    if (!isServerReady) return;
    
    try {
      const elements = document.querySelectorAll('.dashboard-grid > *');
      if (elements.length === 0) {
        console.warn('No elements found for animation');
        return;
      }
      
      anime({
        targets: '.dashboard-grid > *',
        translateY: [20, 0],
        opacity: [0, 1],
        duration: 800,
        delay: anime.stagger(150),
        easing: 'easeOutQuart'
      });
    } catch (err) {
      console.error('Animation error:', err);
      // Silently fail animation, don't crash the app
    }
  }, [isServerReady]);

  // Fetch static/polling data
  useEffect(() => {
    if (!isServerReady) return;

    let cancelled = false;

    const fetchStatus = async () => {
      const endpoints = [
        { url: 'http://127.0.0.1:8080/status', setter: setServerState, name: 'status' },
        { url: 'http://127.0.0.1:8080/devices', setter: setDevices, name: 'devices' },
        { url: 'http://127.0.0.1:8080/network', setter: setNetworkStats, name: 'network' },
        { url: 'http://127.0.0.1:8080/logs', setter: setLogs, name: 'logs' },
      ];

      const processResponse = async (res, setter, name) => {
        if (res?.ok) {
          try {
            const data = await res.json();
            if (!cancelled) setter(data);
          } catch (err) {
            console.error(`Failed to parse ${name} response:`, err);
          }
        }
      };

      const responses = await Promise.all(
        endpoints.map(ep => fetch(ep.url).catch(err => {
          console.warn(`${ep.name} endpoint failed:`, err.message);
          return null;
        }))
      );

      if (cancelled) return;

      responses.forEach((res, index) => {
        const { setter, name } = endpoints[index];
        processResponse(res, setter, name);
      });
    };

    fetchStatus();
    const interval = setInterval(fetchStatus, 2000);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [isServerReady]);

  // WebSocket for real-time controller updates
  useEffect(() => {
    if (!isServerReady) return;

    let ws = null;
    let reconnectTimeout = null;
    let cancelled = false;

    const connectWs = () => {
      if (cancelled) return;

      try {
        ws = new WebSocket('ws://127.0.0.1:8080/ws/live');

        ws.onopen = () => {
          console.log('WebSocket connected');
        };

        ws.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            if (data.type === 'input' && !cancelled) {
              setLiveGamepadData(prev => ({
                ...prev,
                [data.player]: data.data
              }));
            }
          } catch (e) {
            console.warn('Failed to parse WebSocket message:', e);
          }
        };

        ws.onerror = (error) => {
          console.error('WebSocket error:', error);
        };

        ws.onclose = () => {
          if (!cancelled) {
            console.log('WebSocket disconnected, reconnecting in 3s...');
            reconnectTimeout = setTimeout(connectWs, 3000);
          }
        };
      } catch (err) {
        console.error('Failed to create WebSocket:', err);
        if (!cancelled) {
          reconnectTimeout = setTimeout(connectWs, 3000);
        }
      }
    };

    connectWs();
    return () => {
      cancelled = true;
      if (ws) {
        ws.close();
      }
      if (reconnectTimeout) {
        clearTimeout(reconnectTimeout);
      }
    };
  }, [isServerReady]);

  return (
    <div className="dashboard-wrapper">
      {!isServerReady && <ConnectionOverlay error={connectionError} />}
      <header className="dashboard-header">
        <div className="dashboard-title">
          <img src={logo} alt="Kinetix Logo" width="36" height="36" style={{ filter: 'drop-shadow(0 0 8px rgba(233, 69, 96, 0.6))' }} />
          <span>Kinetix Control Center</span>
        </div>
      </header>

      <div className="dashboard-grid">
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
    </div>
  );
}

export default App;
