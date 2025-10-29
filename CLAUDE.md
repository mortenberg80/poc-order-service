# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot Kotlin REST application that simulates order processing for distributed transaction testing. It provides endpoints for order placement, payment processing, and order shipment with full rollback functionality to support the Saga pattern in microservices architectures.

**Key Characteristics:**
- All endpoints are immutable - they create new state objects rather than modifying existing ones
- In-memory state management for tracking operation status
- Each operation (place order, process payment, ship order) has a corresponding rollback endpoint
- OpenAPI-first approach: API is defined in YAML, then code is generated

## Build and Run Commands

### Generate API Code from OpenAPI Specification
```bash
mvn clean generate-sources
```

This generates Kotlin interfaces and data classes from `src/main/resources/openapi/order-service-api.yaml` into `target/generated-sources/openapi/`.

### Build the Project
```bash
mvn clean package
```

### Run the Application
```bash
mvn spring-boot:run
```

The application starts on port 8080 by default.

### Run Tests
```bash
mvn test
```

### Run a Single Test
```bash
mvn test -Dtest=ClassName#methodName
```

## Architecture and Code Structure

### API-First Design Flow
1. **OpenAPI Specification** (`src/main/resources/openapi/order-service-api.yaml`) - Source of truth for the API
2. **Code Generation** - Maven plugin generates interfaces and models
3. **Delegate Implementation** (`OrderApiDelegateImpl`) - Implements the generated delegate interface
4. **Service Layer** (`OrderService`) - Business logic and rollback operations
5. **Repository Layer** (`OrderRepository`) - In-memory state management with ConcurrentHashMap

### Key Design Patterns

**Immutability Pattern:**
- `OrderState` is a Kotlin data class that is immutable
- All operations (place order, payment, shipment) create NEW state objects using `.copy()`
- This enables safe rollback by creating previous states without side effects

**Delegate Pattern:**
- OpenAPI Generator creates `OrderApiDelegate` interface
- `OrderApiDelegateImpl` implements this interface and delegates to `OrderService`
- This separates API concerns (HTTP, validation) from business logic

**State Machine Pattern:**
- Orders progress through states: placed → paid → shipped
- Each state transition is tracked with boolean flags and timestamps
- Validation ensures proper order (can't ship before payment)

### In-Memory State Management

`OrderRepository` uses `ConcurrentHashMap<String, OrderState>` to store order states:
- Thread-safe for concurrent access
- Key = orderId (UUID string)
- Value = OrderState (immutable snapshot)

**Why Immutable States?**
- Enables rollback by simply replacing state with previous version
- No risk of partial updates or inconsistent state
- Thread-safe without complex locking mechanisms

### Rollback Functionality

Each operation endpoint has a corresponding rollback endpoint:

| Operation | Rollback |
|-----------|----------|
| POST `/api/orders/place` | POST `/api/orders/place/rollback` |
| POST `/api/orders/payment` | POST `/api/orders/payment/rollback` |
| POST `/api/orders/ship` | POST `/api/orders/ship/rollback` |

Rollback operations reset the corresponding flags and clear related data:
- `rollbackPlaceOrder()` - Sets `orderPlaced = false`, clears `orderPlacedAt`
- `rollbackPayment()` - Sets `paymentProcessed = false`, clears `paymentId` and `paymentProcessedAt`
- `rollbackShipOrder()` - Sets `shipped = false`, clears `shipmentId`, `shippedAt`, and `shippingAddress`

## OpenAPI Specification Changes

When modifying the API:

1. Edit `src/main/resources/openapi/order-service-api.yaml`
2. Run `mvn generate-sources` to regenerate code
3. Update `OrderApiDelegateImpl` to implement new/changed methods
4. Update `OrderService` if business logic changes are needed

## Project Structure

```
src/main/kotlin/com/example/orderservice/
├── OrderServiceApplication.kt          # Spring Boot main class
├── controller/
│   └── OrderApiDelegateImpl.kt        # API implementation (connects API to service)
├── service/
│   └── OrderService.kt                # Business logic and rollback operations
├── repository/
│   └── OrderRepository.kt             # In-memory state storage
└── domain/
    └── OrderState.kt                  # Immutable state data classes

src/main/resources/
├── openapi/
│   └── order-service-api.yaml         # OpenAPI specification (source of truth)
└── application.yaml                    # Spring Boot configuration

target/generated-sources/openapi/       # Generated from OpenAPI spec (do not edit)
├── api/                               # Generated API interfaces
└── model/                             # Generated request/response models
```

## Testing the API

### Example: Complete Order Flow

```bash
# 1. Place order
curl -X POST http://localhost:8080/api/orders/place \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "items": [
      {"productId": "prod-1", "quantity": 2, "price": 29.99}
    ]
  }'
# Returns: {"orderId": "...", "status": "ORDER_PLACED", "timestamp": "..."}

# 2. Process payment
curl -X POST http://localhost:8080/api/orders/payment \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER_ID_FROM_STEP_1",
    "amount": 59.98,
    "paymentMethod": "CREDIT_CARD"
  }'

# 3. Ship order
curl -X POST http://localhost:8080/api/orders/ship \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER_ID_FROM_STEP_1",
    "shippingAddress": {
      "street": "123 Main St",
      "city": "Springfield",
      "postalCode": "12345",
      "country": "USA"
    }
  }'

# 4. Check order status
curl http://localhost:8080/api/orders/{orderId}
```

### Example: Rollback Scenario

```bash
# Rollback payment (e.g., transaction failed in another service)
curl -X POST http://localhost:8080/api/orders/payment/rollback \
  -H "Content-Type: application/json" \
  -d '{"orderId": "ORDER_ID", "reason": "External service failure"}'
```

## Important Implementation Notes

- **Thread Safety:** OrderRepository uses ConcurrentHashMap for thread-safe operations
- **State Validation:** Service layer validates state transitions (e.g., can't ship unpaid order)
- **Error Handling:** All endpoints return appropriate HTTP status codes and error messages
- **Logging:** All operations are logged at DEBUG level for troubleshooting
- **No Persistence:** This is intentionally in-memory only for testing distributed transactions
