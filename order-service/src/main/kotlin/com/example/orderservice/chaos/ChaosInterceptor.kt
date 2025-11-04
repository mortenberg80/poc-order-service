package com.example.orderservice.chaos

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/**
 * Spring MVC interceptor that applies chaos engineering to requests.
 * Intercepts requests before they reach the controller and injects failures/latency.
 */
@Component
class ChaosInterceptor(
    private val chaosSimulator: ChaosSimulator
) : HandlerInterceptor {
    
    private val logger = LoggerFactory.getLogger(ChaosInterceptor::class.java)
    
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        // Skip chaos for actuator endpoints
        if (request.requestURI.startsWith("/actuator")) {
            return true
        }
        
        // Check for scenario override in header
        val scenarioHeader = request.getHeader("X-Chaos-Scenario")
        if (scenarioHeader != null) {
            try {
                chaosSimulator.activateScenario(scenarioHeader)
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid scenario in header: {}", scenarioHeader)
            }
        }
        
        // Determine endpoint name from request URI
        val endpointName = extractEndpointName(request.requestURI)
        
        // Apply chaos
        val chaosResult = chaosSimulator.applyChaos(endpointName)
        
        if (chaosResult?.failure != null) {
            val failure = chaosResult.failure
            response.status = failure.httpStatus
            response.contentType = "application/json"
            response.writer.write("""
                {
                    "error": "${failure.errorType}",
                    "message": "${failure.message}",
                    "timestamp": "${java.time.OffsetDateTime.now()}"
                }
            """.trimIndent())
            return false  // Stop request processing
        }
        
        return true  // Continue to controller
    }
    
    /**
     * Extract a simplified endpoint name from the URI for chaos configuration mapping.
     * Examples:
     *   /api/orders/place -> place-order
     *   /api/orders/payment -> process-payment
     *   /api/orders/ship -> ship-order
     */
    private fun extractEndpointName(uri: String): String {
        return when {
            uri.contains("/place") && !uri.contains("/rollback") -> "place-order"
            uri.contains("/place/rollback") -> "rollback-place-order"
            uri.contains("/payment") && !uri.contains("/rollback") -> "process-payment"
            uri.contains("/payment/rollback") -> "rollback-payment"
            uri.contains("/ship") && !uri.contains("/rollback") -> "ship-order"
            uri.contains("/ship/rollback") -> "rollback-ship-order"
            uri.matches(Regex(".*/orders/[^/]+$")) -> "get-order-status"
            else -> "unknown"
        }
    }
}
