@echo off
setlocal enabledelayedexpansion

REM === Configuration ===
set REPO_DIR=D:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Java
set LIB_DIR=%REPO_DIR%\lib
set OUT_DIR=%REPO_DIR%\out
set FILE_LIST=%OUT_DIR%\java_files.txt

REM Create out folder if it doesn't exist
if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

REM === Find all .java files and write to file ===
echo Finding all Java files...
if exist "%FILE_LIST%" del "%FILE_LIST%"
for /r "%REPO_DIR%\DeepSeek_R1_32B" %%f in (*.java) do (
    echo %%f >> "%FILE_LIST%"
)

REM === Compile all Java files at once ===
echo Compiling all Java files...
javac -cp "%LIB_DIR%\*" -d "%OUT_DIR%" @"%FILE_LIST%"

echo Done!
pause
