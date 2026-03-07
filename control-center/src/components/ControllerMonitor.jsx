import { FaGamepad } from 'react-icons/fa';
import { useState, useEffect, useRef } from 'react';
import * as anime from 'animejs';

// Controller visualizer component
export default function ControllerMonitor({ liveData, devices }) {
    const [activeTab, setActiveTab] = useState(0);
    const prevButtonsRef = useRef(0);

    // Default neutral input
    const input = liveData[activeTab] || {
        lx: 0, ly: 0,
        rx: 0, ry: 0,
        lt: 0, rt: 0,
        buttons: 0,
        dpad: 0
    };

    useEffect(() => {
        // Trigger button press animation when a new button goes down
        const changed = input.buttons ^ prevButtonsRef.current;
        const pressed = changed & input.buttons;

        if (pressed > 0) {
            anime({
                targets: '.action-btn.active',
                scale: [0.8, 1.2, 1],
                duration: 300,
                easing: 'easeOutElastic(1, .8)'
            });
        }
        prevButtonsRef.current = input.buttons;
    }, [input.buttons]);

    const getJoystickTransform = (x, y) => {
        // scale -32768..32767 to -15px..15px
        const px = (x / 32767) * 15;
        const py = -(y / 32767) * 15; // Invert Y visually
        return `translate(${px}px, ${py}px)`;
    };

    const isBtnPressed = (btnMask, flag) => {
        return (btnMask & flag) !== 0;
    };

    return (
        <div className="panel" style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
            <div className="panel-header" style={{ marginBottom: '0.5rem' }}>
                <FaGamepad size={20} /> Controller Monitor
            </div>

            {/* Player Tabs */}
            <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.5rem' }}>
                {[0, 1, 2, 3].map((slot) => {
                    const isActive = activeTab === slot;
                    const dev = devices.find(d => d.player_index === slot);
                    return (
                        <button key={slot}
                            onClick={() => setActiveTab(slot)}
                            style={{
                                background: isActive ? 'var(--bg-panel-hover)' : 'transparent',
                                color: isActive ? 'white' : 'var(--text-muted)',
                                borderBottom: isActive ? '2px solid var(--accent-color)' : '2px solid transparent',
                                borderRadius: 'var(--radius-sm) var(--radius-sm) 0 0',
                                padding: '0.5rem 1rem'
                            }}>
                            {dev ? `P${slot + 1}: ${dev.device_name}` : `Slot ${slot + 1} (Empty)`}
                        </button>
                    );
                })}
            </div>

            {/* Controller Visualization */}
            <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'rgba(0,0,0,0.2)', borderRadius: 'var(--radius-lg)', position: 'relative', overflow: 'hidden' }}>
                <svg viewBox="0 0 800 500" style={{ width: '100%', height: '100%', maxHeight: '400px' }}>
                    {/* Controller Body Base */}
                    <path d="M 150 50 L 650 50 C 750 50 800 150 800 250 C 800 450 650 450 600 350 L 500 250 L 300 250 L 200 350 C 150 450 0 450 0 250 C 0 150 50 50 150 50 Z"
                        fill="#1f2937" stroke="var(--border-subtle)" strokeWidth="4" />

                    {/* Triggers LT / RT */}
                    <rect x="150" y="20" width="80" height="30" rx="4" fill="#374151" />
                    <rect x="150" y={20 + 30 - ((input.lt / 65535) * 30)} width="80" height={(input.lt / 65535) * 30} rx="4" fill="var(--accent-color)" />

                    <rect x="570" y="20" width="80" height="30" rx="4" fill="#374151" />
                    <rect x="570" y={20 + 30 - ((input.rt / 65535) * 30)} width="80" height={(input.rt / 65535) * 30} rx="4" fill="var(--accent-color)" />

                    {/* D-PAD */}
                    <g transform="translate(250, 280)">
                        <rect x="-40" y="-15" width="80" height="30" fill="#111" rx="4" />
                        <rect x="-15" y="-40" width="30" height="80" fill="#111" rx="4" />

                        <rect x="-15" y="-40" width="30" height="25" fill={[1, 5, 6].includes(input.dpad) ? 'var(--accent-color)' : '#333'} rx="2" />
                        <rect x="-15" y="15" width="30" height="25" fill={[2, 7, 8].includes(input.dpad) ? 'var(--accent-color)' : '#333'} rx="2" />
                        <rect x="-40" y="-15" width="25" height="30" fill={[3, 5, 7].includes(input.dpad) ? 'var(--accent-color)' : '#333'} rx="2" />
                        <rect x="15" y="-15" width="25" height="30" fill={[4, 6, 8].includes(input.dpad) ? 'var(--accent-color)' : '#333'} rx="2" />
                    </g>

                    {/* Left Joystick */}
                    <g transform="translate(190, 160)">
                        <circle cx="0" cy="0" r="50" fill="#111" />
                        <circle cx={(input.lx / 32767) * 20} cy={-(input.ly / 32767) * 20} r="35" fill="#4b5563" style={{ transition: 'all 0.05s linear' }} />
                    </g>

                    {/* Right Joystick */}
                    <g transform="translate(510, 280)">
                        <circle cx="0" cy="0" r="50" fill="#111" />
                        <circle cx={(input.rx / 32767) * 20} cy={-(input.ry / 32767) * 20} r="35" fill="#4b5563" style={{ transition: 'all 0.05s linear' }} />
                    </g>

                    {/* Action Buttons */}
                    <g transform="translate(610, 160)">
                        {/* Y */}
                        <circle className={`action-btn ${isBtnPressed(input.buttons, 8) ? 'active' : ''}`} cx="0" cy="-45" r="20" fill={isBtnPressed(input.buttons, 8) ? '#facc15' : '#333'} />
                        {/* X */}
                        <circle className={`action-btn ${isBtnPressed(input.buttons, 4) ? 'active' : ''}`} cx="-45" cy="0" r="20" fill={isBtnPressed(input.buttons, 4) ? '#3b82f6' : '#333'} />
                        {/* B */}
                        <circle className={`action-btn ${isBtnPressed(input.buttons, 2) ? 'active' : ''}`} cx="45" cy="0" r="20" fill={isBtnPressed(input.buttons, 2) ? '#ef4444' : '#333'} />
                        {/* A */}
                        <circle className={`action-btn ${isBtnPressed(input.buttons, 1) ? 'active' : ''}`} cx="0" cy="45" r="20" fill={isBtnPressed(input.buttons, 1) ? '#22c55e' : '#333'} />
                    </g>
                </svg>
            </div>
        </div>
    );
}
