package com.example.orderservice.chaos

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Logs chaos engineering configuration on application startup.
 * Provides clear visibility into what chaos mode is active and its impact.
 */
@Component
class ChaosStartupLogger(
    private val chaosProperties: ChaosProperties
) {
    private val logger = LoggerFactory.getLogger(ChaosStartupLogger::class.java)
    
    @EventListener(ApplicationReadyEvent::class)
    fun logChaosConfiguration() {
        val config = chaosProperties.toChaosConfiguration()
        
        if (!config.enabled) {
            logger.info("""
                
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
            """.trimIndent())
            return
        }
        
        // Chaos is enabled - provide detailed logging
        logger.warn("""
            
            ╔════════════════════════════════════════════════════════════════╗
            ║          ⚡ CHAOS ENGINEERING: ENABLED ⚡                       ║
            ╠════════════════════════════════════════════════════════════════╣
            ║  WARNING: Service running with CHAOS INJECTION                 ║
            ║  Endpoints may experience failures and/or latency              ║
            ╚════════════════════════════════════════════════════════════════╝
        """.trimIndent())
        
        // Log scenario information if scenarios are configured
        if (config.scenarios.isNotEmpty()) {
            logger.info("Available Scenarios: ${config.scenarios.size}")
            config.scenarios.forEach { (name, scenario) ->
                logger.info("  • $name: ${scenario.description}")
            }
            logger.info("Activate scenarios via: POST /actuator/chaos/scenario/{name}")
            logger.info("")
        }
        
        // Log endpoint-specific configuration
        if (config.endpoints.isNotEmpty()) {
            logger.warn("Chaos Configuration by Endpoint:")
            logger.warn("=" .repeat(80))
            
            config.endpoints.forEach { (endpointName, endpointChaos) ->
                logEndpointChaos(endpointName, endpointChaos)
            }
            
            logger.warn("=" .repeat(80))
        } else {
            logger.info("No per-endpoint chaos configured - using scenarios only")
        }
        
        // Log management endpoints
        logger.info("""
            
            Chaos Management Endpoints:
              • GET  /actuator/chaos/config      - View current configuration
              • GET  /actuator/chaos/scenarios   - List available scenarios
              • POST /actuator/chaos/scenario/{name} - Activate scenario
              • POST /actuator/chaos/reset       - Reset chaos state
              • GET  /actuator/chaos/statistics  - View statistics
        """.trimIndent())
    }
    
    private fun logEndpointChaos(endpointName: String, chaos: EndpointChaos) {
        if (!chaos.enabled) {
            logger.info("[$endpointName] Chaos DISABLED for this endpoint")
            return
        }
        
        val features = mutableListOf<String>()
        
        // Check latency configuration
        chaos.latency?.let { latency ->
            if (latency.enabled) {
                val latencyDesc = when {
                    latency.fixedDelayMs != null -> {
                        "Fixed ${latency.fixedDelayMs}ms delay"
                    }
                    latency.maxDelayMs > latency.minDelayMs -> {
                        "${latency.minDelayMs}-${latency.maxDelayMs}ms random delay"
                    }
                    else -> {
                        "${latency.minDelayMs}ms delay"
                    }
                }
                val probability = (latency.probability * 100).toInt()
                features.add("LATENCY: $latencyDesc (${probability}% of requests)")
            }
        }
        
        // Check failure configuration
        chaos.failure?.let { failure ->
            if (failure.enabled && failure.failureRate > 0) {
                val failurePercent = (failure.failureRate * 100).toInt()
                
                val failureDesc = when {
                    failure.consecutiveFailures > 0 -> {
                        val pattern = "fail ${failure.consecutiveFailures}x"
                        if (failure.consecutiveSuccesses > 0) {
                            "$pattern, succeed ${failure.consecutiveSuccesses}x, repeat"
                        } else {
                            "$pattern, then succeed"
                        }
                    }
                    else -> {
                        "$failurePercent% failure rate"
                    }
                }
                
                val errorType = failure.errorType.name
                val status = failure.httpStatus ?: getDefaultStatusForError(failure.errorType)
                
                features.add("FAILURE: $failureDesc - $errorType (HTTP $status)")
                
                failure.errorMessage?.let { msg ->
                    features.add("  └─ Message: \"$msg\"")
                }
            }
        }
        
        if (features.isEmpty()) {
            logger.info("[$endpointName] Chaos enabled but no features configured")
        } else {
            logger.warn("[$endpointName] Chaos ACTIVE:")
            features.forEach { feature ->
                logger.warn("  $feature")
            }
        }
    }
    
    private fun getDefaultStatusForError(errorType: ChaosErrorType): Int {
        return when (errorType) {
            ChaosErrorType.TIMEOUT -> 408
            ChaosErrorType.SERVER_ERROR -> 500
            ChaosErrorType.SERVICE_UNAVAILABLE -> 503
            ChaosErrorType.BAD_REQUEST -> 400
            ChaosErrorType.CONFLICT -> 409
            ChaosErrorType.RATE_LIMIT -> 429
            ChaosErrorType.NETWORK_ERROR -> 503
            ChaosErrorType.RANDOM -> 500
        }
    }
}
