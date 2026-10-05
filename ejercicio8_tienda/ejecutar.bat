@echo off
REM Compila y abre las dos ventanas (Almacén y Mostrador) sobre el mismo modelo.
cd /d "%~dp0"
dir /s /b src\*.java > fuentes.txt
javac -encoding UTF-8 -d out\app @fuentes.txt || exit /b 1
del fuentes.txt
java -Dfile.encoding=UTF-8 -cp out\app Main
