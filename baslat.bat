@echo off
title Kopru Platformu
echo ==============================================
echo   KOPRU PROJESI BASLATILIYOR...
echo ==============================================

:: 1. Backend'i ayri bir pencerede baslat
start "Kopru - Backend (8080)" powershell -NoExit -Command "$env:JAVA_HOME = 'C:\Users\temel\.jdks\azul-21.0.10'; & 'C:\Users\temel\.jdks\azul-21.0.10\bin\java.exe' -jar target\kopru-0.1.0-SNAPSHOT.jar"

:: Backend'in ayaga kalkmasi icin kisa bir bekleme
timeout /t 3 /nobreak >nul

:: 2. Frontend dizinine gec ve dev sunucusunu baslat
cd frontend
npm run dev
