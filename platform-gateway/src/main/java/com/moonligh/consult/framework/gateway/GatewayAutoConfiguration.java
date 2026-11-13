package com.moonligh.consult.framework.gateway;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;

import com.moonligh.consult.framework.observability.AgentTrace;
import com.moonligh.consult.security.AuthTokens;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * 网关横切过滤器。业务仓只依赖本模块，自行配置路由与 moonligh.gateway 保护段。
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
@EnableConfigurationProperties(GatewayProtectionProperties.class)
public class GatewayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GatewayTraceFilter gatewayTraceFilter(ObjectProvider<AgentTrace> trace) {
        return new GatewayTraceFilter(trace.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public InFlightLimitGatewayFilter inFlightLimitGatewayFilter(
            GatewayProtectionProperties properties, MeterRegistry meters) {
        return new InFlightLimitGatewayFilter(properties, meters);
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenGatewayFilter tokenGatewayFilter(AuthTokens tokens, GatewayProtectionProperties properties) {
        return new TokenGatewayFilter(tokens, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitGatewayFilter rateLimitGatewayFilter(
            GatewayProtectionProperties properties,
            ObjectProvider<RedisRateLimiter> redis,
            MeterRegistry meters) {
        return new RateLimitGatewayFilter(properties, redis, meters);
    }
}
