# Order Service

A Spring Boot Kotlin REST API that simulates order processing for distributed transaction testing. This service provides endpoints for order placement, payment processing, and order shipment with full rollback functionality to support the Saga pattern in microservices architectures.

## Project Structure

This is a multi-module Maven project:

- **order-service** - The main Spring Boot service providing the REST API
- **order-service-client** - A generated Java/Kotlin client library for consuming the Order Service API

## Features

- **Immutable State Management:** All operations create new state objects rather than modifying existing ones
- **In-Memory Storage:** Uses ConcurrentHashMap for thread-safe, in-memory state tracking
- **Rollback Support:** Each operation has a corresponding rollback endpoint for distributed transaction compensation
- **OpenAPI-First:** API defined in OpenAPI specification, code generated automatically
- **Type-Safe:** Built with Kotlin for null safety and immutability
- **Client Library:** Auto-generated type-safe client for easy integration

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Build and Run

```bash
# Build all modules (service + client)
mvn clean install

# Run the service
cd order-service
mvn spring-boot:run
```

The application will start on `http://localhost:8080`.

## API Endpoints

### Order Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders/place` | Place a new order |
| POST | `/api/orders/place/rollback` | Rollback order placement |
| POST | `/api/orders/payment` | Process payment for an order |
| POST | `/api/orders/payment/rollback` | Rollback payment processing |
| POST | `/api/orders/ship` | Ship an order |
| POST | `/api/orders/ship/rollback` | Rollback order shipment |
| GET | `/api/orders/{orderId}` | Get order status |

## Example Usage

### Place Order

```bash
curl -X POST http://localhost:8080/api/orders/place \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "items": [
      {"productId": "prod-1", "quantity": 2, "price": 29.99}
    ]
  }'
```

### Process Payment

```bash
curl -X POST http://localhost:8080/api/orders/payment \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "YOUR_ORDER_ID",
    "amount": 59.98,
    "paymentMethod": "CREDIT_CARD"
  }'
```

### Ship Order

```bash
curl -X POST http://localhost:8080/api/orders/ship \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "YOUR_ORDER_ID",
    "shippingAddress": {
      "street": "123 Main St",
      "city": "Springfield",
      "postalCode": "12345",
      "country": "USA"
    }
  }'
```

### Rollback Payment

```bash
curl -X POST http://localhost:8080/api/orders/payment/rollback \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "YOUR_ORDER_ID",
    "reason": "Transaction failed"
  }'
```

### Check Order Status

```bash
curl http://localhost:8080/api/orders/YOUR_ORDER_ID
```

## Using the Client Library

The `order-service-client` module provides a type-safe Java/Kotlin client library for consuming this API. See [order-service-client/README.md](order-service-client/README.md) for detailed usage instructions.

### Quick Client Example (Kotlin)

```kotlin
import com.example.orderservice.client.api.OrderApi
import com.example.orderservice.client.infrastructure.ApiClient
import com.example.orderservice.client.model.*

val orderApi = OrderApi("http://localhost:8080")

// Place order
val orderResponse = orderApi.placeOrder(
    PlaceOrderRequest(
        customerId = "customer-123",
        items = listOf(OrderItem("prod-1", 2, 29.99.toBigDecimal()))
    )
)

// Process payment
orderApi.processPayment(
    PaymentRequest(orderResponse.orderId, 59.98.toBigDecimal(), "CREDIT_CARD")
)
```

### Adding Client to Your Project

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>order-service-client</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Architecture

The application follows an API-first approach:

1. **OpenAPI Specification** (`src/main/resources/openapi/order-service-api.yaml`) - Defines the API contract
2. **Code Generation** - Maven plugin generates Kotlin interfaces and models
3. **Delegate Implementation** - Implements the generated interfaces
4. **Service Layer** - Contains business logic and rollback operations
5. **Repository Layer** - Manages in-memory state

### Immutable State Design

All operations are immutable and create new state objects:

- Orders are represented by `OrderState` data class
- Each operation creates a new state using Kotlin's `.copy()` method
- Enables safe rollback without side effects
- Thread-safe without complex locking

## Development

### Modifying the API

1. Edit `src/main/resources/openapi/order-service-api.yaml`
2. Run `mvn generate-sources` to regenerate code
3. Update `OrderApiDelegateImpl` to implement changes
4. Update `OrderService` if business logic changes are needed

### Running Tests

```bash
mvn test
```

## Project Structure

```
order-service/                          # Root multi-module project
├── pom.xml                            # Parent POM with module declarations
├── README.md                          # This file
├── order-service/                     # Main service module
│   ├── src/main/
│   │   ├── kotlin/com/example/orderservice/
│   │   │   ├── OrderServiceApplication.kt
│   │   │   ├── controller/OrderApiDelegateImpl.kt
│   │   │   ├── service/OrderService.kt
│   │   │   ├── repository/OrderRepository.kt
│   │   │   └── domain/OrderState.kt
│   │   └── resources/
│   │       ├── openapi/order-service-api.yaml    # API specification (source of truth)
│   │       └── application.yaml
│   └── pom.xml
└── order-service-client/              # Generated client library
    ├── src/main/kotlin/               # Optional custom utilities
    ├── target/generated-sources/      # Generated client code
    ├── pom.xml
    └── README.md                      # Client usage documentation
```

## License

This is a proof-of-concept project for testing distributed transactions.
