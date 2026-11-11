@echo off
rem Builds the game and the browser build, then starts the backend that serves both.
rem Open http://localhost:8080 once it says the server has started.
call mvn -q install -DskipTests || exit /b 1
call mvn -q -f web\pom.xml package || exit /b 1
python webssemble.py || exit /b 1
echo.
echo   Signal Lost is starting at http://localhost:8080
echo.
cd server
call mvn -q spring-boot:run
