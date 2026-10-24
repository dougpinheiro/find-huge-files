# Find Huge Files - Dock App

A macOS app bundle that scans your filesystem for the largest files.

## Installation

1. Build the project:
   ```bash
   cd /Volumes/LaCie/Dev/find-huge-files
   mvn clean package
   ```

2. The app bundle is at: `FindHugeFiles.app`

3. Drag `FindHugeFiles.app` to your Dock

## Usage

1. Click the app in your Dock
2. A Terminal window will open
3. Enter your sudo password when prompted
4. Wait for the scan to complete
5. Results are saved to `~/FindHugeFilesResults/` and the folder opens automatically

## Configuration

Edit `FindHugeFiles.app/Contents/Resources/config.sh` to customize:

- `SCAN_ROOT` - Directory to scan (default: `/` for entire disk)
- `TOP_N` - Number of largest files to show (default: 20)
- `MIN_SIZE` - Minimum file size filter (e.g., `100MB`, `1GB`)
- `EXCLUDE_PATHS` - Comma-separated paths to exclude
- `OUTPUT_FORMAT` - `console`, `json`, `csv`, or `all`
- `OUTPUT_DIR` - Where to save results (default: `~/FindHugeFilesResults`)

## Requirements

- Java 17+ (for running the JAR)
- macOS (for the app bundle)
- sudo access (for scanning system directories)

## Output

Results are saved as:
- `scan_YYYYMMDD_HHMMSS.json` - Structured JSON with metadata
- `scan_YYYYMMDD_HHMMSS.csv` - Spreadsheet-compatible CSV