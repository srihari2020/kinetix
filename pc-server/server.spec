# -*- mode: python ; coding: utf-8 -*-
"""
PyInstaller spec file for Kinetix PC Server v3.

Usage:
    pyinstaller server.spec
"""

import os
import sys
from PyInstaller.utils.hooks import collect_all

block_cipher = None

# Collect everything from vgamepad (includes ViGEmClient DLLs)
vgamepad_datas, vgamepad_binaries, vgamepad_hiddenimports = collect_all("vgamepad")
aiortc_datas, aiortc_binaries, aiortc_hiddenimports = collect_all("aiortc")
fastapi_datas, fastapi_binaries, fastapi_hiddenimports = collect_all("fastapi")
uvicorn_datas, uvicorn_binaries, uvicorn_hiddenimports = collect_all("uvicorn")

a = Analysis(
    ["server.py"],
    pathex=[],
    binaries=vgamepad_binaries + aiortc_binaries + fastapi_binaries + uvicorn_binaries,
    datas=[
        ("controller_mapper.py", "."),
        ("device_manager.py", "."),
        ("discovery.py", "."),
        ("logger.py", "."),
        ("tray_icon.py", "."),
        ("api_server.py", "."),
        ("profile_manager.py", "."),
        ("webrtc_server.py", "."),
        ("game_detector.py", "."),
        ("plugin_manager.py", "."),
        ("kinetix_sdk.py", "."),
        ("plugins", "plugins"),
    ] + vgamepad_datas + aiortc_datas + fastapi_datas + uvicorn_datas,
    hiddenimports=[
        "pystray._win32",
        "PIL._tkinter_finder",
        "psutil"
    ] + vgamepad_hiddenimports + aiortc_hiddenimports + fastapi_hiddenimports + uvicorn_hiddenimports,
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[],
    win_no_prefer_redirects=False,
    win_private_assemblies=False,
    cipher=block_cipher,
    noarchive=False,
)

pyz = PYZ(a.pure, a.zipped_data, cipher=block_cipher)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.zipfiles,
    a.datas,
    [],
    name="server",
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    upx_exclude=[],
    runtime_tmpdir=None,
    console=False,
    disable_windowed_traceback=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
)
