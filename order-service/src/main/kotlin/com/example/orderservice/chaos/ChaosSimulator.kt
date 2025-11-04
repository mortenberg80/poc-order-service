package com.example.orderservice.chaos

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Core chaos simulator service.
 * Handles latency injection, failure simulation, and scenario management.
 */
@Service
class ChaosSimulator(
    private val chaosProperties: ChaosProperties
) {
    private val logger = LoggerFactory.getLogger(ChaosSimulator::class.java)
    
    // Track state per endpoint for consecutive failures/successes
    private val endpointStates = ConcurrentHashMap<String, EndpointChaosState>()
    
    // Active scenario (overrides config)
    @Volatile
    private var activeScenario: String? = null
    
    /**
     * Get current configuration, considering active scenario.
     */
    fun getCurrentConfiguration(): ChaosConfiguration {
        val baseConfig = chaosProperties.toChaosConfiguration()
        
        if (!baseConfig.enabled) {
            return baseConfig
        }
        
        // If scenario is active, merge it with base config
        activeScenario?.let { scenarioName ->
            val scenario = baseConfig.scenarios[scenarioName]
            if (scenario != null) {
                logger.debug("Using active scenario: {}", scenarioName)
                return baseConfig.copy(
                    endpoints = baseConfig.endpoints + scenario.endpoints
                )
            }
        }
        
        return baseConfig
    }
    
    /**
     * Activate a named scenario.
     */
    fun activateScenario(scenarioName: String) {
        val config = chaosProperties.toChaosConfiguration()
        if (config.scenarios.containsKey(scenarioName)) {
            activeScenario = scenarioName
            logger.info("Activated chaos scenario: {}", scenarioName)
        } else {
            throw IllegalArgumentException("Unknown scenario: $scenarioName")
        }
    }
    
    /**
     * Deactivate current scenario.
     */
    fun deactivateScenario() {
        val previous = activeScenario
        activeScenario = null
        if (previous != null) {
            logger.info("Deactivated chaos scenario: {}", previous)
        }
    }
    
    /**
     * Reset all endpoint states.
     */
    fun resetStates() {
        endpointStates.clear()
        logger.info("Reset all chaos endpoint states")
    }
    
    /**
     * Apply chaos to a request for a specific endpoint.
     * Returns null if no chaos should be applied, or a ChaosResult if chaos occurred.
     */
    fun applyChaos(endpointName: String): ChaosResult? {
        val config = getCurrentConfiguration()
        
        if (!config.enabled) {
            return null
        }
        
        val endpointChaos = config.endpoints[endpointName]
        if (endpointChaos == null || !endpointChaos.enabled) {
            return null
        }
        
        // Apply latency first
        val latencyMs = applyLatency(endpointChaos.latency)
        
        // Then check for failure
        val failure = applyFailure(endpointName, endpointChaos.failure)
        
        return if (latencyMs > 0 || failure != null) {
            ChaosResult(latencyMs, failure)
        } else {
            null
        }
    }
    
    private fun applyLatency(latencyConfig: LatencyConfig?): Long {
        if (latencyConfig == null || !latencyConfig.enabled) {
            return 0
        }
        
        // Check probability
        if (Random.nextDouble() > latencyConfig.probability) {
            return 0
        }
        
        val delayMs = if (latencyConfig.fixedDelayMs != null) {
            latencyConfig.fixedDelayMs
        } else {
            if (latencyConfig.maxDelayMs > latencyConfig.minDelayMs) {
                Random.nextLong(latencyConfig.minDelayMs, latencyConfig.maxDelayMs)
            } else {
                latencyConfig.minDelayMs
            }
        }
        
        if (delayMs > 0) {
            logger.debug("Injecting latency: {}ms", delayMs)
            Thread.sleep(delayMs)
        }
        
        return delayMs
    }
    
    private fun applyFailure(endpointName: String, failureConfig: FailureConfig?): ChaosFailure? {
        if (failureConfig == null || !failureConfig.enabled || failureConfig.failureRate <= 0.0) {
            return null
        }
        
        val state = endpointStates.getOrPut(endpointName) { EndpointChaosState() }
        
        // Handle consecutive failures pattern
        if (failureConfig.consecutiveFailures > 0) {
            return handleConsecutivePattern(endpointName, failureConfig, state)
        }
        
        // Random failure based on probability
        if (Random.nextDouble() < failureConfig.failureRate) {
            updateState(endpointName, state.copy(
                failureCount = state.failureCount + 1,
                lastFailureTime = System.currentTimeMillis()
            ))
            
            return createChaosFailure(failureConfig)
        }
        
        return null
    }
    
    private fun handleConsecutivePattern(
        endpointName: String,
        failureConfig: FailureConfig,
        state: EndpointChaosState
    ): ChaosFailure? {
        // If we're in the failure phase
        if (state.failureCount < failureConfig.consecutiveFailures) {
            updateState(endpointName, state.copy(
                failureCount = state.failureCount + 1,
                successCount = 0,
                lastFailureTime = System.currentTimeMillis()
            ))
            logger.debug("Consecutive failure {}/{} for endpoint: {}",
                state.failureCount + 1, failureConfig.consecutiveFailures, endpointName)
            return createChaosFailure(failureConfig)
        }
        
        // If we're in the success phase (or no success pattern configured)
        if (failureConfig.consecutiveSuccesses > 0) {
            if (state.successCount < failureConfig.consecutiveSuccesses) {
                updateState(endpointName, state.copy(
                    successCount = state.successCount + 1
                ))
                logger.debug("Consecutive success {}/{} for endpoint: {}",
                    state.successCount + 1, failureConfig.consecutiveSuccesses, endpointName)
                return null
            }
            
            // Reset the cycle
            updateState(endpointName, EndpointChaosState())
            logger.debug("Resetting consecutive pattern for endpoint: {}", endpointName)
            return createChaosFailure(failureConfig)
        }
        
        // No success pattern, just reset after failures
        updateState(endpointName, EndpointChaosState())
        return null
    }
    
    private fun createChaosFailure(failureConfig: FailureConfig): ChaosFailure {
        val errorType = if (failureConfig.errorType == ChaosErrorType.RANDOM) {
            ChaosErrorType.values()
                .filter { it != ChaosErrorType.RANDOM }
                .random()
        } else {
            failureConfig.errorType
        }
        
        val status = failureConfig.httpStatus ?: when (errorType) {
            ChaosErrorType.TIMEOUT -> 408
            ChaosErrorType.SERVER_ERROR -> 500
            ChaosErrorType.SERVICE_UNAVAILABLE -> 503
            ChaosErrorType.BAD_REQUEST -> 400
            ChaosErrorType.CONFLICT -> 409
            ChaosErrorType.RATE_LIMIT -> 429
            ChaosErrorType.NETWORK_ERROR -> 503
            ChaosErrorType.RANDOM -> 500
        }
        
        val message = failureConfig.errorMessage ?: "Chaos-injected failure: $errorType"
        
        logger.info("Injecting chaos failure: type={}, status={}, message={}", errorType, status, message)
        
        return ChaosFailure(errorType, status, message)
    }
    
    private fun updateState(endpointName: String, newState: EndpointChaosState) {
        endpointStates[endpointName] = newState
    }
    
    /**
     * Get statistics for monitoring.
     */
    fun getStatistics(): ChaosStatistics {
        return ChaosStatistics(
            chaosEnabled = chaosProperties.enabled,
            activeScenario = activeScenario,
            endpointStates = endpointStates.toMap()
        )
    }
}

data class ChaosResult(
    val latencyMs: Long,
    val failure: ChaosFailure?
)

data class ChaosFailure(
    val errorType: ChaosErrorType,
    val httpStatus: Int,
    val message: String
)

data class ChaosStatistics(
    val chaosEnabled: Boolean,
    val activeScenario: String?,
    val endpointStates: Map<String, EndpointChaosState>
)
