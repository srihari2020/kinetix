# -*- mode: python ; coding: utf-8 -*-
"""
PyInstaller spec file for Kinetix PC Server.

Usage:
    pyinstaller kinetix-server.spec
"""

import os
import sys
from PyInstaller.utils.hooks import collect_all

block_cipher = None

# Collect everything from vgamepad (includes ViGEmClient DLLs)
vgamepad_datas, vgamepad_binaries, vgamepad_hiddenimports = collect_all("vgamepad")

a = Analysis(
    ["server.py"],
    pathex=[],
    binaries=vgamepad_binaries,
    datas=[
        ("controller_mapper.py", "."),
        ("tray_icon.py", "."),
    ] + vgamepad_datas,
    hiddenimports=[
        "pystray._win32",
        "PIL._tkinter_finder",
    ] + vgamepad_hiddenimports,
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
    name="kinetix-server",
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    upx_exclude=[],
    runtime_tmpdir=None,
    console=False,            # No console window — uses system tray
    disable_windowed_traceback=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
)
