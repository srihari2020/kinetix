; ──────────────────────────────────────────────────────────────────────
;  Kinetix PC Server – Inno Setup Installer Script
;
;  Prerequisites:
;   1. Build kinetix-server.exe first (run build.bat)
;   2. Place ViGEmBus_Setup_x64.msi in this folder
;   3. Compile with Inno Setup 6+  (https://jrsoftware.org/isinfo.php)
; ──────────────────────────────────────────────────────────────────────

[Setup]
AppName=Kinetix Server
AppVersion=1.0.0
AppPublisher=Kinetix Contributors
AppPublisherURL=https://github.com/kinetix-controller
DefaultDirName={autopf}\Kinetix Server
DefaultGroupName=Kinetix Server
UninstallDisplayIcon={app}\kinetix-server.exe
OutputDir=output
OutputBaseFilename=KinetixServerSetup
Compression=lzma2
SolidCompression=yes
ArchitecturesInstallIn64BitMode=x64compatible
WizardStyle=modern
PrivilegesRequired=admin

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
; Main executable (relative to this .iss file → ../pc-server/dist/)
Source: "..\pc-server\dist\kinetix-server.exe"; DestDir: "{app}"; Flags: ignoreversion

; ViGEmBus driver installer (user must place this MSI in the installer/ folder)
Source: "ViGEmBus_Setup_x64.msi"; DestDir: "{tmp}"; Flags: ignoreversion deleteafterinstall; Check: ViGEmMsiExists

[Icons]
; Desktop shortcut
Name: "{autodesktop}\Kinetix Server"; Filename: "{app}\kinetix-server.exe"; Comment: "Launch Kinetix game controller server"

; Start Menu
Name: "{group}\Kinetix Server"; Filename: "{app}\kinetix-server.exe"
Name: "{group}\Uninstall Kinetix Server"; Filename: "{uninstallexe}"

[Run]
; Install ViGEmBus silently if the MSI is present
Filename: "msiexec.exe"; Parameters: "/i ""{tmp}\ViGEmBus_Setup_x64.msi"" /quiet /norestart"; StatusMsg: "Installing ViGEmBus driver…"; Flags: runhidden waituntilterminated; Check: ViGEmMsiExists

; Optionally launch after install
Filename: "{app}\kinetix-server.exe"; Description: "Launch Kinetix Server now"; Flags: postinstall nowait skipifsilent

[Registry]
; Start with Windows (optional — checked by default in the wizard)
Root: HKCU; Subkey: "Software\Microsoft\Windows\CurrentVersion\Run"; ValueType: string; ValueName: "KinetixServer"; ValueData: """{app}\kinetix-server.exe"""; Flags: uninsdeletevalue; Tasks: startup

[Tasks]
Name: "startup"; Description: "Start Kinetix Server with Windows"; GroupDescription: "Additional options:"; Flags: checkedonce

[Code]
function ViGEmMsiExists(): Boolean;
begin
  Result := FileExists(ExpandConstant('{src}\ViGEmBus_Setup_x64.msi'));
end;

