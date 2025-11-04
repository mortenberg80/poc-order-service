# Chaos Engineering Guide

This service includes comprehensive chaos engineering capabilities for testing distributed transaction scenarios, Saga patterns, and resilience mechanisms.

## Table of Contents
- [Quick Start](#quick-start)
- [Configuration Options](#configuration-options)
- [Scenarios](#scenarios)
- [Runtime Control](#runtime-control)
- [Use Cases](#use-cases)
- [Examples](#examples)

## Quick Start

### Enable Chaos via Configuration

**Option 1: Using Spring Profile**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=chaos
```

On startup, you'll see chaos configuration logged:
```
╔════════════════════════════════════════════════════════════════╗
║          ⚡ CHAOS ENGINEERING: ENABLED ⚡                       ║
╠════════════════════════════════════════════════════════════════╣
║  WARNING: Service running with CHAOS INJECTION                 ║
║  Endpoints may experience failures and/or latency              ║
╚════════════════════════════════════════════════════════════════╝
```

**Option 2: Via Environment Variable**
```bash
export CHAOS_ENABLED=true
mvn spring-boot:run
```

**Option 3: Via Command Line**
```bash
java -jar order-service.jar --chaos.enabled=true
```

**Note:** The service logs detailed chaos configuration on startup, showing which endpoints are affected and how. See [CHAOS_STARTUP_LOGGING.md](CHAOS_STARTUP_LOGGING.md) for examples.

### Enable Chaos at Runtime

```bash
# Activate a scenario
curl -X POST http://localhost:8080/actuator/chaos/scenario/flaky-payment

# Deactivate scenario
curl -X POST http://localhost:8080/actuator/chaos/scenario/deactivate

# Reset all chaos states
curl -X POST http://localhost:8080/actuator/chaos/reset
```

## Configuration Options

### Per-Endpoint Configuration

Configure chaos for specific endpoints in `application.yaml`:

```yaml
chaos:
  enabled: true
  endpoints:
    place-order:
      enabled: true
      latency:
        enabled: true
        min-delay-ms: 100      # Minimum latency
        max-delay-ms: 500      # Maximum latency (random between min-max)
        fixed-delay-ms: null   # Or use fixed delay instead
        probability: 0.3       # Apply to 30% of requests
      failure:
        enabled: true
        failure-rate: 0.1      # 10% of requests fail
        error-type: SERVER_ERROR
        http-status: 500       # Optional: override default status
        error-message: "Custom error message"
        consecutive-failures: 0  # 0 = random, N = fail N times then succeed
        consecutive-successes: 0 # After N failures, succeed M times
```

### Error Types

Available error types:
- `TIMEOUT` (408)
- `SERVER_ERROR` (500)
- `SERVICE_UNAVAILABLE` (503)
- `BAD_REQUEST` (400)
- `CONFLICT` (409)
- `RATE_LIMIT` (429)
- `NETWORK_ERROR` (503)
- `RANDOM` (picks random from above)

### Endpoint Names

Supported endpoint identifiers:
- `place-order` - POST /api/orders/place
- `rollback-place-order` - POST /api/orders/place/rollback
- `process-payment` - POST /api/orders/payment
- `rollback-payment` - POST /api/orders/payment/rollback
- `ship-order` - POST /api/orders/ship
- `rollback-ship-order` - POST /api/orders/ship/rollback
- `get-order-status` - GET /api/orders/{orderId}

## Scenarios

Pre-configured scenarios for common testing patterns:

### 1. saga-compensation-test
**Purpose:** Test Saga compensation/rollback logic  
**Behavior:** Payment always fails (100% failure rate)

```bash
curl -X POST http://localhost:8080/actuator/chaos/scenario/saga-compensation-test
```

### 2. flaky-payment
**Purpose:** Test retry and error handling  
**Behavior:** 
- Payment has 40% failure rate
- 1-3 second latency on 80% of requests
- Random error types

### 3. slow-services
**Purpose:** Test timeout handling  
**Behavior:** All services respond with 2-3 second delays

### 4. retry-test
**Purpose:** Test retry logic with consecutive failures  
**Behavior:** Fail 2 times, then succeed once, repeat

### 5. total-chaos
**Purpose:** Stress test the entire system  
**Behavior:** 
- All endpoints have random failures (25-40%)
- Variable latency (500ms - 3000ms)
- High probability of chaos injection

## Runtime Control

### Management Endpoints

All chaos management endpoints are under `/actuator/chaos`:

#### Get Configuration
```bash
curl http://localhost:8080/actuator/chaos/config
```

Response:
```json
{
  "enabled": true,
  "endpoints": {
    "place-order": { ... },
    "process-payment": { ... }
  },
  "scenarios": { ... }
}
```

#### List Scenarios
```bash
curl http://localhost:8080/actuator/chaos/scenarios
```

Response:
```json
{
  "saga-compensation-test": "Payment always fails to test compensation/rollback logic",
  "flaky-payment": "Payment service is unreliable with high failure rate",
  "slow-services": "All services respond slowly to test timeout handling"
}
```

#### Activate Scenario
```bash
curl -X POST http://localhost:8080/actuator/chaos/scenario/flaky-payment
```

#### Deactivate Scenario
```bash
curl -X POST http://localhost:8080/actuator/chaos/scenario/deactivate
```

#### Reset States
Clears all failure counters and deactivates scenarios:
```bash
curl -X POST http://localhost:8080/actuator/chaos/reset
```

#### View Statistics
```bash
curl http://localhost:8080/actuator/chaos/statistics
```

Response:
```json
{
  "chaosEnabled": true,
  "activeScenario": "flaky-payment",
  "endpointStates": {
    "place-order": {
      "failureCount": 5,
      "successCount": 0,
      "lastFailureTime": 1699012345678
    }
  }
}
```

#### Health Check
```bash
curl http://localhost:8080/actuator/chaos/health
```

### Per-Request Scenario Override

Use HTTP header to activate a scenario for a single request:

```bash
curl -X POST http://localhost:8080/api/orders/place \
  -H "Content-Type: application/json" \
  -H "X-Chaos-Scenario: saga-compensation-test" \
  -d '{ "customerId": "cust-123", "items": [...] }'
```

## Use Cases

### 1. Testing Saga Compensation

**Scenario:** Order placement succeeds, but payment fails. Test that the order is rolled back.

```bash
# Activate saga compensation scenario
curl -X POST http://localhost:8080/actuator/chaos/scenario/saga-compensation-test

# Place an order (succeeds)
ORDER_RESPONSE=$(curl -X POST http://localhost:8080/api/orders/place \
  -H "Content-Type: application/json" \
  -d '{"customerId":"cust-123","items":[{"productId":"prod-1","quantity":2,"price":29.99}]}')

ORDER_ID=$(echo $ORDER_RESPONSE | jq -r '.orderId')

# Try to process payment (will fail)
curl -X POST http://localhost:8080/api/orders/payment \
  -H "Content-Type: application/json" \
  -d "{\"orderId\":\"$ORDER_ID\",\"amount\":59.98,\"paymentMethod\":\"CREDIT_CARD\"}"

# Should return 500 error - your orchestrator should trigger rollback
```

### 2. Testing Retry Logic

**Scenario:** Service fails N times before succeeding

```bash
# Activate retry test scenario (fails 2x, succeeds 1x)
curl -X POST http://localhost:8080/actuator/chaos/scenario/retry-test

# First attempt - fails
curl -X POST http://localhost:8080/api/orders/place ...  # Returns 503

# Second attempt - fails
curl -X POST http://localhost:8080/api/orders/place ...  # Returns 503

# Third attempt - succeeds
curl -X POST http://localhost:8080/api/orders/place ...  # Returns 200
```

### 3. Testing Timeout Handling

**Scenario:** Service responds slowly to test timeout configuration

```bash
# Activate slow services scenario
curl -X POST http://localhost:8080/actuator/chaos/scenario/slow-services

# This will take 2-3 seconds per endpoint
# Ensure your client has appropriate timeouts configured
```

### 4. Testing Circuit Breakers

**Scenario:** Continuous failures to trigger circuit breaker

```bash
# Activate total chaos
curl -X POST http://localhost:8080/actuator/chaos/scenario/total-chaos

# Make multiple requests - high failure rate should trigger circuit breaker
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/orders/place ...
done
```

### 5. Testing Partial Failures

**Scenario:** Some operations succeed, others fail

```yaml
# Custom configuration
chaos:
  enabled: true
  endpoints:
    place-order:
      failure:
        enabled: false  # Always succeeds
    process-payment:
      failure:
        enabled: true
        failure-rate: 0.5  # 50% fail
    ship-order:
      failure:
        enabled: false  # Always succeeds
```

## Examples

### Example 1: Complete Saga Test

```bash
#!/bin/bash

# Reset chaos state
curl -X POST http://localhost:8080/actuator/chaos/reset

# Enable saga compensation scenario
curl -X POST http://localhost:8080/actuator/chaos/scenario/saga-compensation-test

echo "Testing Saga compensation..."

# Place order (should succeed)
echo "1. Placing order..."
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/orders/place \
  -H "Content-Type: application/json" \
  -d '{"customerId":"test-customer","items":[{"productId":"prod-1","quantity":1,"price":100.0}]}')

ORDER_ID=$(echo $ORDER_RESPONSE | jq -r '.orderId')
echo "Order created: $ORDER_ID"

# Try payment (should fail due to chaos)
echo "2. Processing payment (expecting failure)..."
PAYMENT_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8080/api/orders/payment \
  -H "Content-Type: application/json" \
  -d "{\"orderId\":\"$ORDER_ID\",\"amount\":100.0,\"paymentMethod\":\"CREDIT_CARD\"}")

HTTP_CODE=$(echo "$PAYMENT_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" -eq 500 ]; then
  echo "Payment failed as expected (HTTP $HTTP_CODE)"
  
  # Rollback order
  echo "3. Rolling back order..."
  curl -s -X POST http://localhost:8080/api/orders/place/rollback \
    -H "Content-Type: application/json" \
    -d "{\"orderId\":\"$ORDER_ID\",\"reason\":\"Payment failed\"}"
  
  echo "Saga compensation completed successfully!"
else
  echo "ERROR: Payment should have failed but returned HTTP $HTTP_CODE"
fi

# Cleanup
curl -s -X POST http://localhost:8080/actuator/chaos/reset > /dev/null
```

### Example 2: Load Testing with Chaos

```bash
#!/bin/bash

# Enable flaky payment scenario
curl -X POST http://localhost:8080/actuator/chaos/scenario/flaky-payment

# Run load test with chaos
echo "Running load test with chaos enabled..."
for i in {1..100}; do
  (curl -s -X POST http://localhost:8080/api/orders/place \
    -H "Content-Type: application/json" \
    -d "{\"customerId\":\"load-test-$i\",\"items\":[{\"productId\":\"prod-1\",\"quantity\":1,\"price\":10.0}]}" \
    > /dev/null &)
done

wait
echo "Load test completed"

# Check statistics
curl http://localhost:8080/actuator/chaos/statistics
```

### Example 3: Integration Test with Chaos

```kotlin
@SpringBootTest
@TestPropertySource(properties = ["chaos.enabled=true"])
class SagaIntegrationTest {
    
    @Test
    fun `test saga compensation with payment failure`() {
        // Activate scenario programmatically
        restTemplate.postForEntity(
            "http://localhost:8080/actuator/chaos/scenario/saga-compensation-test",
            null,
            Map::class.java
        )
        
        // Place order
        val orderResponse = orderApi.placeOrder(createTestOrder())
        assertThat(orderResponse.status).isEqualTo("ORDER_PLACED")
        
        // Try payment - should fail
        assertThrows<ServerException> {
            orderApi.processPayment(createPaymentRequest(orderResponse.orderId))
        }
        
        // Verify rollback
        orderApi.rollbackPlaceOrder(createRollbackRequest(orderResponse.orderId))
        
        // Cleanup
        restTemplate.postForEntity(
            "http://localhost:8080/actuator/chaos/reset",
            null,
            Map::class.java
        )
    }
}
```

## Best Practices

1. **Always reset after tests**: Use `/actuator/chaos/reset` to clear state
2. **Use scenarios**: Define reusable scenarios instead of manual configuration
3. **Monitor statistics**: Check `/actuator/chaos/statistics` to understand behavior
4. **Disable in production**: Keep `chaos.enabled=false` in production
5. **Test incrementally**: Start with one endpoint, then add complexity
6. **Document your scenarios**: Add clear descriptions for team understanding
7. **Combine with monitoring**: Use with APM tools to observe impact

## Troubleshooting

### Chaos not working?
1. Check `chaos.enabled=true`
2. Verify endpoint name matches configuration
3. Check logs for "Injecting chaos" messages
4. Verify interceptor is registered

### Unexpected behavior?
1. Check active scenario: `GET /actuator/chaos/config`
2. View statistics: `GET /actuator/chaos/statistics`
3. Reset state: `POST /actuator/chaos/reset`

### Performance impact?
- Latency injection uses `Thread.sleep()` - blocking
- Minimal CPU overhead
- Memory usage negligible (state tracking)
- Disable when not needed for testing

## Advanced Configuration

### Custom Scenario in Code

```kotlin
@Configuration
class CustomChaosConfig {
    @Bean
    fun customScenario(): ScenarioConfig {
        return ScenarioConfig(
            description = "Custom test scenario",
            endpoints = mapOf(
                "process-payment" to EndpointChaos(
                    failure = FailureConfig(
                        enabled = true,
                        failureRate = 0.75,
                        errorType = ChaosErrorType.TIMEOUT
                    )
                )
            )
        )
    }
}
```

### Environment-Specific Configuration

```yaml
# application-dev.yaml
chaos:
  enabled: false

# application-test.yaml  
chaos:
  enabled: true
  # ... test-specific config

# application-staging.yaml
chaos:
  enabled: true
  # ... staging chaos config
```

## Summary

The chaos engineering features provide:
- ✅ Latency injection (fixed or random)
- ✅ Failure simulation (random or consecutive)
- ✅ Multiple error types
- ✅ Named scenarios
- ✅ Runtime control via REST API
- ✅ Per-request scenario override
- ✅ Statistics and monitoring
- ✅ Zero production impact when disabled

Perfect for testing:
- Saga patterns
- Distributed transactions
- Retry logic
- Timeout handling
- Circuit breakers
- Error compensation
- Resilience mechanisms
