[Setup]
AppName=Kinetix Control Center
AppVersion=1.0.0
DefaultDirName={autopf}\Kinetix
DefaultGroupName=Kinetix
OutputDir=.\
OutputBaseFilename=KinetixSetup
Compression=lzma2
SolidCompression=yes
ArchitecturesInstallIn64BitMode=x64
SetupIconFile=..\assets\icon.ico

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
; IMPORTANT: Assuming electron-builder output is placed in dist\win-unpacked
Source: "..\control-center\dist\win-unpacked\*"; DestDir: "{app}"; Flags: ignoreversion recurseSubdirs createallsubdirs
Source: "..\installer\install_vigem.bat"; DestDir: "{app}\drivers"; Flags: ignoreversion

[Icons]
Name: "{group}\Kinetix Control Center"; Filename: "{app}\Kinetix Control Center.exe"
Name: "{autodesktop}\Kinetix Control Center"; Filename: "{app}\Kinetix Control Center.exe"; Tasks: desktopicon

[Run]
; Auto-start Kinetix Control Center after installation completes
Filename: "{app}\Kinetix Control Center.exe"; Description: "{cm:LaunchProgram,Kinetix Control Center}"; Flags: nowait postinstall skipifsilent

[Code]
// Silently run the ViGEmBus driver batch installer gracefully during setup
procedure CurStepChanged(CurStep: TSetupStep);
var
  ResultCode: Integer;
begin
  if CurStep = ssPostInstall then
  begin
    Exec(ExpandConstant('{app}\drivers\install_vigem.bat'), '', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  end;
end;
