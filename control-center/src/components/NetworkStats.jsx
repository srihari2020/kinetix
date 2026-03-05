import { Activity, Wifi } from 'lucide-react';
import { AreaChart, Area, ResponsiveContainer, YAxis, Tooltip } from 'recharts';
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
                <Wifi size={20} /> Network Statistics
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
                    <AreaChart data={history}>
                        <defs>
                            <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.8} />
                                <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                            </linearGradient>
                        </defs>
                        <Tooltip
                            contentStyle={{ backgroundColor: 'var(--bg-panel)', border: '1px solid var(--border-subtle)', borderRadius: '4px' }}
                            itemStyle={{ color: '#fff' }}
                        />
                        <YAxis hide domain={['auto', 'auto']} />
                        <Area type="monotone" dataKey="value" stroke="#3b82f6" fillOpacity={1} fill="url(#colorValue)" strokeWidth={2} />
                    </AreaChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
}
