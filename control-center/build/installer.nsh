!macro customInstall
  DetailPrint "Checking if ViGEm Bus Driver is installed..."
  
  ReadRegStr $0 HKLM "SOFTWARE\Nefarius Software Solutions e.U.\ViGEm Bus Driver" "Version"
  
  ${If} $0 == ""
    DetailPrint "ViGEm Bus Driver not found. Silently installing..."
    ExecWait '"msiexec.exe" /i "$INSTDIR\resources\installer\ViGEmBus_Setup_x64.msi" /quiet /norestart'
    DetailPrint "ViGEm Bus Driver installation completed."
  ${Else}
    DetailPrint "ViGEm Bus Driver is already installed (Version: $0). Skipping."
  ${EndIf}
!macroend
