package com.example.demo.common.config.bean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        /*
         * 연결 타입아웃: 3~5초
         * 응답 타임아웃: 5~30초
         *
         * 너무 오래 or 짦게 x 이후 추이를 보면서 조정.
         *
         */
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(10000);

        restTemplate.setRequestFactory(requestFactory);

        return restTemplate;
    }
}

