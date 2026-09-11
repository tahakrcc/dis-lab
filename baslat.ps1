# 1. Backend'i ayri bir pencerede baslat
Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$env:JAVA_HOME = 'C:\Users\temel\.jdks\azul-21.0.10'; & 'C:\Users\temel\.jdks\azul-21.0.10\bin\java.exe' -jar target\kopru-0.1.0-SNAPSHOT.jar"

# 2. Frontend dizinine gec ve dev sunucusunu baslat
Start-Sleep -Seconds 3
Set-Location "$PSScriptRoot\frontend"
npm run dev
