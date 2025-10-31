# Order Service Client

Java/Kotlin client library for the Order Service API. This library provides a type-safe client for interacting with the Order Service.

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>order-service-client</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## HTTP Client Library

This client uses **OkHttp4** as the underlying HTTP client library, chosen for:
- Industry-standard, widely adopted HTTP client
- Excellent performance with connection pooling
- Built-in support for HTTP/2
- Automatic retry and failover capabilities
- Thread-safe and efficient
- Well-tested and maintained by Square

## Usage

### Kotlin Example

```kotlin
import com.example.orderservice.client.api.OrderApi
import com.example.orderservice.client.infrastructure.ApiClient
import com.example.orderservice.client.model.*

fun main() {
    // Create API client
    val apiClient = ApiClient(basePath = "http://localhost:8080")
    val orderApi = OrderApi(basePath = "http://localhost:8080", client = apiClient.client)
    
    // Place an order
    val placeOrderRequest = PlaceOrderRequest(
        customerId = "customer-123",
        items = listOf(
            OrderItem(
                productId = "prod-1",
                quantity = 2,
                price = 29.99.toBigDecimal()
            )
        )
    )
    
    val orderResponse = orderApi.placeOrder(placeOrderRequest)
    println("Order placed: ${orderResponse.orderId}")
    
    // Process payment
    val paymentRequest = PaymentRequest(
        orderId = orderResponse.orderId,
        amount = 59.98.toBigDecimal(),
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
}
```

### Java Example

```java
import com.example.orderservice.client.api.OrderApi;
import com.example.orderservice.client.infrastructure.ApiClient;
import com.example.orderservice.client.model.*;

import java.math.BigDecimal;
import java.util.List;

public class OrderServiceExample {
    public static void main(String[] args) {
        // Create API client
        ApiClient apiClient = new ApiClient("http://localhost:8080");
        OrderApi orderApi = new OrderApi("http://localhost:8080", apiClient.getClient());
        
        try {
            // Place an order
            PlaceOrderRequest placeOrderRequest = new PlaceOrderRequest(
                "customer-123",
                List.of(
                    new OrderItem("prod-1", 2, new BigDecimal("29.99"))
                )
            );
            
            OrderResponse orderResponse = orderApi.placeOrder(placeOrderRequest);
            System.out.println("Order placed: " + orderResponse.getOrderId());
            
            // Process payment
            PaymentRequest paymentRequest = new PaymentRequest(
                orderResponse.getOrderId(),
                new BigDecimal("59.98"),
                "CREDIT_CARD"
            );
            
            PaymentResponse paymentResponse = orderApi.processPayment(paymentRequest);
            System.out.println("Payment processed: " + paymentResponse.getPaymentId());
            
            // Ship order
            Address address = new Address(
                "123 Main St",
                "Springfield",
                "12345",
                "USA"
            );
            ShipOrderRequest shipOrderRequest = new ShipOrderRequest(
                orderResponse.getOrderId(),
                address
            );
            
            ShipOrderResponse shipOrderResponse = orderApi.shipOrder(shipOrderRequest);
            System.out.println("Order shipped: " + shipOrderResponse.getShipmentId());
            
            // Get order status
            OrderStatusResponse orderStatus = orderApi.getOrderStatus(orderResponse.getOrderId());
            System.out.println("Order status: placed=" + orderStatus.getOrderPlaced() + 
                             ", paid=" + orderStatus.getPaymentProcessed() + 
                             ", shipped=" + orderStatus.getShipped());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Rollback Operations

The client also supports rollback operations for distributed transaction scenarios:

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

## Configuration

### Custom HTTP Client

The generated client uses OkHttp4. You can customize the HTTP client:

```kotlin
import okhttp3.OkHttpClient
import java.time.Duration
import java.util.concurrent.TimeUnit

val httpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

val orderApi = OrderApi("http://localhost:8080", httpClient)
```

### Error Handling

```kotlin
import com.example.orderservice.client.infrastructure.ClientException
import com.example.orderservice.client.infrastructure.ServerException

try {
    orderApi.placeOrder(placeOrderRequest)
} catch (e: ClientException) {
    // HTTP 4xx errors
    println("Client error: ${e.statusCode} - ${e.message}")
} catch (e: ServerException) {
    // HTTP 5xx errors
    println("Server error: ${e.statusCode} - ${e.message}")
}
```

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

All models and API methods are type-safe and include validation.

## Generated Structure

The client library includes:
- **API Classes**: `com.example.orderservice.client.api.*` - Type-safe API interfaces
- **Model Classes**: `com.example.orderservice.client.model.*` - Request/Response data models
- **Infrastructure**: `com.example.orderservice.client.infrastructure.*` - HTTP client infrastructure

## Library Choice Considerations

When creating this client, we evaluated several HTTP client options:

1. **OkHttp4** (Selected) ✓
   - Industry standard, widely adopted
   - Excellent performance with connection pooling
   - Works with Java 8+ (though this project uses Java 17)
   - Mature, well-maintained library
   - Good choice for production use

2. **Java 11+ HttpClient**
   - Native to Java 11+, no external dependencies
   - Modern async/sync API
   - Lightweight
   - Best for minimizing dependencies

3. **Apache HttpClient 5**
   - Very mature and feature-rich
   - Complex API, steeper learning curve
   - Heavier dependency footprint

4. **Spring WebClient/RestClient**
   - Excellent if consuming service already uses Spring
   - Adds Spring dependency overhead
   - Not suitable for non-Spring projects

We chose OkHttp4 for its excellent balance of performance, maturity, and ease of use while being suitable for any Java/Kotlin project.

