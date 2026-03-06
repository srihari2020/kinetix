import { FaServer, FaPlay, FaSquare, FaCog } from 'react-icons/fa';

export default function ServerStatus({ state }) {
    const isRunning = state?.status === 'running';

    return (
        <div className="panel server-status">
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
                    <button className="btn-danger" style={{ flex: 1, display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '0.5rem' }}>
                        <FaSquare size={16} /> Stop Server
                    </button>
                ) : (
                    <button className="btn-primary" style={{ flex: 1, display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '0.5rem' }}>
                        <FaPlay size={16} /> Start Server
                    </button>
                )}
                <button className="btn-outline" style={{ padding: '0.5rem' }} title="Server Settings">
                    <FaCog size={20} />
                </button>
            </div>
        </div>
    );
}
