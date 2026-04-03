# Workflow Coverage Calculator - Complete Documentation

## Project Overview

The **Workflow Coverage Calculator** is an automated system designed for **single-click workflow coverage calculation** across multiple medical imaging applications. The system collects workflow execution data from remote test machines, analyzes the data, and generates comprehensive coverage reports showing which workflows have been executed and which remain untested.

### Goal
**One-click execution** to get instant workflow coverage percentages across all medical imaging applications without manual intervention.

### What Problem Does It Solve?
In medical imaging software testing, tracking which workflows have been tested across different applications is complex and time-consuming. This tool automates the entire process:
- Automatically collects workflow data from multiple test machines
- Analyzes execution patterns across different application types
- Calculates coverage percentages
- Provides instant summary of testing progress

## System Architecture

```
Workflow Coverage/
├── run_workflow_coverage_local.bat   # Main execution script (SINGLE-CLICK)
├── Exclude.json                      # Workflows to exclude from calculation
├── Script/
│   └── parse_coverage_summary.py     # Coverage percentage parser
└── WorkflowCoverageCalculator/
    ├── AVCumulativeWorkflowCoverageCalculator/
    │   ├── src/main/java/com/report/
    │   │   └── AVCumulativeWorkflowCoverageCalculator.java
    │   ├── pom.xml                   # Maven configuration
    │   └── target/                   # Build output
    └── AVCumulativeWorkflowCoverageCalculator-0.0.1-SNAPSHOT-jar-with-dependencies.jar
```

## How It Works - Complete Workflow

### High-Level Process Flow

```
[User Double-Clicks BAT File]
         |
         v
[1. Load Hardcoded Credentials]
         |
         v
[2. Connect to 3 Remote Test Machines]
    (192.168.58.228, 192.168.58.223, 192.168.58.209)
         |
         v
[3. Copy Workflow JSON Files from C:\]
    (Date filtered: after 2025-09-01)
         |
         v
[4. Run Java Coverage Calculator]
    (Apply exclusions from Exclude.json)
         |
         v
[5. Parse Results & Display Summary]
         |
         v
[6. Cleanup Temporary Files]
         |
         v
[Display Coverage Percentages]
```

---

## Component 1: Batch File (run_workflow_coverage_local.bat)

### Purpose
The **main orchestrator** - automates the entire workflow coverage calculation process.

### Location
`c:\reno\git\Workflow Coverage\Workflow Coverage\run_workflow_coverage_local.bat`

### What It Does (Step-by-Step)

#### Step 1: Configuration Loading
```batch
HOST1=192.168.58.228
HOST2=192.168.58.223
HOST3=192.168.58.209
FILE_PREFIX=WorkflowsCoverageRecords
START_DATE=2025-09-01
```
- Defines 3 test machine IP addresses
- Sets the file naming pattern to search for
- Defines date range for filtering workflow files

#### Step 2: Load Credentials
```batch
set "USERNAME=fse_admin"
set "PASSWORD=Philips^!"
```
- Credentials are hardcoded in the batch file (lines 8-9)
- Set **before** `enabledelayedexpansion` to avoid special character issues
- Special characters like `!` must be escaped with `^`
- Update these values with your actual credentials

#### Step 3: Create Temporary Folder
```batch
mkdir "workflow json logs"
```
- Creates a temporary directory for collecting JSON files
- Cleans existing folder if present

#### Step 4: Network Share Mapping
```batch
net use "\\192.168.58.228\c$" /user:%USERNAME% "%PASSWORD%" /persistent:no
net use "\\192.168.58.223\c$" /user:%USERNAME% "%PASSWORD%" /persistent:no
net use "\\192.168.58.209\c$" /user:%USERNAME% "%PASSWORD%" /persistent:no
```
- Connects to C$ administrative shares on all 3 machines
- Uses hardcoded credentials from batch file (lines 8-9)
- `/persistent:no` ensures connections don't persist after reboot
- Continues even if some machines fail (shows warnings)

#### Step 5: File Collection (PowerShell)
```powershell
Get-ChildItem -Path $src -Filter "WorkflowsCoverageRecords*.json" -ErrorAction SilentlyContinue |
  Where-Object { ($_.LastWriteTime -ge $start -and $_.LastWriteTime -le $end) -or ($_.CreationTime -ge $start -and $_.CreationTime -le $end) }
```
- Uses PowerShell for reliable network share scanning
- Searches **only C:\ root folder** (files are stored directly in C:\, not subfolders)
- Filters files by date (modified/created after START_DATE: 2025-09-01)
- Finds all files matching `WorkflowsCoverageRecords*.json`
- Copies matching files to local `workflow json logs` folder
- Displays progress for each file found and copied
- Optimized for speed - no recursive search needed

#### Step 6: JAR File Detection
```batch
if exist "%BUILD_JAR%" (
    set "JAR_FILE=%BUILD_JAR%"
) else if exist "%FALLBACK_JAR%" (
    set "JAR_FILE=%FALLBACK_JAR%"
)
```
- Checks for freshly built JAR in `target/` directory
- Falls back to prebuilt JAR if build doesn't exist
- Exits with error if neither JAR is found

#### Step 7: Run Coverage Calculator
```batch
java -jar "%JAR_FILE%" --jsonPath "%LOGS_FOLDER%" --rmvListJsonPath "%EXCLUDE_JSON%"
```
- Executes the Java application
- Passes path to collected JSON files
- Includes exclusion list from `Exclude.json`
- Redirects console output to `WorkflowCoverageLog.txt` in **source folder**
- Generates `data<YYYY-MM-DD>.json` in temporary logs folder
- **Copies output files to source folder** for permanent storage

#### Step 8: Parse and Display Results
```batch
python "Script\parse_coverage_summary.py" --logs "%LOGS_FOLDER%"
```
- Runs Python script to parse the generated JSON
- Displays coverage percentages in clean format
- Shows results on console

#### Step 9: Cleanup
```batch
net use "\\%HOST1%\c$" /delete
net use "\\%HOST2%\c$" /delete
net use "\\%HOST3%\c$" /delete
rmdir /s /q "%LOGS_FOLDER%"
```
- Disconnects all network shares
- Deletes temporary `workflow json logs` folder
- Removes all copied files

### Error Handling
- Warns if network shares fail to map (check credentials)
- Verifies JAR file exists before execution
- Checks if calculator execution succeeds
- Provides detailed error messages at each step
- Continues processing even if some machines are unreachable

---

## Component 2: Java Coverage Calculator

### Purpose
Core calculation engine that analyzes workflow JSON files and computes coverage statistics.

### Location
`WorkflowCoverageCalculator/AVCumulativeWorkflowCoverageCalculator/src/main/java/com/report/AVCumulativeWorkflowCoverageCalculator.java`

### Input Format
Expects JSON files with this structure:
   ```json
   {
     "AppIDs": ["App1", "App2"],
     "WorkflowsList": ["Application - Workflow1", "Application - Workflow2"],
     "ExecutedWorkflows": ["Application - Workflow1"],
     "NotExecutedWorkflows": ["Application - Workflow2"]
   }
   ```

### What It Does

1. **Reads all JSON files** from the provided directory
2. **Parses workflow data** from each file:
   - Extracts `AppIDs`
   - Collects all workflows from `WorkflowsList`
   - Identifies executed workflows from `ExecutedWorkflows`
   - Identifies not-executed workflows from `NotExecutedWorkflows`

3. **Applies exclusions** from `Exclude.json`:
   - Removes specified workflows from overall count
   - Ensures excluded workflows don't affect coverage percentages

4. **Categorizes workflows** by application type:
   - **AvaApplication**: Combines AvaApplicationForMR and AvaApplicationForCT
   - **AVViewerApplication**: Viewer-specific workflows
   - **FCMRInspectionMode**: Includes QFlow, Mapping, Temporal, Spatial, FindingsDashboard
   - **CcaApplication**: CCA-specific workflows
   - **FunctionalCardiacCTApplication**: Functional CT workflows

5. **Calculates coverage** for each category:
   ```
   Coverage % = (Executed Workflows / Total Workflows) × 100
   ```

6. **Special handling for AvaApplication**:
   - Canonicalizes MR and CT workflows
   - Maps `AvaApplicationForMR - Workflow` → `AvaApplicationForCT - Workflow`
   - Calculates unified coverage based on CT workflow count

7. **Generates output**:
   - Detailed console log with all workflow lists
   - JSON file: `data<YYYY-MM-DD>.json` with structured results

### Output Format

The calculator generates `data<YYYY-MM-DD>.json` with this structure:
```json
{
  "AvaApplication": "{\"WorkflowsList Count\":100,\"ExecutedWorkflows Count\":85,\"Percentage :\":\"85%\",\"ExecutedWorkflows\":[...],\"NotExecutedWorkflows\":[...]}",
  "AVViewerApplication": "{...}",
  "FCMRInspectionMode": "{...}",
  "CcaApplication": "{...}",
  "FunctionalCardiacCTApplication": "{...}"
}
```

Note: Each category's data is stored as a JSON string (nested JSON).

### Command-Line Parameters

- `--jsonPath`: Path to folder containing workflow JSON files (required)
- `--rmvListJsonPath`: Path to exclusion list JSON file (optional)
- `--date`: Date in DD-MM-YYYY format (optional, for reporting)

### Maven Build Configuration

**File**: `pom.xml`

**Dependencies**:
- Jackson Core & Databind 2.16.1 (JSON processing)
- Apache Commons IO 2.15.1 (File operations)
- Picocli 4.6.1 (Command-line interface)
- JSch 0.1.55 (SSH/SFTP - used by FileTransfer utility)

**Build Output**:
- Standard JAR: `target/AVCumulativeWorkflowCoverageCalculator-0.0.1-SNAPSHOT.jar`
- **Executable JAR with dependencies**: `target/AVCumulativeWorkflowCoverageCalculator-0.0.1-SNAPSHOT-jar-with-dependencies.jar`

---

## Component 3: Exclusion List (Exclude.json)

### Purpose
Defines workflows that should be excluded from coverage calculations (e.g., deprecated features, known issues).

### Location
`c:\reno\git\Workflow Coverage\Workflow Coverage\Exclude.json`

### Format
```json
{
  "WorkflowsList": [
    "TemporalApplication - Save3DBatchPopup",
    "QFlowInspectionMode - Save3DBatchPopup",
    "AVViewerApplication - CArmAnglePresetComboValueChanged",
    "FCMRInspectionMode - AfterContourDeletedArgs"
  ]
}
```

### How It Works
- Java calculator reads this file
- Removes listed workflows from `overAllWorkFlow` set
- Excluded workflows don't count toward total or coverage percentage
- Displays excluded workflows in console output for transparency

### When to Update
- Add workflows that are deprecated
- Add workflows that are known to be broken
- Add workflows that are not applicable for current testing scope

---

## Component 4: Python Parser (parse_coverage_summary.py)

### Purpose
Parses the Java calculator's output and displays a clean, readable summary of coverage percentages.

### Location
`c:\reno\git\Workflow Coverage\Workflow Coverage\Script\parse_coverage_summary.py`

### What It Does

1. **Finds the latest data file**:
   ```python
   pattern = os.path.join(args.logs, "data*.json")
   files = glob.glob(pattern)
   latest_file = max(files, key=os.path.getmtime)
   ```
   - Searches for all `data*.json` files
   - Selects the most recently modified file

2. **Parses nested JSON**:
   ```python
   data = json.load(f)
   for category in CATEGORIES:
       inner = json.loads(data[category])  # Double parsing
       percentage = inner.get("Percentage :", "0%")
   ```
   - Loads outer JSON structure
   - Each category value is itself a JSON string
   - Parses inner JSON to extract percentage

3. **Displays formatted output**:
   ```
   ==================================================
   WORKFLOW COVERAGE SUMMARY
   ==================================================
   Reading: data2026-03-30.json
   --------------------------------------------------
   FCMRInspectionMode - 30%
   AVViewerApplication - 46%
   CcaApplication - 37%
   AvaApplication - 60%
   FunctionalCardiacCTApplication - 54%
   ==================================================
   ```

### Categories Tracked
```python
CATEGORIES = [
    "FCMRInspectionMode",
    "AVViewerApplication",
    "CcaApplication",
    "AvaApplication",
    "FunctionalCardiacCTApplication",
]
```

### Error Handling
- Exits if no data files found
- Handles JSON parsing errors gracefully
- Shows "ERROR parsing data" for corrupted entries
- Shows "NOT FOUND" for missing categories

### Python Version Compatibility
- Uses `.format()` instead of f-strings
- Compatible with Python 2.7+ and Python 3.x

---

## How to Use - Quick Start Guide

### Prerequisites

1. **Network Access**: Must be on Philips network or connected via VPN
2. **Java**: Java 8 or higher installed
3. **Python**: Python 2.7 or higher installed

### Setup (One-Time)

1. **Configure credentials in batch file**:
   - Open `run_workflow_coverage_local.bat` in a text editor
   - Update lines 8-9 with your credentials:
     ```batch
     set "USERNAME=your_username"
     set "PASSWORD=your_password"
     ```
   - **Important**: If your password contains special characters like `!`, escape them with `^` (e.g., `Password^!`)
   - Save the file

2. **Verify JAR file exists**:
   - Check for `WorkflowCoverageCalculator/AVCumulativeWorkflowCoverageCalculator-0.0.1-SNAPSHOT-jar-with-dependencies.jar`
   - Or build from source: `mvn clean package`

### Running the Tool

**Single-Click Execution**:
1. Double-click `run_workflow_coverage_local.bat`
2. Wait for completion (typically 2-5 minutes depending on network speed)
3. View coverage percentages in console output

**What You'll See**:
```
Using hardcoded credentials for testing...

========================================
Starting Workflow Coverage Calculation
========================================

[1/7] Creating local logs folder...
[2/7] Mapping network shares...
[3/7] Copying JSON logs from all three machines...
[4/7] Checking for JAR file...
[5/7] Running Workflow Coverage Calculator...
[6/7] Parsing coverage results...

==================================================
WORKFLOW COVERAGE SUMMARY
==================================================
FCMRInspectionMode - 30%
AVViewerApplication - 46%
CcaApplication - 37%
AvaApplication - 60%
FunctionalCardiacCTApplication - 54%
==================================================

[7/7] Summary of generated files...
========================================
Workflow Coverage Calculation Complete!
========================================
```

---

## Troubleshooting

### Issue: "WARNING: Failed to map \\192.168.58.xxx\c$ (check credentials)"
**Possible Causes**:
1. Not connected to Philips network/VPN
2. Incorrect credentials in batch file (lines 8-9)
3. Special characters in password not properly escaped
4. Test machine is offline
5. Firewall blocking access

**Solution**: 
- Connect to Philips VPN
- Verify credentials in `run_workflow_coverage_local.bat` (lines 8-9)
- If password contains `!`, escape it with `^` (e.g., `Password^!`)
- Test connection: `Test-Connection 192.168.58.228`
- Test mapping manually: `net use \\192.168.58.228\c$ /user:username password`

### Issue: "Total JSON files copied: 0"
**Possible Causes**:
1. No workflow data has been generated on test machines yet
2. Files are older than START_DATE (2025-09-01)
3. Files have different naming pattern

**Solution**:
- Manually check if files exist: Browse to `\\192.168.58.228\c$` in Windows Explorer
- Verify files are named `WorkflowsCoverageRecords*.json`
- Check file dates are after September 1, 2025
- Adjust START_DATE in batch file if needed (line 17)

### Issue: "ERROR: JAR file not found!"
**Solution**: 
- Check if prebuilt JAR exists in `WorkflowCoverageCalculator/` folder
- Or build from source:
  ```bash
  cd WorkflowCoverageCalculator/AVCumulativeWorkflowCoverageCalculator
  mvn clean package
  ```

### Issue: "SyntaxError: invalid syntax" (Python)
**Solution**: Python script has been updated to support older Python versions. Update to latest version of `parse_coverage_summary.py`

---

## Technical Architecture Details

### Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Test Machines                            │
│  192.168.58.228  │  192.168.58.223  │  192.168.58.209     │
│  WorkflowsCoverageRecords*.json files                       │
└────────────────────┬────────────────────────────────────────┘
                     │ (Network Share Copy)
                     ▼
┌─────────────────────────────────────────────────────────────┐
│         Local Temporary Folder: workflow json logs          │
│              (All JSON files collected here)                │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│         Java Coverage Calculator (JAR)                      │
│  - Reads all JSON files                                     │
│  - Applies exclusions from Exclude.json                     │
│  - Calculates coverage per application                      │
│  - Generates data<YYYY-MM-DD>.json                          │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│         Python Parser (parse_coverage_summary.py)           │
│  - Reads data<YYYY-MM-DD>.json                              │
│  - Extracts coverage percentages                            │
│  - Displays formatted summary                               │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                  Console Output                             │
│  FCMRInspectionMode - 30%                                   │
│  AVViewerApplication - 46%                                  │
│  CcaApplication - 37%                                       │
│  AvaApplication - 60%                                       │
│  FunctionalCardiacCTApplication - 54%                       │
└─────────────────────────────────────────────────────────────┘
```

### File Naming Convention

**Input Files**: `WorkflowsCoverageRecords*.json`
- Generated by test automation on test machines
- Contains workflow execution data
- Multiple files may exist per machine

**Output File**: `data<YYYY-MM-DD>.json`
- Generated by Java calculator
- Contains aggregated coverage data
- One file per execution, named with current date

### Security Considerations

1. **Credentials Storage**:
   - `.env` file excluded from Git via `.gitignore`
   - Never commit credentials to repository
   - Use `.env.example` as template only

2. **Network Access**:
   - Requires administrative share access (C$)
   - Uses `/persistent:no` to avoid saving credentials
   - Disconnects shares after completion

3. **Temporary Files**:
   - All copied files deleted after processing
   - No sensitive data left on local machine

---

## Summary

The **Workflow Coverage Calculator** provides a **single-click solution** for calculating workflow test coverage across multiple medical imaging applications. By automating the collection, analysis, and reporting of workflow execution data, it eliminates manual tracking and provides instant visibility into testing progress.

**Key Benefits**:
- ✅ **Automated**: No manual file collection or data entry
- ✅ **Fast**: Complete analysis in 2-5 minutes
- ✅ **Accurate**: Consistent calculation methodology
- ✅ **Comprehensive**: Covers all major application categories
- ✅ **Secure**: Credentials stored locally, not in code
- ✅ **Clean**: Automatic cleanup of temporary files

**Main Components**:
1. **`.env`** - Secure credential storage
2. **`run_workflow_coverage_local.bat`** - Main orchestrator
3. **Java Calculator** - Core analysis engine
4. **`Exclude.json`** - Workflow exclusion list
5. **`parse_coverage_summary.py`** - Results formatter

**Result**: Instant visibility into workflow test coverage percentages for informed decision-making.

---

## Building from Source (Optional)

If you need to rebuild the Java calculator:

### Prerequisites
- **Java JDK 8** or higher
- **Maven 3.x** installed and configured

### Build Steps
```bash
cd "WorkflowCoverageCalculator\AVCumulativeWorkflowCoverageCalculator"
mvn clean package
```

### Build Output
- Executable JAR: `target/AVCumulativeWorkflowCoverageCalculator-0.0.1-SNAPSHOT-jar-with-dependencies.jar`
- Copy this JAR to parent directory or batch file will use it from `target/` folder

### Maven Dependencies
- Jackson Core & Databind 2.16.1 (JSON processing)
- Apache Commons IO 2.15.1 (File operations)
- Picocli 4.6.1 (Command-line interface)
- JSch 0.1.55 (SFTP utility)
- Java 8 minimum

---

## Running the JAR File Individually (Advanced)

If you need to run the Java calculator manually without the batch file:

### Basic Usage

**Minimum Required:**
```bash
java -jar WorkflowCoverageCalculator\AVCumulativeWorkflowCoverageCalculator-0.0.1-SNAPSHOT-jar-with-dependencies.jar --jsonPath "C:\path\to\json\files"
```

**With Exclusion List:**
```bash
java -jar WorkflowCoverageCalculator\AVCumulativeWorkflowCoverageCalculator-0.0.1-SNAPSHOT-jar-with-dependencies.jar --jsonPath "C:\path\to\json\files" --rmvListJsonPath "Exclude.json"
```

**With Date Parameter:**
```bash
java -jar WorkflowCoverageCalculator\AVCumulativeWorkflowCoverageCalculator-0.0.1-SNAPSHOT-jar-with-dependencies.jar --jsonPath "C:\path\to\json\files" --date 30-03-2026
```

**Complete Example:**
```bash
java -jar WorkflowCoverageCalculator\AVCumulativeWorkflowCoverageCalculator-0.0.1-SNAPSHOT-jar-with-dependencies.jar --jsonPath "C:\WorkflowData" --rmvListJsonPath "Exclude.json" --date 30-03-2026
```

### Command-Line Parameters

| Parameter | Description | Required | Example |
|-----------|-------------|----------|---------|
| `--jsonPath` | Path to folder containing workflow JSON files | **Yes** | `C:\Data\workflows` |
| `--rmvListJsonPath` | Path to exclusion list JSON file | No | `Exclude.json` |
| `--date` | Date in DD-MM-YYYY format for reporting | No | `30-03-2026` |
| `--help` | Display help information | No | - |

### Output Files

When running the JAR manually, it generates:

1. **Console Output**: Detailed coverage information printed to console
   - Coverage percentages per application
   - Executed workflow lists
   - Not-executed workflow lists
   - Overall statistics

2. **JSON File**: `data<YYYY-MM-DD>.json` in the same folder as input JSON files
   - Contains structured coverage data
   - One file per execution, named with current date
   - Format:
     ```json
     {
       "AvaApplication": "{\"WorkflowsList Count\":100,\"ExecutedWorkflows Count\":85,\"Percentage :\":\"85%\",...}",
       "AVViewerApplication": "{...}",
       "FCMRInspectionMode": "{...}",
       "CcaApplication": "{...}",
       "FunctionalCardiacCTApplication": "{...}"
     }
     ```

### Input JSON Format

The calculator expects JSON files with this structure:
```json
{
  "AppIDs": ["AvaApplicationForCT", "AVViewerApplication"],
  "WorkflowsList": [
    "AvaApplicationForCT - Workflow1",
    "AvaApplicationForCT - Workflow2",
    "AVViewerApplication - Workflow3"
  ],
  "ExecutedWorkflows": [
    "AvaApplicationForCT - Workflow1",
    "AVViewerApplication - Workflow3"
  ],
  "NotExecutedWorkflows": [
    "AvaApplicationForCT - Workflow2"
  ]
}
```

### Exclusion List Format

`Exclude.json` format:
```json
{
  "WorkflowsList": [
    "TemporalApplication - Save3DBatchPopup",
    "AVViewerApplication - CArmAnglePresetComboValueChanged",
    "FCMRInspectionMode - AfterContourDeletedArgs"
  ]
}
```

### Example Workflow

1. **Prepare your data**:
   ```bash
   mkdir C:\WorkflowData
   # Copy your workflow JSON files to C:\WorkflowData
   ```

2. **Run the calculator**:
   ```bash
   cd "c:\reno\git\Workflow Coverage\Workflow Coverage"
   java -jar WorkflowCoverageCalculator\AVCumulativeWorkflowCoverageCalculator-0.0.1-SNAPSHOT-jar-with-dependencies.jar --jsonPath "C:\WorkflowData" --rmvListJsonPath "Exclude.json"
   ```

3. **Check results**:
   - View console output for detailed coverage information
   - Open `C:\WorkflowData\data<date>.json` for structured data

### Notes

- The JAR processes all JSON files in the specified folder
- Output JSON file is created in the **same folder** as input files
- Console output is printed to stdout (redirect with `>` if needed)
- Invalid JSON files are skipped with warnings
- The calculator handles missing or incomplete data gracefully

---

## Frequently Asked Questions

### Q: Do I need to manually collect JSON files from test machines?
**A:** No! The batch file automatically connects to all 3 test machines and copies the files for you.

### Q: What if one of the test machines is offline?
**A:** The batch file will show a warning but continue with the other machines. Coverage will be calculated from available data.

### Q: How often should I run this?
**A:** Run it whenever you need updated coverage metrics - typically after test cycles or when you want to check current workflow coverage status.

### Q: Can I run this without VPN?
**A:** No. You must be on the Philips network or connected via VPN to access the test machines.

### Q: Where are the results stored?
**A:** Results are displayed in the console and temporarily stored in `workflow json logs` folder, which is automatically deleted after completion.

### Q: Can I modify which workflows are excluded?
**A:** Yes! Edit `Exclude.json` to add or remove workflows from the exclusion list.

### Q: What if I get "JAR file not found" error?
**A:** The prebuilt JAR should be in the `WorkflowCoverageCalculator/` folder. If missing, rebuild using `mvn clean package`.

---

## Project Metadata

- **Main Class**: `main.java.com.report.AVCumulativeWorkflowCoverageCalculator`
- **Package**: `main.java.com.report`
- **Maven Group ID**: `com.report`
- **Artifact ID**: `AVCumulativeWorkflowCoverageCalculator`
- **Version**: `0.0.1-SNAPSHOT`
- **Java Version**: 8 (source and target)
- **Encoding**: UTF-8

---

## Contact & Support

For questions or issues regarding this tool, please contact the development team.
