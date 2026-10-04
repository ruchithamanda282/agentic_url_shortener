@echo off
setlocal
java -version
if errorlevel 1 (
  echo Java 21 is required. Please install JDK 21 and set JAVA_HOME.
  exit /b 1
)
echo Starting Agentic URL Shortener on http://localhost:8080 ...
mvn spring-boot:run
