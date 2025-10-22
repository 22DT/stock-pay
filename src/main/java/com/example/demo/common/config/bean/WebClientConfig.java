package com.example.demo.common.config.bean;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        // 1️⃣ 커넥션 풀 설정 (Reactor Netty)
        ConnectionProvider provider = ConnectionProvider.builder("payment-pool")
                .maxConnections(200) // 동시에 외부 API 연결 200개까지
                .pendingAcquireTimeout(Duration.ofSeconds(5)) // 커넥션 대기 시간
                .build();

        // 2️⃣ Netty 기반 HttpClient 설정
        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 연결 타임아웃
                .responseTimeout(Duration.ofSeconds(10)) // 응답 타임아웃
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(10)) // read timeout
                                .addHandlerLast(new WriteTimeoutHandler(10)) // write timeout
                );

        // 3️⃣ WebClient 빌더 설정
        return builder
                .baseUrl("https://api.tosspayments.com") // 기본 baseUrl
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
