@echo off
rem Double-click to open CampusTrack as a desktop app.
rem Build the jar first (IntelliJ: Maven > Lifecycle > package).
cd /d "%~dp0"
if not exist "target\campustrack-0.0.1-SNAPSHOT.jar" (
    echo The app has not been built yet.
    echo In IntelliJ open the Maven panel and run Lifecycle ^> package, then try again.
    pause
    exit /b 1
)
start "CampusTrack" javaw -jar "target\campustrack-0.0.1-SNAPSHOT.jar"
