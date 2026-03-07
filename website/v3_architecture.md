# Kinetix v3.0 Architecture

Kinetix v3.0 significantly upgrades the platform by introducing a modular plugin system, WebRTC networking, controller profiles, a visual layout editor, and robust network telemetry.

## System Overview

```mermaid
graph TD
    subgraph Android Controller
        A[ControllerActivity] --> B[WebRtcClient / UdpInputSender]
        C[LayoutEditorActivity] --> D[ControllerProfile]
    end

    subgraph PC Server
        B -->|WebRTC / UDP| E[WebRTCManager / UdpProtocol]
        E --> F[PluginManager]
        F --> G[DeviceManager]
        G --> H[ViGEmBus / vgamepad]
        
        I[GameDetector] --> J[ProfileManager]
        J --> F
        
        G --> K[Telemetry / API Server]
    end
    
    subgraph Control Center (React)
        K -->|WebSocket / HTTP| L[Metrics Dashboard]
    end
```

## Key Features

### 1. Plugin System & SDK
Developers can now drop Python scripts into the `plugins/` folder to extend functionality. Plugins can intercept and manipulate controller input before it reaches the virtual gamepad. Example plugins include a Gyro Mouse (controlling the Windows cursor via phone tilt) and a Macro Engine.

### 2. WebRTC Networking
WebRTC DataChannels have been introduced alongside traditional UDP to provide ultra-low latency, peer-to-peer data transfer that easily traverses NATs. The existing WebSocket handles WebRTC signaling (Offers, Answers, and ICE candidates).

### 3. Controller Profiles & Game Detection
Users can save multiple controller profiles (e.g. "Racing", "FPS", "Default") that define layout overrides, gyro sensitivity, and button mappings. The PC server continuously monitors the foreground Windows application via `GameDetector`; if an associated game is launched, Kinetix instantly loads the appropriate profile and syncs it down to the phone.

### 4. Visual Layout Editor
A new Android interface (`LayoutEditorActivity`) allows users to visually drag and reposition on-screen buttons to fit their hands perfectly. The custom layouts are saved to the current Active Profile.

### 5. Telemetry & Control Center
A React-based web dashboard connects to the PC server's API to display real-time connection health. `DeviceManager` calculates instantaneous packet loss and jitter based on sequence numbers and packet pacing, allowing users to accurately diagnose network conditions in real-time.
