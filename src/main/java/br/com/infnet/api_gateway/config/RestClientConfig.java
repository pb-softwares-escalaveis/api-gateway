package br.com.infnet.api_gateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Primary
    @Bean(name = "defaultRestClientBuilder")
    public RestClient.Builder defaultRestClientBuilder() {
        return RestClient.builder();
    }

    @LoadBalanced
    @Bean(name = "loadBalancedRestClientBuilder")
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}