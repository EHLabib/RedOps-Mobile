@echo off
setlocal
set "GRADLE_VERSION=8.7"
set "BOOTSTRAP_DIR=%USERPROFILE%\.gradle\redops-bootstrap"
set "ZIP_FILE=%BOOTSTRAP_DIR%\gradle-%GRADLE_VERSION%-bin.zip"
set "DIST_DIR=%BOOTSTRAP_DIR%\gradle-%GRADLE_VERSION%"
if not exist "%DIST_DIR%\bin\gradle.bat" (
  if not exist "%BOOTSTRAP_DIR%" mkdir "%BOOTSTRAP_DIR%"
  echo Downloading Gradle %GRADLE_VERSION%. Internet access is required for this one-time setup.
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP_FILE%'"
  if errorlevel 1 (
    echo Download failed. Check your internet connection and try again.
    exit /b 1
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%ZIP_FILE%' -DestinationPath '%BOOTSTRAP_DIR%' -Force"
  if errorlevel 1 (
    echo Could not extract Gradle.
    exit /b 1
  )
)
echo Generating the standard Gradle wrapper files...
call "%DIST_DIR%\bin\gradle.bat" wrapper --gradle-version %GRADLE_VERSION%
if errorlevel 1 (
  echo Wrapper generation failed. Copy the first Gradle error and ask for help.
  exit /b 1
)
echo.
echo Gradle wrapper generated. Now open this android folder in Android Studio and sync.
endlocal
