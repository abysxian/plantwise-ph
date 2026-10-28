#!/bin/sh
[ -f target/agristep.jar ] || mvn -q package -DskipTests
exec java -jar target/agristep.jar
