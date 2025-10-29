# Order Service API Tests

This directory contains IntelliJ HTTP Client tests for the Order Service API and bash scripts to run them using the `ijhttp` CLI tool.

## Files

- **order-service-api.http** - HTTP Client test file with comprehensive API tests
- **run-api-tests.sh** - Full test runner that starts the application, runs tests, and stops the application
- **run-api-tests-simple.sh** - Simple test runner for pre-running applications

## Prerequisites

### Install ijhttp CLI

The `ijhttp` CLI is required to run the tests from the command line.

**macOS (using Homebrew):**
```bash
brew install jetbrains/utils/ijhttp
```

**Manual installation:**
Download from the [official repository](https://github.com/JetBrains/http-request-in-editor-spec/blob/master/cli.md)

**Verify installation:**
```bash
ijhttp --version
```

## Running Tests

### Option 1: Full Automated Test Run

This script will start the application, wait for it to be ready, run all tests, and then stop the application:

```bash
./run-api-tests.sh
```

**Features:**
- Automatically starts the Spring Boot application
- Waits for the application to be ready
- Runs all API tests
- Generates test reports
- Stops the application after tests complete

### Option 2: Simple Test Run (App Already Running)

If your application is already running on `localhost:8080`, use this script:

```bash
./run-api-tests-simple.sh
```

**Prerequisites:**
- Application must be running before executing the script
- Start the app manually with: `mvn spring-boot:run`

### Option 3: Run Tests in IntelliJ IDEA

1. Open `order-service-api.http` in IntelliJ IDEA
2. Ensure the application is running
3. Click the green arrow next to any request to run it individually
4. Or use "Run All Requests in File" to execute all tests

## Test Structure

The test file includes three main test categories:

### 1. Happy Path Tests (Tests 1-6)
- Place an order
- Check order status after placement
- Process payment
- Check order status after payment
- Ship the order
- Verify final order status

### 2. Rollback Tests (Tests 7-15)
- Place order, process payment, and ship
- Rollback shipment and verify
- Rollback payment and verify
- Rollback order placement and verify

### 3. Error Scenario Tests (Tests 16-19)
- Attempt to ship without payment (should fail)
- Get non-existent order (should return 404)
- Process payment for non-existent order (should fail)

## Test Reports

After running tests with the bash scripts, reports are saved to:
```
./test-reports/
```

The reports include:
- Test execution results
- Response times
- Success/failure status for each request

## Test Variables

The test file uses IntelliJ HTTP Client variables to track state across requests:

- `{{orderId}}` - Order ID from the first test flow
- `{{rollbackOrderId}}` - Order ID for rollback testing
- `{{errorTestOrderId}}` - Order ID for error scenario testing
- `{{paymentId}}` - Payment ID from payment processing
- `{{shipmentId}}` - Shipment ID from order shipping

These variables are automatically set and used across test requests.

## Customizing Tests

### Modify Base URL

To run tests against a different environment, edit the `@baseUrl` variable in `order-service-api.http`:

```
@baseUrl = http://your-server:port
```

### Add New Tests

Follow the existing pattern:

```http
### Test Name
POST {{baseUrl}}/api/endpoint
Content-Type: {{contentType}}

{
  "field": "value"
}

> {%
    client.test("Test description", function() {
        client.assert(response.status === 200, "Response status check");
        client.assert(response.body.field !== undefined, "Field exists");
    });
    client.global.set("variableName", response.body.field);
%}
```

## Troubleshooting

### Application Not Starting

If the application fails to start in `run-api-tests.sh`:
- Check if port 8080 is already in use: `lsof -i :8080`
- Verify Maven is configured correctly: `mvn --version`
- Check application logs for errors

### Tests Failing

If tests fail:
1. Verify the application is running: `curl http://localhost:8080/actuator/health`
2. Check test-reports directory for detailed error messages
3. Run individual tests in IntelliJ IDEA for debugging
4. Verify the OpenAPI spec matches the implementation

### ijhttp Not Found

If the script reports `ijhttp` is not installed:
```bash
# Install using Homebrew
brew install jetbrains/utils/ijhttp

# Or follow manual installation instructions
# https://github.com/JetBrains/http-request-in-editor-spec/blob/master/cli.md
```

## CI/CD Integration

To integrate these tests into your CI/CD pipeline:

```yaml
# Example GitHub Actions
- name: Run API Tests
  run: |
    mvn spring-boot:run &
    sleep 10
    ./run-api-tests-simple.sh
```

## References

- [IntelliJ HTTP Client Documentation](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html)
- [ijhttp CLI Documentation](https://github.com/JetBrains/http-request-in-editor-spec/blob/master/cli.md)
- [Order Service API Documentation](./README.md)
