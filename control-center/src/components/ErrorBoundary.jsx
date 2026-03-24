import React from 'react';
import './ErrorBoundary.css';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
    };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    console.error('Frontend Error Caught:', error, errorInfo);
    this.setState({
      error,
      errorInfo,
    });
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="error-boundary-container">
          <div className="error-boundary-content">
            <h1>⚠️ Application Error</h1>
            <p className="error-message">The Kinetix Control Center encountered an unexpected error:</p>
            <div className="error-details">
              <p className="error-name">{this.state.error?.toString()}</p>
              {this.state.errorInfo && (
                <details className="error-stack">
                  <summary>Stack Trace</summary>
                  <pre>{this.state.errorInfo.componentStack}</pre>
                </details>
              )}
            </div>
            <div className="error-actions">
              <button onClick={() => window.location.reload()}>Reload App</button>
              <button onClick={() => this.setState({ hasError: false, error: null, errorInfo: null })}>
                Dismiss
              </button>
            </div>
            <p className="error-help">
              Check the console (DevTools) for detailed error information. 
              Ensure the Python backend server is running on port 8080.
            </p>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
