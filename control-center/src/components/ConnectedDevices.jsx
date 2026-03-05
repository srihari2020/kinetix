import { Smartphone, Battery, Signal, X } from 'lucide-react';

export default function ConnectedDevices({ devices }) {
    const getAvatarColor = (index) => {
        const colors = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6'];
        return colors[index % colors.length];
    };

    return (
        <div className="panel" style={{ flex: 1, minHeight: 0 }}>
            <div className="panel-header" style={{ display: 'flex', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <Smartphone size={20} />
                    Connected Devices
                </div>
                <span style={{ fontSize: '0.875rem', backgroundColor: 'var(--bg-panel-hover)', padding: '0.1rem 0.5rem', borderRadius: '1rem' }}>
                    {devices.length} / 4
                </span>
            </div>

            <div style={{ overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '0.75rem', marginTop: '0.5rem' }}>
                {devices.length === 0 ? (
                    <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)', textAlign: 'center', padding: '2rem 0' }}>
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
                                <X size={18} />
                            </button>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
}

// Simple fallback icon for Activity
const Activity = ({ size }) => (
    <svg xmlns="http://www.w3.org/2000/svg" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
    </svg>
);
