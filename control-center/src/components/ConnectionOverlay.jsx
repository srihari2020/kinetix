import React from 'react';

export default function ConnectionOverlay() {
  return (
    <div className="connection-overlay" role="status" aria-live="polite" aria-busy="true">
      <div className="connection-overlay__card">
        <div className="connection-overlay__spinner" />
        <div className="connection-overlay__title">Connecting to Kinetix Server...</div>
        <div className="connection-overlay__subtitle">Ensuring virtual controller drivers are active.</div>
      </div>
    </div>
  );
}

