import React, { useEffect, useRef } from 'react';
import anime from 'animejs';
import { FaWindows, FaAndroid, FaGithub, FaWifi, FaBolt, FaGamepad, FaPaintBrush } from 'react-icons/fa';
import './index.css';

export default function App() {
  const svgRef = useRef(null);

  useEffect(() => {
    // Float animation for the hero visualizer
    anime({
      targets: '.svg-float',
      translateY: ['-15px', '15px'],
      direction: 'alternate',
      loop: true,
      easing: 'easeInOutSine',
      duration: 2000
    });

    // Sub-elements pulse
    anime({
      targets: '.visualizer-glow',
      opacity: [0.3, 0.8],
      scale: [0.9, 1.1],
      direction: 'alternate',
      loop: true,
      easing: 'easeInOutSine',
      duration: 1500
    });
  }, []);

  return (
    <>
      <nav>
        <div className="container nav-content">
          <div className="logo text-gradient">KINETIX</div>
          <div className="nav-links">
            <a href="#features">Features</a>
            <a href="#download">Download</a>
            <a href="https://github.com/srihari2020/kinetix" target="_blank" rel="noreferrer">GitHub</a>
          </div>
        </div>
      </nav>

      <main>
        {/* HERO */}
        <section className="hero">
          <div className="hero-bg-glow" />
          <div className="container hero-grid">

            <div className="hero-content">
              <h1><span className="text-gradient">Ultimate</span> Virtual Controller</h1>
              <p>Transform your Android device into a low-latency, fully customizable PC gamepad over Wi-Fi.</p>
              <div className="hero-actions">
                <a href="#download" className="btn-neon">
                  <FaWindows size={20} /> Get Control Center
                </a>
                <a href="#download" className="btn-outline">
                  <FaAndroid size={20} /> Get Android App
                </a>
              </div>
            </div>

            <div className="hero-visualizer">
              <svg className="svg-float" viewBox="0 0 800 500" ref={svgRef}>
                <defs>
                  <filter id="glow">
                    <feGaussianBlur stdDeviation="8" result="coloredBlur" />
                    <feMerge>
                      <feMergeNode in="coloredBlur" />
                      <feMergeNode in="SourceGraphic" />
                    </feMerge>
                  </filter>
                </defs>
                <path d="M 150 50 L 650 50 C 750 50 800 150 800 250 C 800 450 650 450 600 350 L 500 250 L 300 250 L 200 350 C 150 450 0 450 0 250 C 0 150 50 50 150 50 Z"
                  fill="rgba(255,255,255,0.03)" stroke="rgba(255,255,255,0.1)" strokeWidth="2" style={{ backdropFilter: 'blur(10px)' }} />

                {/* Visual accents */}
                <circle cx="200" cy="180" r="45" fill="var(--accent-neon)" className="visualizer-glow" filter="url(#glow)" />
                <circle cx="600" cy="280" r="45" fill="var(--accent-neon)" className="visualizer-glow" filter="url(#glow)" />

                <g fill="rgba(255,255,255,0.2)">
                  <rect x="250" y="270" width="80" height="20" rx="4" />
                  <rect x="280" y="240" width="20" height="80" rx="4" />
                </g>
                <g fill="rgba(255,255,255,0.2)" transform="translate(600, 150)">
                  <circle cx="0" cy="-35" r="15" />
                  <circle cx="-35" cy="0" r="15" />
                  <circle cx="35" cy="0" r="15" />
                  <circle cx="0" cy="35" r="15" />
                </g>
              </svg>
            </div>

          </div>
        </section>

        {/* FEATURES */}
        <section id="features" className="features-section">
          <div className="container">
            <h2 className="section-title">Why Kinetix?</h2>
            <div className="features-grid">

              <div className="glass-panel feature-card">
                <FaBolt className="feature-icon" />
                <h3>120Hz Polling</h3>
                <p>Bare-metal UDP streaming ensures hyper-fast input delivery indistinguishable from a wired gamepad.</p>
              </div>

              <div className="glass-panel feature-card">
                <FaPaintBrush className="feature-icon" />
                <h3>Layout Designer</h3>
                <p>Fully customize your controller on your phone. Drag, resize, add or remove buttons to fit your playstyle.</p>
              </div>

              <div className="glass-panel feature-card">
                <FaGamepad className="feature-icon" />
                <h3>Native ViGEm Support</h3>
                <p>Your PC recognizes Kinetix seamlessly as a native Xbox 360 controller. Compatible with 100% of modern games.</p>
              </div>

            </div>
          </div>
        </section>

        {/* DOWNLOAD */}
        <section id="download" className="features-section" style={{ background: 'rgba(255,255,255,0.02)' }}>
          <div className="container" style={{ textAlign: 'center' }}>
            <h2 className="section-title">Start Playing</h2>
            <div style={{ display: 'flex', gap: '2rem', justifyContent: 'center', flexWrap: 'wrap' }}>

              <div className="glass-panel" style={{ flex: '1', minWidth: '300px', maxWidth: '400px' }}>
                <FaWindows size={48} style={{ marginBottom: '1.5rem', opacity: 0.8 }} />
                <h3 style={{ marginBottom: '1rem' }}>PC Server</h3>
                <p style={{ color: 'var(--text-muted)', marginBottom: '2rem' }}>All-in-one Electron dashboard and ViGEmBus backend.</p>
                <a href="https://github.com/srihari2020/kinetix/releases/download/v1.4.0/kinetix-pc.zip" target="_blank" rel="noreferrer" className="btn-neon" style={{ width: '100%', justifyContent: 'center' }}>Download .ZIP</a>
              </div>

              <div className="glass-panel" style={{ flex: '1', minWidth: '300px', maxWidth: '400px' }}>
                <FaAndroid size={48} style={{ marginBottom: '1.5rem', opacity: 0.8 }} />
                <h3 style={{ marginBottom: '1rem' }}>Android App</h3>
                <p style={{ color: 'var(--text-muted)', marginBottom: '2rem' }}>The ultra-responsive controller client for your phone.</p>
                <a href="https://github.com/srihari2020/kinetix/releases/download/v1.4.0/kinetix.apk" target="_blank" rel="noreferrer" className="btn-outline" style={{ width: '100%', justifyContent: 'center' }}>Download .APK</a>
              </div>

            </div>
          </div>
        </section>

      </main>

      <footer style={{ padding: '3rem 0', textAlign: 'center', color: 'var(--text-muted)', borderTop: '1px solid var(--glass-border)' }}>
        <p>Open Source Project. Designed and built with Kinetix.</p>
        <a href="https://github.com/srihari2020/kinetix" style={{ color: 'var(--text-main)', marginTop: '1rem', display: 'inline-block' }}><FaGithub size={24} /></a>
      </footer>
    </>
  );
}
