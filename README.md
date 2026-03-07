# Kinetix

![Kinetix Banner](https://img.shields.io/badge/Kinetix-Ultimate%20Virtual%20Controller-ff2a5f?style=for-the-badge) ![Platform](https://img.shields.io/badge/Platform-Windows%20|%20Android-4caf50?style=for-the-badge)

Kinetix is an ultra-low latency, highly customizable, and open-source phone-to-PC controller platform. It transforms your Android device into a professional-grade Windows gamepad over your local Wi-Fi network, providing an experience completely indistinguishable from a wired Xbox 360 controller.

**🌍 [Visit the Kinetix Website](https://kinetix-snowy.vercel.app/)** 

---

## ✨ Features

- **120Hz UDP Polling**: Built for speed. Our fast-path networking guarantees instant trigger pulls, analog stick movements, and zero-delay inputs.
- **Native ViGEm Support**: Emulates an Xbox 360 controller via the ViGEmBus driver. 100% compatibility with Steam, Epic Games, Game Pass, and all modern titles. No mapping required!
- **Complete Customization (Layout Designer)**: Tweak every aspect of the controller directly on your phone. Pinch to resize, drag to place, and build the perfect ergonomic layout tailored specifically to your playstyle. 
- **Modern Control Center**: A seamless Electron-based dashboard on Windows to monitor connected devices, backend metrics, and logs in a slick Neon Glassmorphism UI.

---

## 🚀 Installation & Setup

Kinetix is designed to be frictionless.

1. **PC Server**: Download `KinetixSetup.exe` and install the Control Center on your Windows machine.
2. **Android App**: Download `kinetix.apk` to your phone and install the application.
3. **Launch Control Center**: Open the `Kinetix Control Center` on Windows. The ViGEmBus backend will automatically start and expose your local IP.
4. **Connect Controller**: Open the Kinetix Android App, enter your PC's IP address, and tap Connect! You are now ready to play!

---

## 📥 Download

The latest stable binaries are hosted directly on our website:

- [⬇️ Download for Windows](https://kinetix-snowy.vercel.app/downloads/KinetixSetup.exe)
- [⬇️ Download for Android](https://kinetix-snowy.vercel.app/downloads/kinetix.apk)

*(Note: Replace links with authentic Vercel URL once fully deployed)*

---

## 🛠 Project Structure

Kinetix consists of an entire ecosystem seamlessly integrated:
- `/website` - React/Vite landing page serving downloads and marketing.
- `/control-center` - Electron desktop dashboard (UI built in React).
- `/pc-server` - Python fast-path proxy managing UDP and the ViGEmBus native hooks.
- `/android-controller` - The native Android Kotlin client sending inputs.
- `/installer` - Unattended build script bundling the binaries into a smooth user setup.
