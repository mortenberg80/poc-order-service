#!/bin/bash

# Simple script to run IntelliJ HTTP Client tests
# Assumes the application is already running on localhost:8080
# Prerequisites: ijhttp CLI must be installed

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

HTTP_FILE="order-service-api.http"
REPORT_DIR="./test-reports"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Running Order Service API Tests${NC}"
echo -e "${BLUE}========================================${NC}"

# Check if ijhttp is installed
if ! command -v ijhttp &> /dev/null; then
    echo -e "${RED}Error: ijhttp CLI is not installed${NC}"
    echo -e "${YELLOW}Install using: brew install jetbrains/utils/ijhttp${NC}"
    exit 1
fi

# Check if HTTP test file exists
if [ ! -f "$HTTP_FILE" ]; then
    echo -e "${RED}Error: HTTP test file not found: $HTTP_FILE${NC}"
    exit 1
fi

# Create report directory
mkdir -p "$REPORT_DIR"

# Run the tests
ijhttp --log-level BASIC --report "$REPORT_DIR" "$HTTP_FILE"

echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Test reports saved to: $REPORT_DIR${NC}"
echo -e "${BLUE}========================================${NC}"
