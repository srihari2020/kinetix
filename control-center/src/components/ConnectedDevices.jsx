import { FaMobileAlt, FaTimes } from 'react-icons/fa';
import { Activity } from 'lucide-react'; // kept locally below

export default function ConnectedDevices({ devices }) {
    const getAvatarColor = (index) => {
        const colors = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6'];
        return colors[index % colors.length];
    };

    return (
        <div className="panel" style={{ flex: 1, minHeight: 0 }}>
            <div className="panel-header" style={{ display: 'flex', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <FaMobileAlt size={20} />
                    Connected Devices
                </div>
                <span style={{ fontSize: '0.875rem', backgroundColor: 'var(--bg-panel-hover)', padding: '0.1rem 0.5rem', borderRadius: '1rem' }}>
                    {devices.length} / 4
                </span>
            </div>

            <div style={{ overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '0.75rem', marginTop: '0.5rem', flex: 1 }}>
                {devices.length === 0 ? (
                    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)', textAlign: 'center', padding: '2rem 1rem' }}>
                        <FaMobileAlt size={48} style={{ opacity: 0.15, marginBottom: '1rem' }} />
                        No devices connected.<br />Enter the PC's IP in the Kinetix app.
                    </div>
                ) : (
                    devices.map((device, idx) => (
                        <div key={device.device_id || idx} style={{
                            display: 'flex',
                            alignItems: 'center',
                            padding: '0.75rem',
                            backgroundColor: 'var(--bg-panel-hover)',
                            borderRadius: 'var(--radius-md)',
                            border: '1px solid var(--border-subtle)',
                            gap: '1rem'
                        }}>
                            <div style={{
                                width: '32px', height: '32px',
                                borderRadius: '50%',
                                backgroundColor: getAvatarColor(device.player_index),
                                display: 'flex', alignItems: 'center', justifyContent: 'center',
                                fontWeight: 'bold', fontSize: '1rem', color: 'white'
                            }}>
                                P{device.player_index + 1}
                            </div>

                            <div style={{ flex: 1, overflow: 'hidden' }}>
                                <div style={{ fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                    {device.device_name}
                                </div>
                                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'flex', gap: '0.5rem' }}>
                                    <span>{device.ip_address}</span>
                                    <span style={{ display: 'flex', alignItems: 'center', gap: '0.15rem' }}>
                                        <Activity size={12} /> {device.packets_per_sec} Hz
                                    </span>
                                </div>
                            </div>

                            <button className="btn-outline" style={{ padding: '0.25rem', border: 'none', color: 'var(--danger)' }} title="Disconnect">
                                <FaTimes size={18} />
                            </button>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
}

// Activity imported via lucide-react
