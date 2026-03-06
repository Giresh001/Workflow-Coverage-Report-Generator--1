# WorkflowCoverageCalculator Documentation

## Overview

The **WorkflowCoverageCalculator** is a Java-based Maven application that calculates cumulative workflow coverage percentages across different medical imaging applications. It analyzes workflow execution data from JSON files and generates comprehensive coverage reports categorized by application type.

## Module Structure

```
WorkflowCoverageCalculator/
├── AVCumulativeWorkflowCoverageCalculator/
│   ├── src/
│   │   └── main/
│   │       └── java/
│   │           └── com/
│   │               └── report/
│   │                   ├── AVCumulativeWorkflowCoverageCalculator.java (Main Calculator)
│   │                   └── FileTransfer.java (SFTP File Transfer Utility)
│   ├── pom.xml (Maven Build Configuration)
│   ├── target/ (Build Output Directory)
│   └── bin/ (Compiled Classes)
└── AVCumulativeWorkflowCoverageCalculator-0.0.1-SNAPSHOT-jar-with-dependencies.jar (Executable JAR)
```

## What It Does

The calculator performs the following operations:

1. **Reads JSON workflow data files** from a specified directory
2. **Aggregates workflow information** including:
   - Total workflows available
   - Executed workflows
   - Not executed workflows
   - Application IDs
3. **Calculates coverage percentages** for different application categories:
   - **AvaApplication** (AvaApplicationForMR, AvaApplicationForCT)
   - **AVViewerApplication**
   - **FCMRInspectionMode** (includes QFlowInspectionMode, MappingApplication, TemporalApplication, SpatialApplication, FindingsDashboardApplication)
   - **CcaApplication**
   - **FunctionalCardiacCTApplication**
4. **Generates detailed reports** showing executed vs not-executed workflows
5. **Exports results** to a JSON file with timestamp

## Input Requirements

### Required Input

1. **JSON Workflow Files Directory** (`--jsonPath`)
   - Directory containing workflow JSON files
   - Files must follow this structure:
   ```json
   {
     "AppIDs": ["App1", "App2"],
     "WorkflowsList": ["Application - Workflow1", "Application - Workflow2"],
     "ExecutedWorkflows": ["Application - Workflow1"],
     "NotExecutedWorkflows": ["Application - Workflow2"]
   }
   ```

### Optional Input

2. **Date Parameter** (`--date`)
   - Format: `DD-MM-YYYY`
   - Used for reporting purposes

3. **Removal List JSON** (`--rmvListJsonPath`)
   - JSON file specifying workflows to exclude from calculations
   - Format:
   ```json
   {
     "WorkflowsList": [
       "Application - WorkflowToExclude1",
       "Application - WorkflowToExclude2"
     ]
   }
   ```

## Output

### Console Output

The application prints detailed information to the console:

- **Coverage percentage** for each application category
- **Overall workflow count** per category
- **Executed workflow count** per category
- **Not executed workflow count** per category
- **Complete lists** of:
  - Executed workflows
  - Not executed workflows
  - All workflows

Example console output:
```
***************************************************************

AvaApplication 85%

overall :100
executed :85
Not executed : 15
AvaApplication /Executedlist :  

AvaApplicationForCT - Workflow1
AvaApplicationForCT - Workflow2
...

AvaApplication /Not Executed :  
AvaApplicationForCT - Workflow3
...
```

### File Output

**JSON Report File**: `data<YYYY-MM-DD>.json`
- Created in the same directory as the input JSON files
- Contains structured data for each application category:
  ```json
  {
    "AvaApplication": {
      "WorkflowsList Count": 100,
      "ExecutedWorkflows Count": 85,
      "Percentage :": "85%",
      "ExecutedWorkflows": [...],
      "NotExecutedWorkflows": [...]
    },
    "AVViewerApplication": {...},
    ...
  }
  ```

## Dependencies

The project uses the following Maven dependencies:

- **Jackson Core & Databind** (2.16.1) - JSON processing
- **Apache Commons IO** (2.15.1) - File operations
- **JSch** (0.1.55) - SSH/SFTP file transfer (used by FileTransfer utility)
- **Picocli** (4.6.1) - Command-line interface framework
- **Java 8** - Minimum required version

## Build Instructions

### Prerequisites

- **Java JDK 8** or higher
- **Maven 3.x** installed and configured

### Building the Project

1. Navigate to the project directory:
   ```bash
   cd "c:\reno\git\Workflow Coverage\Workflow Coverage\WorkflowCoverageCalculator\AVCumulativeWorkflowCoverageCalculator"
   ```

2. Clean and build the project:
   ```bash
   mvn clean package
   ```

3. The build process will create:
   - Standard JAR: `target/AVCumulativeWorkflowCoverageCalculator-0.0.1-SNAPSHOT.jar`
   - **Executable JAR with dependencies**: `target/AVCumulativeWorkflowCoverageCalculator-0.0.1-SNAPSHOT-jar-with-dependencies.jar`

### Build Output

After successful build, you'll find:
- Compiled classes in `target/classes/`
- Packaged JAR files in `target/`
- The executable JAR includes all dependencies (Jackson, Commons IO, JSch, Picocli)

## Running the Application

### Using the Executable JAR

**Basic Command:**
```bash
java -jar AVCumulativeWorkflowCoverageCalculator-0.0.1-SNAPSHOT-jar-with-dependencies.jar --jsonPath <path_to_json_folder>
```

**With Date Parameter:**
```bash
java -jar AVCumulativeWorkflowCoverageCalculator-0.0.1-SNAPSHOT-jar-with-dependencies.jar --jsonPath <path_to_json_folder> --date 15-03-2026
```

**With Exclusion List:**
```bash
java -jar AVCumulativeWorkflowCoverageCalculator-0.0.1-SNAPSHOT-jar-with-dependencies.jar --jsonPath <path_to_json_folder> --rmvListJsonPath <path_to_removal_json>
```

**Complete Example:**
```bash
java -jar AVCumulativeWorkflowCoverageCalculator-0.0.1-SNAPSHOT-jar-with-dependencies.jar --jsonPath "C:\WorkflowData\json" --date 02-03-2026 --rmvListJsonPath "C:\WorkflowData\exclude.json"
```

### Command-Line Options

| Option | Description | Required | Example |
|--------|-------------|----------|---------|
| `--jsonPath` | Path to folder containing workflow JSON files | Yes | `C:\Data\workflows` |
| `--date` | Date in DD-MM-YYYY format | No | `15-03-2026` |
| `--rmvListJsonPath` | Path to JSON file with workflows to exclude | No | `C:\Data\exclude.json` |
| `--help` | Display help information | No | - |

### Using Maven

Alternatively, run directly with Maven:
```bash
mvn exec:java -Dexec.mainClass="main.java.com.report.AVCumulativeWorkflowCoverageCalculator" -Dexec.args="--jsonPath <path>"
```

## Special Features

### AvaApplication Coverage Calculation

The calculator has special logic for AvaApplication that:
- **Canonicalizes** workflows between AvaApplicationForMR and AvaApplicationForCT
- Maps MR workflows to their CT equivalents for unified coverage calculation
- Calculates coverage based on CT workflow count
- Example: `AvaApplicationForMR - Workflow1` is treated as `AvaApplicationForCT - Workflow1`

### Workflow Exclusion

You can exclude specific workflows from coverage calculations using the `--rmvListJsonPath` option. Excluded workflows are:
- Removed from the overall workflow list
- Not counted in coverage percentages
- Listed in console output for transparency

## Additional Utility: FileTransfer

The module includes a `FileTransfer.java` utility for SFTP file operations:

**Purpose**: Transfer files between local and remote systems via SSH/SFTP

**Usage:**
```bash
java -cp AVCumulativeWorkflowCoverageCalculator-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.report.FileTransfer <local_path> <remote_path> <remote_ip> <username> <password>
```

**Parameters:**
1. Local file path
2. Remote file path
3. Remote PC IP address
4. SSH username
5. SSH password

## Error Handling

The application handles various error scenarios:

- **Invalid JSON files**: Skips and continues to next file
- **Missing keys**: Gracefully handles missing JSON keys
- **Invalid removal list**: Throws error with format guidance
- **File write errors**: Reports inability to create output file
- **Division by zero**: Returns 0% coverage when no workflows exist

## Example Workflow

1. **Prepare input data**:
   - Collect workflow JSON files in a directory (e.g., `C:\WorkflowData`)
   - Optionally create an exclusion list JSON

2. **Build the application**:
   ```bash
   mvn clean package
   ```

3. **Run the calculator**:
   ```bash
   java -jar target/AVCumulativeWorkflowCoverageCalculator-0.0.1-SNAPSHOT-jar-with-dependencies.jar --jsonPath "C:\WorkflowData"
   ```

4. **Review results**:
   - Check console output for detailed coverage information
   - Open generated `data<date>.json` file for structured results

## Troubleshooting

**Issue**: "Wrong file in the directory" message
- **Solution**: Ensure all JSON files in the directory follow the expected format

**Issue**: Output file not created
- **Solution**: Verify write permissions in the target directory

**Issue**: "Please check the file path is correct" for removal list
- **Solution**: Verify the removal list JSON exists and follows the correct format

**Issue**: Build fails
- **Solution**: Ensure Java 8+ and Maven are properly installed and configured

## Technical Notes

- **Main Class**: `main.java.com.report.AVCumulativeWorkflowCoverageCalculator`
- **Package**: `main.java.com.report`
- **Maven Group ID**: `com.report`
- **Artifact ID**: `AVCumulativeWorkflowCoverageCalculator`
- **Version**: `0.0.1-SNAPSHOT`
- **Java Version**: 8 (source and target)
- **Encoding**: UTF-8

## License & Contact

For questions or issues regarding this module, please contact the development team.
