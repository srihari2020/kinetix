# 🎮 Kinetix

> **Next-generation Android-to-PC Gamepad Controller**

Kinetix transforms your Android smartphone into a professional-grade wireless game controller for your PC. Move beyond simple virtual buttons—Kinetix offers competitive latency (up to 120Hz refresh rate), real haptic feedback, customizable dynamic layouts, and physical gyroscope support for precise aiming and steering. 

Whether you need an extra controller for spontaneous split-screen multiplayer, or a dedicated racing wheel utilizing the gyroscope, Kinetix is built for flexibility and performance.

---

## ✨ Features
- **Ultra-Low Latency:** Optimized UDP and WebSocket packet delivery allowing up to 120Hz input polling.
- **Dynamic Layouts:** Customize, drag and drop, and resize your virtual buttons using JSON-driven controller profiles (FPS, Driving, Default, etc.).
- **True Fullscreen Immersion:** Actionbar-free edge-to-edge Android experience for zero distractions.
- **Advanced Feedback:** Enjoy tactile responses with real-time ripple & glow animations, and haptic vibration integration.
- **Gyroscope Integration:** Map your phone's physical movements to analog sticks or triggers for precise steering in racing games or aiming in shooters.
- **Glassmorphism Control Center:** A beautiful, responsive React/Electron dashboard for managing your server, tuning settings, and visualizing controller inputs in real-time.

---

## 🚀 Installation & Setup

### 1. Requirements
- A Windows PC (for Kinetix Server + Control Center)
- An Android Phone (Android 11.0+ recommended)
- **Dependencies:** 
  - [ViGEmBus Driver](https://github.com/ViGEm/ViGEmBus) (Required for translating Android inputs to Windows Xbox 360 controller inputs).
  - Node.js (for building/running the PC Control Center).
  - Python 3.10+ (for the PC backend server).

### 2. PC Server Setup
1. **Clone the repository:**
   ```bash
   git clone https://github.com/YOUR_USERNAME/Kinetix.git
   cd Kinetix
   ```
2. **Install Python dependencies:**
   ```bash
   cd pc-server
   pip install -r requirements.txt
   ```
3. **Run the Kinetix Server:**
   ```bash
   python server.py
   ```
4. **Launch the Control Center Dashboard:**
   Open a new terminal session.
   ```bash
   cd control-center
   npm install
   npm run dev
   ```
   *(Or build the production version with `npm run build` and run electron directly)*

### 3. Android App Setup
1. Open the `/android-controller` directory in **Android Studio**.
2. Sync the Gradle project and build the APK.
3. Install the APK on your Android device.
4. Make sure your phone and PC are on the **same Wi-Fi network**.
5. Launch the Kinetix app on your phone. It will automatically scan the LAN for your PC Server, or you can manually enter the PC's IP address.

---

## 🛠 Build Instructions
The project is split into three main parts:
1. `pc-server/`: Python backend utilizing `fastapi`, `websockets`, `uvicorn`, and `vgamepad`.
2. `control-center/`: React + Vite + Electron dashboard.
3. `android-controller/`: Native Android app written in Kotlin.

---

## 📝 License
Distributed under the MIT License. See `LICENSE` for more information.
