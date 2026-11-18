package com.moonligh.consult.framework.vector;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** 没配密钥或向量库连不上时，嵌入返回空，查询返回空列表。 */
@AutoConfiguration
@EnableConfigurationProperties(VectorProperties.class)
public class VectorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public VectorStore vectorStore(VectorProperties properties) {
        return new MilvusVectorStore(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public TextReranker textReranker(VectorProperties properties) {
        return new BailianRerankClient(properties);
    }
}
