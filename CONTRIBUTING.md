# Contributing to Kinetix

Thank you for your interest in contributing! 🎮

## How to Contribute

### Reporting Bugs

1. Check existing [Issues](../../issues) to avoid duplicates.
2. Open a new issue with:
   - Steps to reproduce
   - Expected vs. actual behaviour
   - Device info (Android version, PC OS, Python version)

### Suggesting Features

Open an issue with the **enhancement** label describing:
- The use case
- Proposed solution
- Alternatives considered

### Submitting Code

1. **Fork** the repo and create a branch from `main`:
   ```bash
   git checkout -b feature/my-feature
   ```
2. Make your changes.
3. Test locally:
   - **Python server:** `python server.py --no-tray` starts without errors.
   - **Android app:** builds with `./gradlew assembleDebug`.
4. Commit with a descriptive message:
   ```bash
   git commit -m "feat: add gyroscope support"
   ```
5. Push and open a **Pull Request**.

### Commit Message Convention

We follow [Conventional Commits](https://www.conventionalcommits.org/):

| Prefix | Usage |
|---|---|
| `feat:` | New feature |
| `fix:` | Bug fix |
| `docs:` | Documentation only |
| `refactor:` | Code change that is neither a fix nor a feature |
| `chore:` | Build, CI, or tool changes |

## Development Setup

### PC Server

```powershell
cd pc-server
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python server.py --no-tray
```

### Android App

1. Open `android-controller/` in Android Studio.
2. Sync Gradle → Run on a device.

## Code Style

- **Python:** Follow PEP 8. Use type hints where practical.
- **Kotlin:** Follow official [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).
