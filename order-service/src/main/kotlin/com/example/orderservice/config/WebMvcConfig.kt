package com.example.orderservice.config

import com.example.orderservice.chaos.ChaosInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Web MVC configuration to register the chaos interceptor.
 */
@Configuration
class WebMvcConfig(
    private val chaosInterceptor: ChaosInterceptor
) : WebMvcConfigurer {
    
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(chaosInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/actuator/**")
    }
}
