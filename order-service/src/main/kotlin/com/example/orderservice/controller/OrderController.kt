package com.example.orderservice.controller

import com.example.orderservice.api.OrderApi
import com.example.orderservice.domain.AddressState
import com.example.orderservice.domain.OrderItemState
import com.example.orderservice.model.*
import com.example.orderservice.service.OrderNotFoundException
import com.example.orderservice.service.OrderService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

/**
 * Implementation of the OpenAPI-generated OrderApiDelegate interface.
 * This connects the API layer to the service layer.
 */
@RestController
class OrderController(
    private val orderService: OrderService
) : OrderApi {

    private val logger = LoggerFactory.getLogger(OrderController::class.java)

    override fun placeOrder(placeOrderRequest: PlaceOrderRequest): ResponseEntity<OrderResponse> {
        return try {
            val items = placeOrderRequest.items.map { item ->
                OrderItemState(
                    productId = item.productId,
                    quantity = item.quantity,
                    price = item.price
                )
            }

            val orderState = orderService.placeOrder(
                customerId = placeOrderRequest.customerId,
                items = items
            )

            val response = OrderResponse(
                orderId = orderState.orderId,
                status = "ORDER_PLACED",
                timestamp = orderState.orderPlacedAt ?: OffsetDateTime.now()
            )

            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error placing order", e)
            ResponseEntity.badRequest().body(
                OrderResponse(
                    orderId = "",
                    status = "ERROR: ${e.message}",
                    timestamp = OffsetDateTime.now()
                )
            )
        }
    }

    override fun rollbackPlaceOrder(rollbackRequest: RollbackRequest): ResponseEntity<RollbackResponse> {
        return try {
            val orderState = orderService.rollbackPlaceOrder(rollbackRequest.orderId)

            val response = RollbackResponse(
                orderId = orderState.orderId,
                status = "ORDER_PLACEMENT_ROLLED_BACK",
                timestamp = OffsetDateTime.now()
            )

            ResponseEntity.ok(response)
        } catch (e: OrderNotFoundException) {
            logger.error("Order not found for rollback", e)
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                RollbackResponse(
                    orderId = rollbackRequest.orderId,
                    status = "ERROR: ${e.message}",
                    timestamp = OffsetDateTime.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Error rolling back order placement", e)
            ResponseEntity.badRequest().body(
                RollbackResponse(
                    orderId = rollbackRequest.orderId,
                    status = "ERROR: ${e.message}",
                    timestamp = OffsetDateTime.now()
                )
            )
        }
    }

    override fun processPayment(paymentRequest: PaymentRequest): ResponseEntity<PaymentResponse> {
        return try {
            val orderState = orderService.processPayment(
                orderId = paymentRequest.orderId,
                amount = paymentRequest.amount,
                paymentMethod = paymentRequest.paymentMethod
            )

            val response = PaymentResponse(
                orderId = orderState.orderId,
                paymentId = orderState.paymentId ?: "",
                status = "PAYMENT_PROCESSED",
                timestamp = orderState.paymentProcessedAt ?: OffsetDateTime.now()
            )

            ResponseEntity.ok(response)
        } catch (e: OrderNotFoundException) {
            logger.error("Order not found for payment", e)
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                PaymentResponse(
                    orderId = paymentRequest.orderId,
                    paymentId = "",
                    status = "ERROR: ${e.message}",
                    timestamp = OffsetDateTime.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Error processing payment", e)
            ResponseEntity.badRequest().body(
                PaymentResponse(
                    orderId = paymentRequest.orderId,
                    paymentId = "",
                    status = "ERROR: ${e.message}",
                    timestamp = OffsetDateTime.now()
                )
            )
        }
    }

    override fun rollbackPayment(rollbackRequest: RollbackRequest): ResponseEntity<RollbackResponse> {
        return try {
            val orderState = orderService.rollbackPayment(rollbackRequest.orderId)

            val response = RollbackResponse(
                orderId = orderState.orderId,
                status = "PAYMENT_ROLLED_BACK",
                timestamp = OffsetDateTime.now()
            )

            ResponseEntity.ok(response)
        } catch (e: OrderNotFoundException) {
            logger.error("Order not found for payment rollback", e)
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                RollbackResponse(
                    orderId = rollbackRequest.orderId,
                    status = "ERROR: ${e.message}",
                    timestamp = OffsetDateTime.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Error rolling back payment", e)
            ResponseEntity.badRequest().body(
                RollbackResponse(
                    orderId = rollbackRequest.orderId,
                    status = "ERROR: ${e.message}",
                    timestamp = OffsetDateTime.now()
                )
            )
        }
    }

    override fun shipOrder(shipOrderRequest: ShipOrderRequest): ResponseEntity<ShipOrderResponse> {
        return try {
            val address = AddressState(
                street = shipOrderRequest.shippingAddress.street,
                city = shipOrderRequest.shippingAddress.city,
                postalCode = shipOrderRequest.shippingAddress.postalCode,
                country = shipOrderRequest.shippingAddress.country
            )

            val orderState = orderService.shipOrder(
                orderId = shipOrderRequest.orderId,
                shippingAddress = address,
                trackingNumber = shipOrderRequest.trackingNumber
            )

            val response = ShipOrderResponse(
                orderId = orderState.orderId,
                shipmentId = orderState.shipmentId ?: "",
                status = "ORDER_SHIPPED",
                timestamp = orderState.shippedAt ?: OffsetDateTime.now()
            )

            ResponseEntity.ok(response)
        } catch (e: OrderNotFoundException) {
            logger.error("Order not found for shipment", e)
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ShipOrderResponse(
                    orderId = shipOrderRequest.orderId,
                    shipmentId = "",
                    status = "ERROR: ${e.message}",
                    timestamp = OffsetDateTime.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Error shipping order", e)
            ResponseEntity.badRequest().body(
                ShipOrderResponse(
                    orderId = shipOrderRequest.orderId,
                    shipmentId = "",
                    status = "ERROR: ${e.message}",
                    timestamp = OffsetDateTime.now()
                )
            )
        }
    }

    override fun rollbackShipOrder(rollbackRequest: RollbackRequest): ResponseEntity<RollbackResponse> {
        return try {
            val orderState = orderService.rollbackShipOrder(rollbackRequest.orderId)

            val response = RollbackResponse(
                orderId = orderState.orderId,
                status = "SHIPMENT_ROLLED_BACK",
                timestamp = OffsetDateTime.now()
            )

            ResponseEntity.ok(response)
        } catch (e: OrderNotFoundException) {
            logger.error("Order not found for shipment rollback", e)
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                RollbackResponse(
                    orderId = rollbackRequest.orderId,
                    status = "ERROR: ${e.message}",
                    timestamp = OffsetDateTime.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Error rolling back shipment", e)
            ResponseEntity.badRequest().body(
                RollbackResponse(
                    orderId = rollbackRequest.orderId,
                    status = "ERROR: ${e.message}",
                    timestamp = OffsetDateTime.now()
                )
            )
        }
    }

    override fun getOrderStatus(orderId: String): ResponseEntity<OrderStatusResponse> {
        return try {
            val orderState = orderService.getOrderStatus(orderId)

            val response = OrderStatusResponse(
                orderId = orderState.orderId,
                orderPlaced = orderState.orderPlaced,
                paymentProcessed = orderState.paymentProcessed,
                shipped = orderState.shipped,
                customerId = orderState.customerId,
                totalAmount = orderState.totalAmount
            )

            ResponseEntity.ok(response)
        } catch (e: OrderNotFoundException) {
            logger.error("Order not found", e)
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        } catch (e: Exception) {
            logger.error("Error retrieving order status", e)
            ResponseEntity.internalServerError().build()
        }
    }
}
