@echo off
REM Compila el sistema de la tienda y ejecuta las pruebas JUnit 5 (usa los .jar de ..\lib).
cd /d "%~dp0"
set CP=..\lib\*
if exist out rmdir /s /q out
dir /s /b src\*.java > fuentes.txt
javac -encoding UTF-8 -d out\app @fuentes.txt || exit /b 1
dir /s /b test\*.java > pruebas.txt
javac -encoding UTF-8 -cp "out\app;%CP%" -d out\test @pruebas.txt || exit /b 1
del fuentes.txt pruebas.txt
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "out\app;out\test;%CP%" RunTests
