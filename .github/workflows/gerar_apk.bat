@echo off
chcp 65001 >nul
cd /d "%~dp0"
where node >nul 2>nul || (echo Instale o Node.js LTS em https://nodejs.org e rode de novo. & pause & exit /b 1)
where java >nul 2>nul || (echo Instale o JDK 17 em https://adoptium.net e rode de novo. & pause & exit /b 1)
if "%ANDROID_HOME%"=="" if "%ANDROID_SDK_ROOT%"=="" (echo Instale o Android Studio e defina a variavel ANDROID_HOME para a pasta do SDK. & pause & exit /b 1)
call npm install || goto erro
if not exist android call npx cap add android || goto erro
node preparar.js || goto erro
call npx cap sync android || goto erro
cd android
call gradlew.bat assembleDebug || goto erro
cd ..
copy /y android\app\build\outputs\apk\debug\app-debug.apk CaveiraX.apk >nul
echo.
echo PRONTO! O arquivo CaveiraX.apk esta nesta pasta. Envie para o celular e instale.
pause
exit /b 0
:erro
echo.
echo Falhou. Veja a mensagem acima.
pause
exit /b 1
