package com.example.orderservice.chaos

/**
 * Configuration for chaos engineering features.
 * Allows simulation of failures, latency, and various error conditions.
 */
data class ChaosConfiguration(
    val enabled: Boolean = false,
    val endpoints: Map<String, EndpointChaos> = emptyMap(),
    val scenarios: Map<String, ScenarioConfig> = emptyMap(),
    val globalDefaults: GlobalChaosDefaults = GlobalChaosDefaults()
)

data class EndpointChaos(
    val enabled: Boolean = true,
    val latency: LatencyConfig? = null,
    val failure: FailureConfig? = null
)

data class LatencyConfig(
    val enabled: Boolean = true,
    val minDelayMs: Long = 0,
    val maxDelayMs: Long = 0,
    val fixedDelayMs: Long? = null,
    val probability: Double = 1.0  // 1.0 = 100%, 0.5 = 50%
)

data class FailureConfig(
    val enabled: Boolean = true,
    val failureRate: Double = 0.0,  // 0.0 to 1.0
    val errorType: ChaosErrorType = ChaosErrorType.SERVER_ERROR,
    val httpStatus: Int? = null,
    val errorMessage: String? = null,
    val consecutiveFailures: Int = 0,  // 0 = random, >0 = fail N times then succeed
    val consecutiveSuccesses: Int = 0   // After consecutive failures, succeed N times
)

data class ScenarioConfig(
    val description: String,
    val endpoints: Map<String, EndpointChaos> = emptyMap()
)

data class GlobalChaosDefaults(
    val defaultLatencyMs: Long = 0,
    val defaultFailureRate: Double = 0.0,
    val defaultErrorType: ChaosErrorType = ChaosErrorType.SERVER_ERROR
)

enum class ChaosErrorType {
    TIMEOUT,
    SERVER_ERROR,
    SERVICE_UNAVAILABLE,
    BAD_REQUEST,
    CONFLICT,
    RATE_LIMIT,
    NETWORK_ERROR,
    RANDOM
}

/**
 * Represents the current state of chaos for an endpoint.
 */
data class EndpointChaosState(
    val failureCount: Int = 0,
    val successCount: Int = 0,
    val lastFailureTime: Long = 0
)
