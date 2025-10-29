#!/bin/bash

# Script to run IntelliJ HTTP Client tests for Order Service API
# Prerequisites: ijhttp CLI must be installed
# Installation: https://github.com/JetBrains/http-request-in-editor-spec/blob/master/cli.md

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
APP_PORT=8080
BASE_URL="http://localhost:${APP_PORT}"
HTTP_FILE="order-service-api.http"
MAX_WAIT_TIME=60
REPORT_DIR="./test-reports"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Order Service API Test Runner${NC}"
echo -e "${BLUE}========================================${NC}"

# Check if ijhttp is installed
if ! command -v ijhttp &> /dev/null; then
    echo -e "${RED}Error: ijhttp CLI is not installed${NC}"
    echo -e "${YELLOW}Please install it from: https://github.com/JetBrains/http-request-in-editor-spec/blob/master/cli.md${NC}"
    echo -e "${YELLOW}Or using Homebrew: brew install jetbrains/utils/ijhttp${NC}"
    exit 1
fi

echo -e "${GREEN}✓ ijhttp CLI found: $(ijhttp --version)${NC}"

# Check if HTTP test file exists
if [ ! -f "$HTTP_FILE" ]; then
    echo -e "${RED}Error: HTTP test file not found: $HTTP_FILE${NC}"
    exit 1
fi

echo -e "${GREEN}✓ HTTP test file found: $HTTP_FILE${NC}"

# Function to check if application is running
check_app_running() {
    curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" 2>/dev/null || echo "000"
}

# Function to wait for application to be ready
wait_for_app() {
    echo -e "${YELLOW}Waiting for application to be ready at $BASE_URL...${NC}"
    local elapsed=0
    local interval=2

    while [ $elapsed -lt $MAX_WAIT_TIME ]; do
        local status=$(check_app_running)

        if [ "$status" = "200" ] || [ "$status" = "404" ]; then
            echo -e "${GREEN}✓ Application is ready!${NC}"
            return 0
        fi

        echo -n "."
        sleep $interval
        elapsed=$((elapsed + interval))
    done

    echo -e "${RED}✗ Application did not start within ${MAX_WAIT_TIME} seconds${NC}"
    return 1
}

# Check if we need to start the application
APP_STATUS=$(check_app_running)

if [ "$APP_STATUS" = "000" ]; then
    echo -e "${YELLOW}Application is not running. Attempting to start...${NC}"
    echo -e "${YELLOW}Note: This script assumes the application can be started with 'mvn spring-boot:run'${NC}"
    echo -e "${YELLOW}If the application is already running elsewhere, press Ctrl+C and run tests only${NC}"

    # Start application in background
    mvn spring-boot:run > /dev/null 2>&1 &
    APP_PID=$!
    echo -e "${BLUE}Started application with PID: $APP_PID${NC}"

    # Wait for application to be ready
    if ! wait_for_app; then
        echo -e "${RED}Failed to start application. Killing process...${NC}"
        kill $APP_PID 2>/dev/null || true
        exit 1
    fi

    STARTED_APP=true
else
    echo -e "${GREEN}✓ Application is already running${NC}"
    STARTED_APP=false
fi

# Create report directory
mkdir -p "$REPORT_DIR"

# Run the tests
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Running API Tests...${NC}"
echo -e "${BLUE}========================================${NC}"

# Run ijhttp with the HTTP file
# --report flag generates a report in the specified directory
# --log-level controls the verbosity of the output
if ijhttp --log-level BASIC --report "$REPORT_DIR" "$HTTP_FILE"; then
    echo -e "${BLUE}========================================${NC}"
    echo -e "${GREEN}✓ All tests completed successfully!${NC}"
    echo -e "${BLUE}========================================${NC}"
    TEST_EXIT_CODE=0
else
    echo -e "${BLUE}========================================${NC}"
    echo -e "${RED}✗ Some tests failed${NC}"
    echo -e "${BLUE}========================================${NC}"
    TEST_EXIT_CODE=1
fi

# Display test report location
echo -e "${BLUE}Test reports saved to: $REPORT_DIR${NC}"

# Cleanup: Stop the application if we started it
if [ "$STARTED_APP" = true ]; then
    echo -e "${YELLOW}Stopping application (PID: $APP_PID)...${NC}"
    kill $APP_PID 2>/dev/null || true
    wait $APP_PID 2>/dev/null || true
    echo -e "${GREEN}✓ Application stopped${NC}"
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Test run complete${NC}"
echo -e "${BLUE}========================================${NC}"

exit $TEST_EXIT_CODE
