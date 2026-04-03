@echo off
REM ========================================
REM Workflow Coverage Calculator - Local Runner
REM Automated workflow coverage calculation tool
REM ========================================

REM Hardcoded credentials for testing (set BEFORE enabledelayedexpansion)
set "USERNAME=fse_admin"
set "PASSWORD=Philips^!"

setlocal enabledelayedexpansion

REM Configuration - Hardcoded IPs
set "HOST1=192.168.58.228"
set "HOST2=192.168.58.223"
set "HOST3=192.168.58.209"
set "FILE_PREFIX=WorkflowsCoverageRecords"
set "START_DATE=2025-09-01"

echo Using hardcoded credentials for testing...
echo.
echo ========================================
echo Starting Workflow Coverage Calculation
echo ========================================
echo.

REM Step 1: Create local logs folder
echo [1/7] Creating local logs folder...
set "LOGS_FOLDER=%~dp0workflow json logs"
if exist "%LOGS_FOLDER%" (
    echo Cleaning existing logs folder...
    rmdir /s /q "%LOGS_FOLDER%"
)
mkdir "%LOGS_FOLDER%"
echo Created: %LOGS_FOLDER%
echo.

REM Step 2: Map network shares
echo [2/7] Mapping network shares...
REM First, disconnect any existing connections to avoid conflicts
net use "\\%HOST1%\c$" /delete >nul 2>&1
net use "\\%HOST2%\c$" /delete >nul 2>&1
net use "\\%HOST3%\c$" /delete >nul 2>&1

net use "\\%HOST1%\c$" /user:%USERNAME% "%PASSWORD%" /persistent:no >nul 2>&1
set "ERR1=%ERRORLEVEL%"
if %ERR1% NEQ 0 (
    echo WARNING: Failed to map \\%HOST1%\c$ ^(check credentials^)
) else (
    echo Mapped: \\%HOST1%\c$
)

net use "\\%HOST2%\c$" /user:%USERNAME% "%PASSWORD%" /persistent:no >nul 2>&1
set "ERR2=%ERRORLEVEL%"
if %ERR2% NEQ 0 (
    echo WARNING: Failed to map \\%HOST2%\c$ ^(check credentials^)
) else (
    echo Mapped: \\%HOST2%\c$
)

net use "\\%HOST3%\c$" /user:%USERNAME% "%PASSWORD%" /persistent:no >nul 2>&1
set "ERR3=%ERRORLEVEL%"
if %ERR3% NEQ 0 (
    echo WARNING: Failed to map \\%HOST3%\c$ ^(check credentials^)
) else (
    echo Mapped: \\%HOST3%\c$
)
echo.

REM Step 3: Copy JSON logs from all three machines using PowerShell
echo [3/7] Copying JSON logs from all three machines...
echo This may take a few minutes...
echo.

powershell -Command "$sources = @('\\%HOST1%\c$', '\\%HOST2%\c$', '\\%HOST3%\c$'); $dest = '%LOGS_FOLDER%'; $start = [datetime]::Parse('%START_DATE%'); $end = Get-Date; $fileCount = 0; foreach ($src in $sources) { Write-Host \"Scanning $src ...\"; Get-ChildItem -Path $src -Filter '%FILE_PREFIX%*.json' -ErrorAction SilentlyContinue | Where-Object { ($_.LastWriteTime -ge $start -and $_.LastWriteTime -le $end) -or ($_.CreationTime -ge $start -and $_.CreationTime -le $end) } | ForEach-Object { $fileCount++; Write-Host \"  Found: $($_.Name)\"; Copy-Item -Path $_.FullName -Destination $dest -Force; Write-Host \"  Copied: $($_.Name)\" } }; Write-Host \"\"; Write-Host \"Total JSON files copied: $fileCount\""
echo.

REM Step 4: Count copied files
set "FILE_COUNT=0"
for %%F in ("%LOGS_FOLDER%\*.json") do set /a FILE_COUNT+=1
echo Total JSON files copied: %FILE_COUNT%
echo.

REM Step 5: Build the Java application (optional, use prebuilt if available)
echo [4/7] Checking for JAR file...
set "BUILD_JAR=%~dp0WorkflowCoverageCalculator\AVCumulativeWorkflowCoverageCalculator\target\AVCumulativeWorkflowCoverageCalculator-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
set "FALLBACK_JAR=%~dp0WorkflowCoverageCalculator\AVCumulativeWorkflowCoverageCalculator-0.0.1-SNAPSHOT-jar-with-dependencies.jar"

if exist "%BUILD_JAR%" (
    set "JAR_FILE=%BUILD_JAR%"
    echo Using built JAR: %BUILD_JAR%
) else if exist "%FALLBACK_JAR%" (
    set "JAR_FILE=%FALLBACK_JAR%"
    echo Using fallback JAR: %FALLBACK_JAR%
) else (
    echo ERROR: JAR file not found!
    echo Please build the project first using: mvn clean package
    pause
    exit /b 1
)
echo.

REM Step 6: Run workflow coverage calculator
echo [5/7] Running Workflow Coverage Calculator...
set "EXCLUDE_JSON=%~dp0Exclude.json"
set "OUTPUT_FOLDER=%~dp0"
java -jar "%JAR_FILE%" --jsonPath "%LOGS_FOLDER%" --rmvListJsonPath "%EXCLUDE_JSON%" > "%OUTPUT_FOLDER%WorkflowCoverageLog.txt" 2>&1

if errorlevel 1 (
    echo ERROR: Workflow Coverage Calculator failed!
    echo Check the log file: %OUTPUT_FOLDER%WorkflowCoverageLog.txt
    pause
    exit /b 1
)
echo Workflow Coverage Calculator completed successfully.
echo Log saved to: %OUTPUT_FOLDER%WorkflowCoverageLog.txt
echo.

REM Step 6.5: Move data JSON files to source folder
echo Moving output files to source folder...
for %%F in ("%LOGS_FOLDER%\data*.json") do (
    copy "%%F" "%OUTPUT_FOLDER%" >nul 2>&1
    echo Copied: %%~nxF
)
echo.

REM Step 7: Parse and display coverage percentages
echo [6/7] Parsing coverage results...
echo.
python "%~dp0Script\parse_coverage_summary.py" --logs "%OUTPUT_FOLDER%"
echo.

REM Step 8: Display output files
echo [7/7] Summary of generated files:
echo ========================================
if exist "%OUTPUT_FOLDER%WorkflowCoverageLog.txt" (
    echo [OK] WorkflowCoverageLog.txt
)
for %%F in ("%OUTPUT_FOLDER%data*.json") do (
    echo [OK] %%~nxF
)
echo ========================================
echo Output files saved to: %OUTPUT_FOLDER%
echo.

REM Cleanup network shares
echo Disconnecting network shares...
net use "\\%HOST1%\c$" /delete >nul 2>&1
net use "\\%HOST2%\c$" /delete >nul 2>&1
net use "\\%HOST3%\c$" /delete >nul 2>&1

echo.
echo ========================================
echo Workflow Coverage Calculation Complete!
echo ========================================
echo.

REM Cleanup - Delete the logs folder
echo Cleaning up temporary files...
if exist "%LOGS_FOLDER%" (
    rmdir /s /q "%LOGS_FOLDER%"
    echo Deleted: %LOGS_FOLDER%
)
echo.

pause
