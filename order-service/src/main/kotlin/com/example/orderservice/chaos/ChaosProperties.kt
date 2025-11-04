package com.example.orderservice.chaos

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Spring Boot configuration properties for chaos engineering.
 * Binds to 'chaos' prefix in application.yaml
 */
@Configuration
@ConfigurationProperties(prefix = "chaos")
class ChaosProperties {
    var enabled: Boolean = false
    var endpoints: Map<String, EndpointChaosProperties> = mutableMapOf()
    var scenarios: Map<String, ScenarioProperties> = mutableMapOf()
    var globalDefaults: GlobalDefaultsProperties = GlobalDefaultsProperties()
    
    fun toChaosConfiguration(): ChaosConfiguration {
        return ChaosConfiguration(
            enabled = enabled,
            endpoints = endpoints.mapValues { it.value.toEndpointChaos() },
            scenarios = scenarios.mapValues { it.value.toScenarioConfig() },
            globalDefaults = globalDefaults.toGlobalChaosDefaults()
        )
    }
}

class EndpointChaosProperties {
    var enabled: Boolean = true
    var latency: LatencyProperties? = null
    var failure: FailureProperties? = null
    
    fun toEndpointChaos(): EndpointChaos {
        return EndpointChaos(
            enabled = enabled,
            latency = latency?.toLatencyConfig(),
            failure = failure?.toFailureConfig()
        )
    }
}

class LatencyProperties {
    var enabled: Boolean = true
    var minDelayMs: Long = 0
    var maxDelayMs: Long = 0
    var fixedDelayMs: Long? = null
    var probability: Double = 1.0
    
    fun toLatencyConfig(): LatencyConfig {
        return LatencyConfig(
            enabled = enabled,
            minDelayMs = minDelayMs,
            maxDelayMs = maxDelayMs,
            fixedDelayMs = fixedDelayMs,
            probability = probability
        )
    }
}

class FailureProperties {
    var enabled: Boolean = true
    var failureRate: Double = 0.0
    var errorType: String = "SERVER_ERROR"
    var httpStatus: Int? = null
    var errorMessage: String? = null
    var consecutiveFailures: Int = 0
    var consecutiveSuccesses: Int = 0
    
    fun toFailureConfig(): FailureConfig {
        return FailureConfig(
            enabled = enabled,
            failureRate = failureRate,
            errorType = ChaosErrorType.valueOf(errorType),
            httpStatus = httpStatus,
            errorMessage = errorMessage,
            consecutiveFailures = consecutiveFailures,
            consecutiveSuccesses = consecutiveSuccesses
        )
    }
}

class ScenarioProperties {
    var description: String = ""
    var endpoints: Map<String, EndpointChaosProperties> = mutableMapOf()
    
    fun toScenarioConfig(): ScenarioConfig {
        return ScenarioConfig(
            description = description,
            endpoints = endpoints.mapValues { it.value.toEndpointChaos() }
        )
    }
}

class GlobalDefaultsProperties {
    var defaultLatencyMs: Long = 0
    var defaultFailureRate: Double = 0.0
    var defaultErrorType: String = "SERVER_ERROR"
    
    fun toGlobalChaosDefaults(): GlobalChaosDefaults {
        return GlobalChaosDefaults(
            defaultLatencyMs = defaultLatencyMs,
            defaultFailureRate = defaultFailureRate,
            defaultErrorType = ChaosErrorType.valueOf(defaultErrorType)
        )
    }
}
