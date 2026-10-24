#!/bin/bash

# Find Huge Files - Dock Launcher Configuration
# Edit these values to customize your scan

# Scan settings
# Use "/" for entire filesystem (requires sudo)
# Use "/Volumes/LaCie/Dev/find-huge-files" for testing
SCAN_ROOT="/"
TOP_N=20
MIN_SIZE="0"          # e.g., "100MB", "1GB", or "0" for no minimum
EXCLUDE_PATHS="/System,/private,/dev,/proc,/sys,/run,/tmp,/var/tmp"
FOLLOW_LINKS=false

# Output settings
OUTPUT_DIR="$HOME/FindHugeFilesResults"
OUTPUT_FORMAT="all"   # console, json, csv, all
QUIET=false           # false = show progress in terminal
JAVA_OPTS="-Xmx512m"