import { FaTerminal } from 'react-icons/fa';
import { useEffect, useRef } from 'react';

export default function LogsViewer({ logs }) {
    const scrollRef = useRef(null);

    // Auto-scroll to bottom when logs update
    useEffect(() => {
        if (scrollRef.current) {
            scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
        }
    }, [logs]);

    return (
        <div className="panel" style={{ flex: '1.5', minHeight: 0, display: 'flex', flexDirection: 'column' }}>
            <div className="panel-header">
                <FaTerminal size={20} /> Server Logs
            </div>

            <div
                ref={scrollRef}
                style={{
                    flex: 1,
                    overflowY: 'auto',
                    backgroundColor: '#000',
                    padding: '1rem',
                    borderRadius: 'var(--radius-md)',
                    fontFamily: 'monospace',
                    fontSize: '0.8rem',
                    color: '#d4d4d8',
                    lineHeight: '1.4'
                }}
            >
                {(!logs || logs.length === 0) ? (
                    <div style={{ color: 'var(--text-muted)' }}>Waiting for logs...</div>
                ) : (
                    logs.map((log, i) => {
                        // Very simple syntax highlighting for log levels
                        let color = '#d4d4d8';
                        if (log.includes('WARNING')) color = '#f59e0b';
                        if (log.includes('ERROR')) color = '#ef4444';
                        if (log.includes('INFO')) color = '#3b82f6';
                        if (log.includes('DEBUG')) color = '#9ca3af';

                        return (
                            <div key={i} style={{ color, marginBottom: '0.2rem', wordBreak: 'break-all' }}>
                                {log}
                            </div>
                        );
                    })
                )}
            </div>
        </div>
    );
}
