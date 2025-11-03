# Order Service Client - Kotlinx Serialization

Kotlin client library for the Order Service API using **kotlinx.serialization** and **Ktor client**. This library provides a purely Kotlin-native approach to consuming the Order Service.

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>order-service-client-kotlinx</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Why Kotlinx Serialization?

This client uses **kotlinx.serialization** instead of Jackson, providing:
- **Pure Kotlin**: Native Kotlin library with first-class language support
- **Compile-time safety**: Serialization checked at compile time
- **Lightweight**: Smaller runtime footprint than Jackson
- **Multiplatform ready**: Can be used in Kotlin Multiplatform projects
- **Better null-safety**: Leverages Kotlin's null-safety features
- **No reflection by default**: Better performance and security

## HTTP Client: Ktor

Uses **Ktor Client** (CIO engine) for HTTP communication:
- **Kotlin-first**: Designed specifically for Kotlin
- **Coroutine support**: Built-in async/await patterns (optional)
- **Lightweight**: Minimal dependencies
- **Flexible**: Easy to configure and extend
- **Multiplatform**: Works across JVM, JS, and Native

## Usage

### Basic Example

```kotlin
import com.example.orderservice.client.kotlinx.api.OrderApi
import com.example.orderservice.client.kotlinx.model.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun main() {
    // Create HTTP client with kotlinx.serialization
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }
    
    // Create API client
    val orderApi = OrderApi("http://localhost:8080", httpClient)
    
    // Place an order
    val placeOrderRequest = PlaceOrderRequest(
        customerId = "customer-123",
        items = listOf(
            OrderItem(
                productId = "prod-1",
                quantity = 2,
                price = 29.99
            )
        )
    )
    
    val orderResponse = orderApi.placeOrder(placeOrderRequest)
    println("Order placed: ${orderResponse.orderId}")
    
    // Process payment
    val paymentRequest = PaymentRequest(
        orderId = orderResponse.orderId,
        amount = 59.98,
        paymentMethod = "CREDIT_CARD"
    )
    
    val paymentResponse = orderApi.processPayment(paymentRequest)
    println("Payment processed: ${paymentResponse.paymentId}")
    
    // Ship order
    val shipOrderRequest = ShipOrderRequest(
        orderId = orderResponse.orderId,
        shippingAddress = Address(
            street = "123 Main St",
            city = "Springfield",
            postalCode = "12345",
            country = "USA"
        )
    )
    
    val shipOrderResponse = orderApi.shipOrder(shipOrderRequest)
    println("Order shipped: ${shipOrderResponse.shipmentId}")
    
    // Get order status
    val orderStatus = orderApi.getOrderStatus(orderResponse.orderId)
    println("Order status: placed=${orderStatus.orderPlaced}, paid=${orderStatus.paymentProcessed}, shipped=${orderStatus.shipped}")
    
    // Clean up
    httpClient.close()
}
```

### With Custom Configuration

```kotlin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

val httpClient = HttpClient(CIO) {
    // Timeouts
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 30_000
    }
    
    // Content negotiation with custom JSON config
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        })
    }
    
    // Logging (optional)
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.INFO
    }
    
    // Default request configuration
    defaultRequest {
        header("User-Agent", "OrderServiceClient/1.0")
    }
}

val orderApi = OrderApi("http://localhost:8080", httpClient)
```

### Rollback Operations

```kotlin
// Rollback payment
val rollbackRequest = RollbackRequest(
    orderId = orderId,
    reason = "External service failure"
)
orderApi.rollbackPayment(rollbackRequest)

// Rollback shipment
orderApi.rollbackShipOrder(rollbackRequest)

// Rollback entire order
orderApi.rollbackPlaceOrder(rollbackRequest)
```

## Error Handling

```kotlin
import io.ktor.client.plugins.*
import io.ktor.http.*

try {
    orderApi.placeOrder(placeOrderRequest)
} catch (e: ClientRequestException) {
    // HTTP 4xx errors
    println("Client error: ${e.response.status} - ${e.message}")
} catch (e: ServerResponseException) {
    // HTTP 5xx errors
    println("Server error: ${e.response.status} - ${e.message}")
} catch (e: ResponseException) {
    // Other HTTP errors
    println("HTTP error: ${e.response.status}")
}
```

## Comparison with Jackson Client

| Feature | Kotlinx Client | Jackson Client |
|---------|---------------|----------------|
| Serialization | kotlinx.serialization | Jackson |
| HTTP Client | Ktor | OkHttp4 |
| Language | Kotlin-first | Java/Kotlin |
| Null-safety | Compile-time | Runtime |
| Performance | Lightweight, no reflection | Mature, reflection-based |
| Size | Smaller footprint | Larger footprint |
| Multiplatform | Yes | No |
| Maturity | Modern, growing | Battle-tested |
| Best for | Pure Kotlin projects | Java/Kotlin mixed projects |

## When to Use This Client

**Choose kotlinx.serialization client when:**
- Building a pure Kotlin project
- Need multiplatform support
- Want compile-time serialization safety
- Prefer lightweight dependencies
- Working with Kotlin coroutines/async patterns
- Want better null-safety guarantees

**Choose Jackson client when:**
- Working with mixed Java/Kotlin codebase
- Need maximum compatibility with Java ecosystem
- Already using Jackson in your project
- Need mature, battle-tested serialization
- Working with complex JSON transformations

## Building the Client

To regenerate the client from the OpenAPI specification:

```bash
mvn clean generate-sources
```

To build and install locally:

```bash
mvn clean install
```

## API Documentation

The client is generated from the OpenAPI specification located at:
`../order-service/src/main/resources/openapi/order-service-api.yaml`

All models are annotated with `@Serializable` and include compile-time validation.

## Generated Structure

The client library includes:
- **API Classes**: `com.example.orderservice.client.kotlinx.api.*` - Type-safe API interfaces
- **Model Classes**: `com.example.orderservice.client.kotlinx.model.*` - Serializable data models
- **Infrastructure**: `com.example.orderservice.client.kotlinx.infrastructure.*` - HTTP client infrastructure

## Dependencies

This client requires:
- Kotlin 1.9.21+
- kotlinx.serialization 1.6.0+
- Ktor client 2.3.5+
- Java 17+

## Example: Complete Order Flow

```kotlin
import com.example.orderservice.client.kotlinx.api.OrderApi
import com.example.orderservice.client.kotlinx.model.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

fun processOrder() {
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }
    
    val orderApi = OrderApi("http://localhost:8080", httpClient)
    
    try {
        // 1. Place order
        val orderResponse = orderApi.placeOrder(
            PlaceOrderRequest(
                customerId = "customer-123",
                items = listOf(
                    OrderItem("prod-1", 2, 29.99),
                    OrderItem("prod-2", 1, 49.99)
                )
            )
        )
        println("✓ Order placed: ${orderResponse.orderId}")
        
        // 2. Process payment
        val paymentResponse = orderApi.processPayment(
            PaymentRequest(
                orderId = orderResponse.orderId,
                amount = 109.97,
                paymentMethod = "CREDIT_CARD"
            )
        )
        println("✓ Payment processed: ${paymentResponse.paymentId}")
        
        // 3. Ship order
        val shipmentResponse = orderApi.shipOrder(
            ShipOrderRequest(
                orderId = orderResponse.orderId,
                shippingAddress = Address(
                    street = "123 Main St",
                    city = "Springfield",
                    postalCode = "12345",
                    country = "USA"
                )
            )
        )
        println("✓ Order shipped: ${shipmentResponse.shipmentId}")
        
        // 4. Check final status
        val status = orderApi.getOrderStatus(orderResponse.orderId)
        println("✓ Final status - Placed: ${status.orderPlaced}, Paid: ${status.paymentProcessed}, Shipped: ${status.shipped}")
        
    } catch (e: Exception) {
        println("✗ Error: ${e.message}")
    } finally {
        httpClient.close()
    }
}
```

## Migration from Jackson Client

If migrating from the Jackson-based client:

```kotlin
// Jackson client (old)
val apiClient = ApiClient("http://localhost:8080")
val orderApi = OrderApi("http://localhost:8080", apiClient.client)

// Kotlinx client (new)
val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) { json() }
}
val orderApi = OrderApi("http://localhost:8080", httpClient)
```

The API surface remains the same, only the initialization differs.
