@echo off
setlocal enabledelayedexpansion

REM === Configuration ===
set REPO_DIR=D:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Java
set LIB_DIR=%REPO_DIR%\lib
set DB_NAME=my-codeql-db-java
set SARIF_OUT=resultsjava.sarif

echo === [1] Clean previous builds ===
for /r "%REPO_DIR%" %%d in (out) do (
    if exist "%%d" (
        echo Deleting "%%d"
        rmdir /s /q "%%d"
    )
)

echo === [2] Compile all Java files recursively ===

REM Find and compile all .java files under Scenario folders
for /r "%REPO_DIR%" %%f in (*.java) do (
    set FILE=%%f
    set DIR=%%~dpf
    set DIR=!DIR:~0,-1!

    REM Create 'out' folder inside that directory
    if not exist "!DIR!\out" (
        mkdir "!DIR!\out"
    )

    echo Compiling: %%f
    javac -cp "%LIB_DIR%\*" -d "!DIR!\out" "%%f"
)
