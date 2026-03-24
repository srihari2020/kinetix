import React from 'react';

export default function ConnectionOverlay({ error }) {
  return (
    <div className="connection-overlay" role="status" aria-live="polite" aria-busy="true">
      <div className="connection-overlay__card">
        {error ? (
          <>
            <div className="connection-overlay__spinner error" />
            <div className="connection-overlay__title error">Connection Failed</div>
            <div className="connection-overlay__subtitle">{error}</div>
            <div className="connection-overlay__tip">
              Make sure the Python backend server is running. Check the console for more details.
            </div>
          </>
        ) : (
          <>
            <div className="connection-overlay__spinner" />
            <div className="connection-overlay__title">Connecting to Kinetix Server...</div>
            <div className="connection-overlay__subtitle">Ensuring virtual controller drivers are active.</div>
          </>
        )}
      </div>
    </div>
  );
}

