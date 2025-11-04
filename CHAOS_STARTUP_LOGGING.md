# Chaos Engineering Startup Logging Examples

This document shows example startup logs for different chaos configurations.

## Example 1: Chaos Disabled (Default)

When starting with default configuration (`chaos.enabled=false`):

```
INFO  o.s.b.w.e.tomcat.TomcatWebServer : Tomcat started on port(s): 8080 (http)
INFO  c.e.o.chaos.ChaosStartupLogger   : 

╔════════════════════════════════════════════════════════════════╗
║          CHAOS ENGINEERING: DISABLED                           ║
╠════════════════════════════════════════════════════════════════╣
║  Service running in NORMAL MODE - no chaos injection          ║
║  All endpoints will respond normally without failures/delays  ║
║                                                                ║
║  To enable chaos:                                              ║
║  • Set chaos.enabled=true in application.yaml                 ║
║  • Use profile: --spring.profiles.active=chaos                ║
║  • Runtime: POST /actuator/chaos/scenario/{name}              ║
╚════════════════════════════════════════════════════════════════╝

INFO  c.e.o.OrderServiceApplication    : Started OrderServiceApplication in 2.345 seconds
```

## Example 2: Chaos Enabled with Scenarios (No Active Endpoints)

When starting with `chaos.enabled=true` but no per-endpoint config:

```
INFO  o.s.b.w.e.tomcat.TomcatWebServer : Tomcat started on port(s): 8080 (http)
WARN  c.e.o.chaos.ChaosStartupLogger   : 

╔════════════════════════════════════════════════════════════════╗
║          ⚡ CHAOS ENGINEERING: ENABLED ⚡                       ║
╠════════════════════════════════════════════════════════════════╣
║  WARNING: Service running with CHAOS INJECTION                 ║
║  Endpoints may experience failures and/or latency              ║
╚════════════════════════════════════════════════════════════════╝

INFO  c.e.o.chaos.ChaosStartupLogger   : Available Scenarios: 5
INFO  c.e.o.chaos.ChaosStartupLogger   :   • saga-compensation-test: Payment always fails to test compensation/rollback logic
INFO  c.e.o.chaos.ChaosStartupLogger   :   • flaky-payment: Payment service is unreliable with high failure rate
INFO  c.e.o.chaos.ChaosStartupLogger   :   • slow-services: All services respond slowly to test timeout handling
INFO  c.e.o.chaos.ChaosStartupLogger   :   • retry-test: Fail twice then succeed - tests retry logic
INFO  c.e.o.chaos.ChaosStartupLogger   :   • total-chaos: Everything is failing and slow
INFO  c.e.o.chaos.ChaosStartupLogger   : Activate scenarios via: POST /actuator/chaos/scenario/{name}
INFO  c.e.o.chaos.ChaosStartupLogger   : 
INFO  c.e.o.chaos.ChaosStartupLogger   : No per-endpoint chaos configured - using scenarios only
INFO  c.e.o.chaos.ChaosStartupLogger   : 

Chaos Management Endpoints:
  • GET  /actuator/chaos/config      - View current configuration
  • GET  /actuator/chaos/scenarios   - List available scenarios
  • POST /actuator/chaos/scenario/{name} - Activate scenario
  • POST /actuator/chaos/reset       - Reset chaos state
  • GET  /actuator/chaos/statistics  - View statistics

INFO  c.e.o.OrderServiceApplication    : Started OrderServiceApplication in 2.456 seconds
```

## Example 3: Chaos Profile Active (Per-Endpoint Configuration)

When starting with `--spring.profiles.active=chaos`:

```
INFO  o.s.b.w.e.tomcat.TomcatWebServer : Tomcat started on port(s): 8080 (http)
WARN  c.e.o.chaos.ChaosStartupLogger   : 

╔════════════════════════════════════════════════════════════════╗
║          ⚡ CHAOS ENGINEERING: ENABLED ⚡                       ║
╠════════════════════════════════════════════════════════════════╣
║  WARNING: Service running with CHAOS INJECTION                 ║
║  Endpoints may experience failures and/or latency              ║
╚════════════════════════════════════════════════════════════════╝

INFO  c.e.o.chaos.ChaosStartupLogger   : Available Scenarios: 5
INFO  c.e.o.chaos.ChaosStartupLogger   :   • saga-compensation-test: Payment always fails to test compensation/rollback logic
INFO  c.e.o.chaos.ChaosStartupLogger   :   • flaky-payment: Payment service is unreliable with high failure rate
INFO  c.e.o.chaos.ChaosStartupLogger   :   • slow-services: All services respond slowly to test timeout handling
INFO  c.e.o.chaos.ChaosStartupLogger   :   • retry-test: Fail twice then succeed - tests retry logic
INFO  c.e.o.chaos.ChaosStartupLogger   :   • total-chaos: Everything is failing and slow
INFO  c.e.o.chaos.ChaosStartupLogger   : Activate scenarios via: POST /actuator/chaos/scenario/{name}
INFO  c.e.o.chaos.ChaosStartupLogger   : 
WARN  c.e.o.chaos.ChaosStartupLogger   : Chaos Configuration by Endpoint:
WARN  c.e.o.chaos.ChaosStartupLogger   : ================================================================================
WARN  c.e.o.chaos.ChaosStartupLogger   : [place-order] Chaos ACTIVE:
WARN  c.e.o.chaos.ChaosStartupLogger   :   LATENCY: 100-500ms random delay (30% of requests)
WARN  c.e.o.chaos.ChaosStartupLogger   :   FAILURE: 10% failure rate - SERVER_ERROR (HTTP 500)
WARN  c.e.o.chaos.ChaosStartupLogger   : [process-payment] Chaos ACTIVE:
WARN  c.e.o.chaos.ChaosStartupLogger   :   LATENCY: 200-1000ms random delay (40% of requests)
WARN  c.e.o.chaos.ChaosStartupLogger   :   FAILURE: 15% failure rate - SERVICE_UNAVAILABLE (HTTP 503)
WARN  c.e.o.chaos.ChaosStartupLogger   : [ship-order] Chaos ACTIVE:
WARN  c.e.o.chaos.ChaosStartupLogger   :   LATENCY: 100-300ms random delay (20% of requests)
WARN  c.e.o.chaos.ChaosStartupLogger   :   FAILURE: 10% failure rate - TIMEOUT (HTTP 408)
WARN  c.e.o.chaos.ChaosStartupLogger   : ================================================================================
INFO  c.e.o.chaos.ChaosStartupLogger   : 

Chaos Management Endpoints:
  • GET  /actuator/chaos/config      - View current configuration
  • GET  /actuator/chaos/scenarios   - List available scenarios
  • POST /actuator/chaos/scenario/{name} - Activate scenario
  • POST /actuator/chaos/reset       - Reset chaos state
  • GET  /actuator/chaos/statistics  - View statistics

INFO  c.e.o.OrderServiceApplication    : Started OrderServiceApplication in 2.567 seconds
```

## Example 4: Custom Configuration with Fixed Latency and Consecutive Failures

When configured with:
```yaml
chaos:
  enabled: true
  endpoints:
    process-payment:
      latency:
        enabled: true
        fixed-delay-ms: 2000
        probability: 1.0
      failure:
        enabled: true
        consecutive-failures: 2
        consecutive-successes: 1
        error-type: SERVICE_UNAVAILABLE
        error-message: "Payment gateway temporarily unavailable"
```

Output:
```
WARN  c.e.o.chaos.ChaosStartupLogger   : 

╔════════════════════════════════════════════════════════════════╗
║          ⚡ CHAOS ENGINEERING: ENABLED ⚡                       ║
╠════════════════════════════════════════════════════════════════╣
║  WARNING: Service running with CHAOS INJECTION                 ║
║  Endpoints may experience failures and/or latency              ║
╚════════════════════════════════════════════════════════════════╝

WARN  c.e.o.chaos.ChaosStartupLogger   : Chaos Configuration by Endpoint:
WARN  c.e.o.chaos.ChaosStartupLogger   : ================================================================================
WARN  c.e.o.chaos.ChaosStartupLogger   : [process-payment] Chaos ACTIVE:
WARN  c.e.o.chaos.ChaosStartupLogger   :   LATENCY: Fixed 2000ms delay (100% of requests)
WARN  c.e.o.chaos.ChaosStartupLogger   :   FAILURE: fail 2x, succeed 1x, repeat - SERVICE_UNAVAILABLE (HTTP 503)
WARN  c.e.o.chaos.ChaosStartupLogger   :     └─ Message: "Payment gateway temporarily unavailable"
WARN  c.e.o.chaos.ChaosStartupLogger   : ================================================================================
```

## Example 5: Specific Endpoint Disabled

When an endpoint has chaos configuration but is explicitly disabled:
```yaml
chaos:
  enabled: true
  endpoints:
    place-order:
      enabled: false  # Explicitly disabled
      latency:
        enabled: true
        fixed-delay-ms: 1000
```

Output:
```
WARN  c.e.o.chaos.ChaosStartupLogger   : Chaos Configuration by Endpoint:
WARN  c.e.o.chaos.ChaosStartupLogger   : ================================================================================
INFO  c.e.o.chaos.ChaosStartupLogger   : [place-order] Chaos DISABLED for this endpoint
WARN  c.e.o.chaos.ChaosStartupLogger   : ================================================================================
```

## Log Levels Used

- **INFO** - Normal operation, chaos disabled, or informational messages
- **WARN** - Chaos is enabled and actively affecting endpoints
  - Used to ensure developers are aware chaos is active
  - Makes it obvious in logs when testing with chaos

## Benefits of Startup Logging

1. **Immediate Visibility** - Know chaos mode at startup without checking config
2. **Configuration Validation** - Verify settings are loaded correctly
3. **Team Communication** - Log output can be shared to show test setup
4. **Debugging Aid** - Quickly identify if unexpected behavior is chaos-related
5. **Documentation** - Logs serve as runtime documentation of chaos settings

## Troubleshooting

### Not seeing chaos logs?
1. Check log level: `logging.level.com.example.orderservice.chaos=INFO`
2. Ensure application has fully started (logs appear after "Started OrderServiceApplication")
3. Check if chaos is actually enabled in your configuration

### Logs too verbose?
Adjust log levels in `application.yaml`:
```yaml
logging:
  level:
    com.example.orderservice.chaos: WARN  # Only show warnings
```

### Want to see configuration changes?
The startup logger only runs once. To see active configuration:
```bash
curl http://localhost:8080/actuator/chaos/config
```
