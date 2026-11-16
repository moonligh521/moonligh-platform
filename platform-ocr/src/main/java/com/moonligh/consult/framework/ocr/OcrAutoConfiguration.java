package com.moonligh.consult.framework.ocr;

import java.util.Optional;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** 没配密钥时仍提供客户端，识图直接返回空，不拖垮服务。 */
@AutoConfiguration
@EnableConfigurationProperties(OcrProperties.class)
public class OcrAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OcrClient ocrClient(OcrProperties properties) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            return (image, mimeType, prompt) -> Optional.empty();
        }
        return new DashScopeOcrClient(properties);
    }
}
