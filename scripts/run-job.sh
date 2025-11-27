#!/bin/bash
# BatchWeaver Job Runner for Linux/Mac
# Usage: ./run-job.sh jobName=myJob [param1=value1 param2=value2 ...]

# Get the script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Change to project directory
cd "$PROJECT_DIR"

# Run the application
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar "$@"
