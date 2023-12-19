package ru.shop.backend.search.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableElasticsearchRepositories
@EnableScheduling
public class AppConfig {
    @Bean
    public ClientConfiguration clientConfiguration(@Value("${spring.elasticsearch.uris}") String elasticUrl) {
        return ClientConfiguration.builder().connectedTo(elasticUrl)
                .build();
    }
    @Bean
    @Autowired
    public RestHighLevelClient restHighLevelClient(ClientConfiguration client) {
        return RestClients.create(client).rest();
    }
}
