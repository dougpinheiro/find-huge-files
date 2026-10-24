# Find Huge Files

A Java application that scans the filesystem and lists the largest files with their absolute paths and sizes.

## Features

- **Efficient scanning**: Uses a min-heap to track only the top N files (constant memory)
- **Fast**: Parallel-friendly file tree walking with `Files.walkFileTree`
- **Safe**: Gracefully handles permission errors, skips system paths by default
- **Flexible output**: Console table, JSON, CSV, or all formats
- **Cross-platform**: Works on macOS, Linux, and Windows

## Building

Requires Java 17+ and Maven.

```bash
mvn clean package
```

This creates a fat JAR at `target/find-huge-files-1.0.0.jar`.

## Usage

### Basic Usage (Top 20 files, console table)

```bash
java -jar target/find-huge-files-1.0.0.jar
```

### Custom Root Directory

```bash
java -jar target/find-huge-files-1.0.0.jar /home/user
```

### Top 50 Files with JSON Export

```bash
java -jar target/find-huge-files-1.0.0.jar / --top 50 --output results.json --format json
```

### Both Console and File Output (JSON + CSV)

```bash
java -jar target/find-huge-files-1.0.0.jar / --top 20 --output scan_results --format all
```

### Minimum File Size Filter

```bash
java -jar target/find-huge-files-1.0.0.jar / --min-size 100MB
```

### Custom Excluded Paths

```bash
java -jar target/find-huge-files-1.0.0.jar / --exclude "/mnt,/media,/home/user/.cache"
```

### Simple Output Format

```bash
java -jar target/find-huge-files-1.0.0.jar / --simple
```

### Quiet Mode (No Progress)

```bash
java -jar target/find-huge-files-1.0.0.jar / --quiet
```

## Command Line Options

| Option | Short | Description | Default |
|--------|-------|-------------|---------|
| `--top` | `-n` | Number of largest files to show | 20 |
| `--output` | `-o` | Output file path (creates .json and .csv) | - |
| `--format` | `-f` | Output format: console, json, csv, all | console |
| `--exclude` | `-e` | Comma-separated paths to exclude | System paths |
| `--min-size` | | Minimum file size (e.g., 10MB, 1GB) | 0 |
| `--follow-links` | `-L` | Follow symbolic links | false |
| `--progress` | `-p` | Progress interval (0 to disable) | 10000 |
| `--quiet` | `-q` | Suppress progress output | false |
| `--simple` | | Use simple list format | false |
| `--help` | `-h` | Show help | - |
| `--version` | `-V` | Show version | - |

## Output Formats

### Console Table (Default)

```
Rank  Size       Modified              Owner  Path
────────────────────────────────────────────────────────────────────────
1     15.23 GB   2024-01-15 10:30:45   user   /Users/user/Library/Containers/com.docker.docker/Data/vms/0/data/Docker.raw
2      8.45 GB   2024-01-10 14:22:10   user   /Users/user/Downloads/movie.mkv
3      4.12 GB   2024-01-05 09:15:33   root   /var/log/system.log
...
```

### JSON Output

```json
{
  "totalFiles": 20,
  "totalSizeBytes": 34567890123,
  "scanTime": "2024-01-20T15:30:45.123Z",
  "files": [
    {
      "fileName": "Docker.raw",
      "absolutePath": "/Users/user/Library/Containers/com.docker.docker/Data/vms/0/data/Docker.raw",
      "sizeBytes": 16357785600,
      "formattedSize": "15.23 GB",
      "lastModified": "2024-01-15T10:30:45Z",
      "owner": "user"
    }
  ]
}
```

### CSV Output

```csv
Rank,File Name,Absolute Path,Size (Bytes),Size (Human),Last Modified,Owner
1,Docker.raw,/Users/user/Library/Containers/com.docker.docker/Data/vms/0/data/Docker.raw,16357785600,15.23 GB,2024-01-15T10:30:45,user
```

## Default Excluded Paths

These paths are excluded by default to avoid system/virtual filesystems:

- `/System` (macOS system)
- `/private` (macOS private)
- `/dev` (Device files)
- `/Volumes` (Mount points - virtual)
- `/proc` (Linux process info)
- `/sys` (Linux sysfs)
- `/run` (Runtime data)
- `/tmp`, `/var/tmp` (Temporary files)

## Performance Tips

1. **Run as root/sudo** for complete filesystem access
2. **Use `--min-size`** to skip tiny files (e.g., `--min-size 10MB`)
3. **Exclude mount points** with `--exclude "/mnt,/media,/Volumes"`
4. **Disable progress** with `--quiet` for slightly faster scans

## Example: Find Files Larger Than 1GB on Entire Disk

```bash
sudo java -jar target/find-huge-files-1.0.0.jar / --min-size 1GB --top 50 --output huge_files --format all --quiet
```

## License

MIT