import { FaNetworkWired } from 'react-icons/fa';
import { AreaChart, Area, ResponsiveContainer, YAxis, Tooltip, CartesianGrid } from 'recharts';
import { useState, useEffect } from 'react';

export default function NetworkStats({ stats }) {
    const [history, setHistory] = useState([]);

    useEffect(() => {
        setHistory(prev => {
            const newHistory = [...prev, { time: new Date().toLocaleTimeString(), value: stats?.packet_rate_in || 0 }];
            if (newHistory.length > 20) newHistory.shift();
            return newHistory;
        });
    }, [stats?.packet_rate_in]);

    return (
        <div className="panel" style={{ flex: 1, minHeight: '220px' }}>
            <div className="panel-header">
                <FaNetworkWired size={20} /> Network Statistics
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 1fr', gap: '1rem', marginBottom: '1.5rem' }}>
                <div style={{ padding: '0.75rem', backgroundColor: 'var(--bg-panel-hover)', borderRadius: 'var(--radius-md)' }}>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Latency</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 600 }}>{stats?.latency_ms?.toFixed(1) || 0} ms</div>
                </div>
                <div style={{ padding: '0.75rem', backgroundColor: 'var(--bg-panel-hover)', borderRadius: 'var(--radius-md)' }}>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Jitter</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 600 }}>{stats?.jitter_ms?.toFixed(1) || 0} ms</div>
                </div>

                <div style={{ padding: '0.75rem', backgroundColor: 'var(--bg-panel-hover)', borderRadius: 'var(--radius-md)' }}>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Packet Rate</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 600 }}>{Math.round(stats?.packet_rate_in || 0)} Hz</div>
                </div>

                <div style={{ padding: '0.75rem', backgroundColor: 'var(--bg-panel-hover)', borderRadius: 'var(--radius-md)' }}>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Packet Loss</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 600, color: stats?.packet_loss_pct > 0 ? 'var(--warning)' : 'var(--success)' }}>
                        {(stats?.packet_loss_pct || 0).toFixed(1)}%
                    </div>
                </div>
            </div>

            <div style={{ flex: 1, width: '100%', minHeight: '60px' }}>
                <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={history} margin={{ top: 10, right: 0, left: 0, bottom: 0 }}>
                        <defs>
                            <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="5%" stopColor="var(--accent-color)" stopOpacity={0.6} />
                                <stop offset="95%" stopColor="var(--accent-color)" stopOpacity={0} />
                            </linearGradient>
                        </defs>
                        <Tooltip
                            contentStyle={{ backgroundColor: 'var(--bg-panel)', border: '1px solid var(--border-subtle)', borderRadius: '8px', boxShadow: '0 4px 20px rgba(0,0,0,0.5)' }}
                            itemStyle={{ color: '#fff', fontWeight: 'bold' }}
                            labelStyle={{ color: 'var(--text-muted)', marginBottom: '4px' }}
                            cursor={{ stroke: 'var(--border-subtle)', strokeWidth: 1, strokeDasharray: '3 3' }}
                        />
                        <YAxis hide domain={['auto', 'auto']} />
                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border-subtle)" opacity={0.5} />
                        <Area type="monotone" dataKey="value" stroke="var(--accent-color)" fillOpacity={1} fill="url(#colorValue)" strokeWidth={3} animationDuration={300} />
                    </AreaChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
}
