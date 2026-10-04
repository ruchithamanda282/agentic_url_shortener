$ErrorActionPreference = "Stop"
Write-Host "Checking Java..."
java -version
Write-Host "Starting Agentic URL Shortener on http://localhost:8080 ..."
mvn spring-boot:run
