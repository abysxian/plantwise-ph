@echo off
rem Double-click to start PlantWise (needs Java 17+ and a built jar: run "mvn package -DskipTests" once).
if not exist target\agristep.jar (
  echo Building first...
  call mvn -q package -DskipTests
)
java -jar target\agristep.jar
pause
