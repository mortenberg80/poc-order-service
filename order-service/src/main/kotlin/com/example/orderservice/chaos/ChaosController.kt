package com.example.orderservice.chaos

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for managing chaos configuration at runtime.
 * Allows activating scenarios, resetting state, and viewing statistics.
 */
@RestController
@RequestMapping("/actuator/chaos")
class ChaosController(
    private val chaosSimulator: ChaosSimulator,
    private val chaosProperties: ChaosProperties
) {
    
    /**
     * Get current chaos configuration.
     */
    @GetMapping("/config")
    fun getConfig(): ResponseEntity<ChaosConfiguration> {
        return ResponseEntity.ok(chaosSimulator.getCurrentConfiguration())
    }
    
    /**
     * List available scenarios.
     */
    @GetMapping("/scenarios")
    fun getScenarios(): ResponseEntity<Map<String, String>> {
        val config = chaosProperties.toChaosConfiguration()
        val scenarios = config.scenarios.mapValues { it.value.description }
        return ResponseEntity.ok(scenarios)
    }
    
    /**
     * Activate a specific scenario.
     */
    @PostMapping("/scenario/{scenarioName}")
    fun activateScenario(@PathVariable scenarioName: String): ResponseEntity<Map<String, String>> {
        return try {
            chaosSimulator.activateScenario(scenarioName)
            ResponseEntity.ok(mapOf(
                "status" to "activated",
                "scenario" to scenarioName
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to e.message.orEmpty()
            ))
        }
    }
    
    /**
     * Deactivate current scenario.
     */
    @PostMapping("/scenario/deactivate")
    fun deactivateScenario(): ResponseEntity<Map<String, String>> {
        chaosSimulator.deactivateScenario()
        return ResponseEntity.ok(mapOf("status" to "deactivated"))
    }
    
    /**
     * Reset all chaos states (failure counters, etc.)
     */
    @PostMapping("/reset")
    fun reset(): ResponseEntity<Map<String, String>> {
        chaosSimulator.resetStates()
        chaosSimulator.deactivateScenario()
        return ResponseEntity.ok(mapOf("status" to "reset"))
    }
    
    /**
     * Get chaos statistics.
     */
    @GetMapping("/statistics")
    fun getStatistics(): ResponseEntity<ChaosStatistics> {
        return ResponseEntity.ok(chaosSimulator.getStatistics())
    }
    
    /**
     * Health check for chaos system.
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        val config = chaosSimulator.getCurrentConfiguration()
        return ResponseEntity.ok(mapOf(
            "enabled" to config.enabled,
            "endpointsConfigured" to config.endpoints.size,
            "scenariosAvailable" to config.scenarios.size
        ))
    }
}
