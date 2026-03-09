; ──────────────────────────────────────────────────────────────────────
;  Kinetix Control Center v3.0 – Inno Setup Installer Script
;
;  Prerequisites:
;   1. Run build-all.bat (builds Server & Electron app)
;   2. Place ViGEmBus_Setup_x64.msi in installer/prereqs/
;   3. Compile with Inno Setup 6+
; ──────────────────────────────────────────────────────────────────────

[Setup]
AppName=Kinetix Control Center
AppVersion=3.0.0
AppPublisher=Kinetix Contributors
AppPublisherURL=https://github.com/kinetix-controller
DefaultDirName={autopf}\Kinetix Control Center
DefaultGroupName=Kinetix
UninstallDisplayIcon={app}\Kinetix Control Center.exe
OutputDir=..\dist
OutputBaseFilename=KinetixSetup
Compression=lzma2
SolidCompression=yes
ArchitecturesInstallIn64BitMode=x64compatible
WizardStyle=modern
PrivilegesRequired=admin

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
; The unpacked Electron application
Source: "..\control-center\release\Kinetix Control Center-win32-x64\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

; ViGEmBus installer
Source: "prereqs\ViGEmBus_Setup_x64.msi"; DestDir: "{tmp}"; Flags: ignoreversion deleteafterinstall; Check: ViGEmMsiExists

[Dirs]
Name: "{app}\config"
Name: "{app}\logs"

[Icons]
; Desktop shortcut
Name: "{autodesktop}\Kinetix Control Center"; Filename: "{app}\Kinetix Control Center.exe"; Comment: "Launch Kinetix Controller Server & Dashboard"

; Start Menu
Name: "{group}\Kinetix Control Center"; Filename: "{app}\Kinetix Control Center.exe"
Name: "{group}\Uninstall Kinetix"; Filename: "{uninstallexe}"

[Run]
; Install ViGEmBus silently if the MSI is present
Filename: "msiexec.exe"; Parameters: "/i ""{tmp}\ViGEmBus_Setup_x64.msi"" /quiet /norestart"; StatusMsg: "Installing ViGEmBus driver…"; Flags: runhidden waituntilterminated; Check: ViGEmMsiExists

; Optionally launch after install
Filename: "{app}\Kinetix Control Center.exe"; Description: "Launch Kinetix Control Center now"; Flags: postinstall nowait skipifsilent

[Registry]
; Start with Windows (optional)
Root: HKCU; Subkey: "Software\Microsoft\Windows\CurrentVersion\Run"; ValueType: string; ValueName: "KinetixControlCenter"; ValueData: """{app}\Kinetix Control Center.exe"""; Flags: uninsdeletevalue; Tasks: startup

[Tasks]
Name: "startup"; Description: "Start Kinetix Control Center with Windows"; GroupDescription: "Additional options:"; Flags: checkedonce

[Code]
function ViGEmMsiExists(): Boolean;
begin
  Result := FileExists(ExpandConstant('{src}\prereqs\ViGEmBus_Setup_x64.msi'));
end;
