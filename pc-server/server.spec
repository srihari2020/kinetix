 # -*- mode: python ; coding: utf-8 -*-

from pathlib import Path
from PyInstaller.utils.hooks import collect_dynamic_libs

hiddenimports = [
    "vgamepad",
    "vgamepad.win",
    "vgamepad.win.vigem_client"
]

# Base binaries collected from vgamepad
binaries = collect_dynamic_libs("vgamepad")

# Explicitly add the ViGEmClient DLL from the venv, equivalent to:
#   --add-binary "venv/Lib/site-packages/vgamepad/win/utils/ViGEmClient.dll;vgamepad/win/utils"
vigem_path = Path("venv/Lib/site-packages/vgamepad/win/utils/ViGEmClient.dll")
if vigem_path.exists():
    binaries.append((str(vigem_path), "vgamepad/win/utils"))

a = Analysis(
    ['server.py'],
    pathex=[],
    binaries=binaries,
    datas=[],
    hiddenimports=hiddenimports,
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[],
    noarchive=False,
    optimize=0,
)
pyz = PYZ(a.pure)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.datas,
    [],
    name='server',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    upx_exclude=[],
    runtime_tmpdir=None,
    console=False,
    disable_windowed_traceback=False,
    argv_emulation=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
)
