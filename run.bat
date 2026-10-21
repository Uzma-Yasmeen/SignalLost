@echo off
REM Build if needed, then play. Double-click this, or run it from a terminal.
setlocal

if not exist "target\classes\com\signallost\Main.class" (
    echo Compiling Signal Lost...
    call mvn -q compile
    if errorlevel 1 (
        echo.
        echo Build failed. Check that Java and Maven are installed.
        pause
        exit /b 1
    )
)

java -cp target\classes com.signallost.Main
