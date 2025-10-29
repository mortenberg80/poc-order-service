package com.example.orderservice.repository

import com.example.orderservice.domain.OrderState
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory repository for storing order states.
 * Uses ConcurrentHashMap for thread-safe operations.
 * All operations are immutable - they create new state rather than modifying existing state.
 */
@Repository
class OrderRepository {

    private val orders = ConcurrentHashMap<String, OrderState>()

    /**
     * Save or update an order state.
     * This replaces the entire state with a new immutable state.
     */
    fun save(orderState: OrderState): OrderState {
        orders[orderState.orderId] = orderState
        return orderState
    }

    /**
     * Find an order by its ID.
     */
    fun findById(orderId: String): OrderState? {
        return orders[orderId]
    }

    /**
     * Check if an order exists.
     */
    fun exists(orderId: String): Boolean {
        return orders.containsKey(orderId)
    }

    /**
     * Delete an order (used for complete rollback).
     */
    fun deleteById(orderId: String) {
        orders.remove(orderId)
    }

    /**
     * Get all orders (for testing/debugging purposes).
     */
    fun findAll(): List<OrderState> {
        return orders.values.toList()
    }

    /**
     * Clear all orders (for testing purposes).
     */
    fun clear() {
        orders.clear()
    }
}
