package com.example.orderservice.domain

import java.time.OffsetDateTime

/**
 * Immutable representation of an order's state in the system.
 * Each operation creates a new state rather than modifying existing state.
 */
data class OrderState(
    val orderId: String,
    val customerId: String,
    val items: List<OrderItemState>,
    val totalAmount: Double,
    val orderPlaced: Boolean = false,
    val orderPlacedAt: OffsetDateTime? = null,
    val paymentProcessed: Boolean = false,
    val paymentId: String? = null,
    val paymentProcessedAt: OffsetDateTime? = null,
    val shipped: Boolean = false,
    val shipmentId: String? = null,
    val shippedAt: OffsetDateTime? = null,
    val shippingAddress: AddressState? = null
)

data class OrderItemState(
    val productId: String,
    val quantity: Int,
    val price: Double
)

data class AddressState(
    val street: String,
    val city: String,
    val postalCode: String,
    val country: String
)
