import { FaServer, FaPlay, FaSquare, FaCog, FaTimes } from 'react-icons/fa';
import { useState } from 'react';

export default function ServerStatus({ state }) {
    const isRunning = state?.status === 'running';
    const [showSettings, setShowSettings] = useState(false);

    const handleStopServer = async () => {
        try {
            await fetch('http://127.0.0.1:8080/shutdown', { method: 'POST' });
        } catch (err) {
            console.error('Failed to stop server', err);
        }
    };

    return (
        <div className="panel server-status" style={{ position: 'relative' }}>
            <div className="panel-header">
                <FaServer size={20} className={isRunning ? 'active' : ''} />
                Server Status
            </div>

            <div style={{ marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '1rem' }}>
                <div className={`status-dot ${isRunning ? 'active' : 'offline'}`}></div>
                <div style={{ fontSize: '1.25rem', fontWeight: 600 }}>
                    {isRunning ? 'Online' : 'Offline'}
                </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'auto 1fr', gap: '0.75rem 1.5rem', marginBottom: '1.5rem' }}>
                <div style={{ color: 'var(--text-muted)' }}>IP Address</div>
                <div style={{ fontWeight: 500 }}>{state?.ip || '---'}</div>

                <div style={{ color: 'var(--text-muted)' }}>WebSockets</div>
                <div style={{ fontWeight: 500, color: 'var(--info)' }}>Port {state?.ws_port || '---'}</div>

                <div style={{ color: 'var(--text-muted)' }}>UDP Input</div>
                <div style={{ fontWeight: 500, color: 'var(--warning)' }}>Port {state?.udp_port || '---'}</div>
            </div>

            <div style={{ display: 'flex', gap: '0.75rem', marginTop: 'auto' }}>
                {isRunning ? (
                    <button onClick={handleStopServer} className="btn-danger" style={{ flex: 1, display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '0.5rem' }}>
                        <FaSquare size={16} /> Stop Server
                    </button>
                ) : (
                    <button className="btn-primary" style={{ flex: 1, display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '0.5rem' }}>
                        <FaPlay size={16} /> Start Server
                    </button>
                )}
                <button onClick={() => setShowSettings(true)} className="btn-outline" style={{ padding: '0.5rem' }} title="Server Settings">
                    <FaCog size={20} />
                </button>
            </div>

            {/* Settings Modal */}
            {showSettings && (
                <div style={{
                    position: 'absolute', top: 0, left: 0, right: 0, bottom: 0,
                    backgroundColor: 'rgba(20, 25, 40, 0.95)',
                    backdropFilter: 'blur(12px)',
                    borderRadius: 'var(--radius-lg)',
                    padding: '1.5rem',
                    zIndex: 10,
                    display: 'flex',
                    flexDirection: 'column'
                }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                        <h3 style={{ margin: 0, color: '#fff' }}>Server Settings</h3>
                        <button onClick={() => setShowSettings(false)} className="btn-outline" style={{ padding: '0.5rem', border: 'none' }}>
                            <FaTimes size={16} />
                        </button>
                    </div>
                    <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                        <div>
                            <label style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-muted)', fontSize: '0.9rem' }}>WebSockets Port</label>
                            <input type="number" defaultValue="8765" style={{ width: '100%', padding: '0.75rem', borderRadius: '4px', backgroundColor: '#0f172a', border: '1px solid var(--border-subtle)', color: '#fff' }} disabled />
                        </div>
                        <div>
                            <label style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-muted)', fontSize: '0.9rem' }}>UDP Input Port</label>
                            <input type="number" defaultValue="5743" style={{ width: '100%', padding: '0.75rem', borderRadius: '4px', backgroundColor: '#0f172a', border: '1px solid var(--border-subtle)', color: '#fff' }} disabled />
                        </div>
                        <div>
                            <label style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-muted)', fontSize: '0.9rem' }}>Device Timeout (s)</label>
                            <input type="number" defaultValue="10" style={{ width: '100%', padding: '0.75rem', borderRadius: '4px', backgroundColor: '#0f172a', border: '1px solid var(--border-subtle)', color: '#fff' }} disabled />
                        </div>
                        <div style={{ marginTop: 'auto', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                            Settings update requires restart
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
