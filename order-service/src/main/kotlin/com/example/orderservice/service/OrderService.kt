package com.example.orderservice.service

import com.example.orderservice.domain.AddressState
import com.example.orderservice.domain.OrderItemState
import com.example.orderservice.domain.OrderState
import com.example.orderservice.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.*

/**
 * Service layer for order processing operations.
 * All operations are immutable and create new state objects.
 * Includes rollback functionality for distributed transaction simulation.
 */
@Service
class OrderService(private val orderRepository: OrderRepository) {

    private val logger = LoggerFactory.getLogger(OrderService::class.java)

    /**
     * Place a new order in the system.
     * Creates an initial order state with orderPlaced flag set to true.
     */
    fun placeOrder(customerId: String, items: List<OrderItemState>): OrderState {
        val orderId = UUID.randomUUID().toString()
        val totalAmount = items.sumOf { it.price * it.quantity }

        logger.info("Placing order: orderId={}, customerId={}, totalAmount={}", orderId, customerId, totalAmount)

        val orderState = OrderState(
            orderId = orderId,
            customerId = customerId,
            items = items,
            totalAmount = totalAmount,
            orderPlaced = true,
            orderPlacedAt = OffsetDateTime.now()
        )

        return orderRepository.save(orderState)
    }

    /**
     * Rollback order placement.
     * Resets the orderPlaced flag to false.
     */
    fun rollbackPlaceOrder(orderId: String): OrderState {
        logger.info("Rolling back order placement: orderId={}", orderId)

        val currentState = orderRepository.findById(orderId)
            ?: throw OrderNotFoundException("Order not found: $orderId")

        val rolledBackState = currentState.copy(
            orderPlaced = false,
            orderPlacedAt = null
        )

        return orderRepository.save(rolledBackState)
    }

    /**
     * Process payment for an order.
     * Sets the paymentProcessed flag and generates a payment ID.
     */
    fun processPayment(orderId: String, amount: Double, paymentMethod: String): OrderState {
        logger.info("Processing payment: orderId={}, amount={}, paymentMethod={}", orderId, amount, paymentMethod)

        val currentState = orderRepository.findById(orderId)
            ?: throw OrderNotFoundException("Order not found: $orderId")

        if (!currentState.orderPlaced) {
            throw IllegalStateException("Cannot process payment for an order that hasn't been placed")
        }

        if (currentState.paymentProcessed) {
            throw IllegalStateException("Payment already processed for order: $orderId")
        }

        val paymentId = UUID.randomUUID().toString()

        val updatedState = currentState.copy(
            paymentProcessed = true,
            paymentId = paymentId,
            paymentProcessedAt = OffsetDateTime.now()
        )

        return orderRepository.save(updatedState)
    }

    /**
     * Rollback payment processing.
     * Resets the paymentProcessed flag and clears payment-related fields.
     */
    fun rollbackPayment(orderId: String): OrderState {
        logger.info("Rolling back payment: orderId={}", orderId)

        val currentState = orderRepository.findById(orderId)
            ?: throw OrderNotFoundException("Order not found: $orderId")

        val rolledBackState = currentState.copy(
            paymentProcessed = false,
            paymentId = null,
            paymentProcessedAt = null
        )

        return orderRepository.save(rolledBackState)
    }

    /**
     * Ship an order.
     * Sets the shipped flag and generates a shipment ID.
     */
    fun shipOrder(orderId: String, shippingAddress: AddressState, trackingNumber: String? = null): OrderState {
        logger.info("Shipping order: orderId={}, address={}", orderId, shippingAddress)

        val currentState = orderRepository.findById(orderId)
            ?: throw OrderNotFoundException("Order not found: $orderId")

        if (!currentState.orderPlaced) {
            throw IllegalStateException("Cannot ship an order that hasn't been placed")
        }

        if (!currentState.paymentProcessed) {
            throw IllegalStateException("Cannot ship an order without payment being processed")
        }

        if (currentState.shipped) {
            throw IllegalStateException("Order already shipped: $orderId")
        }

        val shipmentId = trackingNumber ?: UUID.randomUUID().toString()

        val updatedState = currentState.copy(
            shipped = true,
            shipmentId = shipmentId,
            shippedAt = OffsetDateTime.now(),
            shippingAddress = shippingAddress
        )

        return orderRepository.save(updatedState)
    }

    /**
     * Rollback order shipment.
     * Resets the shipped flag and clears shipment-related fields.
     */
    fun rollbackShipOrder(orderId: String): OrderState {
        logger.info("Rolling back shipment: orderId={}", orderId)

        val currentState = orderRepository.findById(orderId)
            ?: throw OrderNotFoundException("Order not found: $orderId")

        val rolledBackState = currentState.copy(
            shipped = false,
            shipmentId = null,
            shippedAt = null,
            shippingAddress = null
        )

        return orderRepository.save(rolledBackState)
    }

    /**
     * Get the current state of an order.
     */
    fun getOrderStatus(orderId: String): OrderState {
        return orderRepository.findById(orderId)
            ?: throw OrderNotFoundException("Order not found: $orderId")
    }
}

class OrderNotFoundException(message: String) : RuntimeException(message)
