import { Gamepad2 } from 'lucide-react';
import { useState } from 'react';

// Controller visualizer component
export default function ControllerMonitor({ liveData, devices }) {
    const [activeTab, setActiveTab] = useState(0);

    // Default neutral input
    const input = liveData[activeTab] || {
        lx: 0, ly: 0,
        rx: 0, ry: 0,
        lt: 0, rt: 0,
        buttons: 0,
        dpad: 0
    };

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
                <Gamepad2 size={20} /> Controller Monitor
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
            <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'rgba(0,0,0,0.2)', borderRadius: 'var(--radius-lg)', position: 'relative' }}>
                <div style={{
                    width: '500px', height: '300px',
                    border: '2px solid var(--border-subtle)',
                    borderRadius: '150px 150px 100px 100px',
                    position: 'relative',
                    backgroundColor: '#1f2937',
                    boxShadow: 'inset 0 10px 30px rgba(0,0,0,0.5)'
                }}>
                    {/* Triggers (LT / RT) */}
                    <div style={{
                        position: 'absolute', top: '-15px', left: '80px', width: '60px', height: '20px',
                        backgroundColor: '#374151', borderRadius: '4px', overflow: 'hidden'
                    }}>
                        <div style={{ width: '100%', height: `${(input.lt / 65535) * 100}%`, backgroundColor: 'var(--accent-color)', position: 'absolute', bottom: 0 }} />
                    </div>
                    <div style={{
                        position: 'absolute', top: '-15px', right: '80px', width: '60px', height: '20px',
                        backgroundColor: '#374151', borderRadius: '4px', overflow: 'hidden'
                    }}>
                        <div style={{ width: '100%', height: `${(input.rt / 65535) * 100}%`, backgroundColor: 'var(--accent-color)', position: 'absolute', bottom: 0 }} />
                    </div>

                    {/* D-PAD */}
                    <div className={`dpad ${input.dpad > 0 ? 'active' : ''}`} style={{ position: 'absolute', bottom: '80px', left: '120px', width: '50px', height: '50px', backgroundColor: '#111', borderRadius: '5px' }}>
                        {/* Simplified dpad visual */}
                        <div style={{ position: 'absolute', top: 0, left: '15px', width: '20px', height: '15px', backgroundColor: [1, 5, 6].includes(input.dpad) ? 'var(--accent-color)' : '#333' }} />
                        <div style={{ position: 'absolute', bottom: 0, left: '15px', width: '20px', height: '15px', backgroundColor: [2, 7, 8].includes(input.dpad) ? 'var(--accent-color)' : '#333' }} />
                        <div style={{ position: 'absolute', left: 0, top: '15px', width: '15px', height: '20px', backgroundColor: [3, 5, 7].includes(input.dpad) ? 'var(--accent-color)' : '#333' }} />
                        <div style={{ position: 'absolute', right: 0, top: '15px', width: '15px', height: '20px', backgroundColor: [4, 6, 8].includes(input.dpad) ? 'var(--accent-color)' : '#333' }} />
                    </div>

                    {/* Left Joystick */}
                    <div style={{ position: 'absolute', top: '80px', left: '80px', width: '60px', height: '60px', backgroundColor: '#111', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <div style={{
                            width: '36px', height: '36px', backgroundColor: '#4b5563', borderRadius: '50%',
                            transform: getJoystickTransform(input.lx, input.ly),
                            transition: 'transform 0.05s linear',
                            boxShadow: '0 4px 6px rgba(0,0,0,0.5)'
                        }} />
                    </div>

                    {/* Right Joystick */}
                    <div style={{ position: 'absolute', bottom: '70px', right: '150px', width: '60px', height: '60px', backgroundColor: '#111', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <div style={{
                            width: '36px', height: '36px', backgroundColor: '#4b5563', borderRadius: '50%',
                            transform: getJoystickTransform(input.rx, input.ry),
                            transition: 'transform 0.05s linear',
                            boxShadow: '0 4px 6px rgba(0,0,0,0.5)'
                        }} />
                    </div>

                    {/* Action Buttons */}
                    <div style={{ position: 'absolute', top: '70px', right: '70px', width: '80px', height: '80px', position: 'relative' }}>
                        {/* Y */}
                        <div style={{ position: 'absolute', top: '-10px', left: '30px', width: '24px', height: '24px', borderRadius: '50%', backgroundColor: isBtnPressed(input.buttons, 8) ? '#facc15' : '#333' }} />
                        {/* X */}
                        <div style={{ position: 'absolute', top: '25px', left: '-5px', width: '24px', height: '24px', borderRadius: '50%', backgroundColor: isBtnPressed(input.buttons, 4) ? '#3b82f6' : '#333' }} />
                        {/* B */}
                        <div style={{ position: 'absolute', top: '25px', right: '-15px', width: '24px', height: '24px', borderRadius: '50%', backgroundColor: isBtnPressed(input.buttons, 2) ? '#ef4444' : '#333' }} />
                        {/* A */}
                        <div style={{ position: 'absolute', bottom: '-5px', left: '30px', width: '24px', height: '24px', borderRadius: '50%', backgroundColor: isBtnPressed(input.buttons, 1) ? '#22c55e' : '#333' }} />
                    </div>
                </div>
            </div>
        </div>
    );
}
