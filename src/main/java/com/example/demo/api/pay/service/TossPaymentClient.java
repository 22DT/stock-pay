package com.example.demo.api.pay.service;

import com.example.demo.api.item.repository.ItemRepository;
import com.example.demo.api.member.entity.Member;
import com.example.demo.api.member.repository.MemberRepository;
import com.example.demo.api.pay.dto.external.request.TossPaymentCancelRequestDTO;
import com.example.demo.api.pay.dto.external.request.TossPaymentConfirmRequestDTO;
import com.example.demo.api.pay.dto.external.response.TossPaymentResponseDTO;
import com.example.demo.api.pay.dto.internal.PaymentCancelDTO;
import com.example.demo.api.pay.dto.internal.PaymentDTO;
import com.example.demo.api.pay.enums.TossPaymentCancelExceptionType;
import com.example.demo.api.pay.enums.TossPaymentConfirmExceptionType;
import com.example.demo.api.pay.enums.TossPaymentGetExceptionType;
import com.example.demo.api.pay.exception.PaymentException;
import com.example.demo.api.pay.exception.PaymentTimeoutException;
import com.example.demo.api.pay.exception.PaymentUnknownException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 연결 타임아웃: 재시도 가능
 * 읽기 타임아웃: 읽기 작업, 멱등성이 보장된 갱신만 할 것.
 *
 * 재시도 횟수: 계속 x 응답 시간도 길어짐. 1 ~ 2회 정도 그 이상이면 외부 시스템 문제 있다고 판단.
 * 재시도 간격: 즉시 할 경우 똑같은 문제로 타임아웃 -> 조금 기달렸다가 시도.
 *
 * 재시도 요청이 많을 경우 외부 서비스 부하 -> 우리도 같이 느려짐. -> 동시 요청 제한 고려(벌크헤드 패턴 등).
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentClient {
    private static final String TOSS_PAYMENT_CONFIRM_URL = "/v1/payments/confirm";
    private static final String TOSS_PAYMENT_GET_URL = "/v1/payments/{paymentKey}";
    private static final String TOSS_PAYMENT_CANCEL_URL = "/v1/payments/{paymentKey}/cancel";
    private final RestTemplate restTemplate;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final PaymentUpdater paymentUpdater;
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;

    @Value("${toss.payments.secret-key}")
    private String secretKey;

    @Value("${toss.payments.base-url}")
    private String baseUrl;


    /**
     * 결제 승인 요청
     */

    @Retryable(
            retryFor = { ResourceAccessException.class, HttpServerErrorException.class },
            maxAttempts = 3,
            backoff = @Backoff(
                    delay = 1000,
                    multiplier = 2.0,
                    random = true,
                    maxDelay = 5000
            )
    )
    public PaymentDTO confirmPayment(String paymentKey, String orderId, Long amount) {
        log.info("[confirmPayment][call]");

        String encodedAuth = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.setContentType(MediaType.APPLICATION_JSON);

//        headers.set("TossPayments-Test-Code", "UNKNOWN_PAYMENT_ERROR");

        TossPaymentConfirmRequestDTO request = new TossPaymentConfirmRequestDTO(paymentKey, orderId, amount);

        HttpEntity<TossPaymentConfirmRequestDTO> entity = new HttpEntity<>(request, headers);


        try {

            ResponseEntity<TossPaymentResponseDTO> response = restTemplate.exchange(
                    baseUrl + TOSS_PAYMENT_CONFIRM_URL,
                    HttpMethod.POST,
                    entity,
                    TossPaymentResponseDTO.class
            );

            TossPaymentResponseDTO body = response.getBody();
            return PaymentDTO.from(body);

        } catch (HttpClientErrorException e) { // 4xx 에러
            log.warn("[confirmPayment][4XX 에러 ][paymentKey={}]", paymentKey, e);
            String responseBody = e.getResponseBodyAsString();
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());

            String code;
            String message;
            try {
                JsonNode json = objectMapper.readTree(responseBody);
                code= json.path("code").asText();
                message= json.path("message").asText();
            } catch (Exception parseEx) {
                // 파싱 실패 시에도 알 수 없는 예외 던짐
                throw new PaymentUnknownException(status, "Failed to parse error response");
            }

            PaymentException paymentException = TossPaymentConfirmExceptionType.fromErrorCode(
                    status,
                    code,
                    message
            );

            log.info("[confirmPayment][paymentException= {}]", paymentException.getClass());

            throw paymentException;
        }
    }

    @Recover
    public PaymentDTO recoverConfirmPayment(ResourceAccessException e, String paymentKey, String orderId, Long amount) {
        log.error("[confirmPayment][네트워크 에러로 결제 승인 실패][merchantOrderId= {}, paymentKey= {}]", orderId, paymentKey, e);
        throw new PaymentTimeoutException(HttpStatus.REQUEST_TIMEOUT, e.getMessage());
    }

    @Recover
    public PaymentDTO recoverConfirmPayment(HttpServerErrorException e, String paymentKey, String orderId, Long amount) {
        log.error("[confirmPayment][서버 오류로 결제 승인 실패][merchantOrderId= {}, paymentKey= {}]", orderId, paymentKey, e);
        String responseBody = e.getResponseBodyAsString();
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        String message;

        try {
            JsonNode json = objectMapper.readTree(responseBody);
            message = json.path("message").asText();
        } catch (Exception parseEx) {
            // 파싱 실패 시에도 알 수 없는 예외 던짐
            throw new PaymentUnknownException(status, "Failed to parse error response");
        }
        throw new PaymentTimeoutException(HttpStatus.resolve(e.getStatusCode().value()), message);
    }
    @Recover
    public PaymentDTO recoverConfirmPayment(PaymentException e, String paymentKey, String orderId, Long amount) {
        throw e;
    }

    /**
     * 결제 요청 (non blocking)
     */
    public CompletableFuture<Void> confirmPaymentAsync(String paymentKey, String orderId, Long amount) {
        log.info("[confirmPaymentAsync][start] paymentKey={}, orderId={}", paymentKey, orderId);

        String encodedAuth = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        WebClient.RequestHeadersSpec<?> request = webClient.post()
                .uri(TOSS_PAYMENT_CONFIRM_URL)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(new TossPaymentConfirmRequestDTO(paymentKey, orderId, amount));

        return request.retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class)
                                .doOnNext(body -> log.error("[HTTP 4xx] {}", body))
                                .then(Mono.error(new RuntimeException("클라이언트 오류 발생")))
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class)
                                .doOnNext(body -> log.error("[HTTP 5xx] {}", body))
                                .then(Mono.error(new RuntimeException("서버 오류 발생")))
                )
                .bodyToMono(TossPaymentResponseDTO.class)
                .doOnNext(r -> log.info("[1] after retrieve"))
                .map(PaymentDTO::from)
                .doOnNext(dto -> log.info("[2] after map"))
                .doOnNext(dto -> log.info("[3] after publishOn"))
                .flatMap(dto -> Mono.fromRunnable(() -> paymentUpdater.updatePaymentAsDone(dto))
                                .subscribeOn(Schedulers.boundedElastic())
                )
                .then()
                .toFuture();


    }

    private Mono<? extends Throwable> handle4xxError(ClientResponse response, String paymentKey) {
        log.warn("[confirmPayment][4XX 에러][paymentKey={}]", paymentKey);


        return response.bodyToMono(String.class)
                .flatMap(body -> {
                    try {
                        JsonNode json = objectMapper.readTree(body);
                        String code = json.path("code").asText();
                        String message = json.path("message").asText();

                        PaymentException paymentException = TossPaymentConfirmExceptionType.fromErrorCode(
                                HttpStatus.resolve(response.statusCode().value()),
                                code,
                                message
                        );

                        return Mono.fromRunnable(() -> {
                                    paymentUpdater.updatePaymentAsFailed(paymentKey); // 예시
                                })
                                .subscribeOn(Schedulers.boundedElastic()) // DB I/O는 블로킹 → boundedElastic
                                .then(Mono.error(paymentException)); // 업데이트 끝나면 예외 발생시킴

                    } catch (Exception e) {
                        return Mono.fromRunnable(() -> {
                                    paymentUpdater.updatePaymentAsFailed(paymentKey);
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .then(Mono.error(new PaymentUnknownException(
                                        HttpStatus.resolve(response.statusCode().value()),
                                        "Failed to parse error response"
                                )));
                    }
                });
    }

    private Mono<? extends Throwable> handle5xxError(ClientResponse response, String paymentKey) {
        log.error("[confirmPayment][5XX 에러][paymentKey={}]", paymentKey);
        return response.bodyToMono(String.class)
                .flatMap(body -> {
                    try {
                        JsonNode json = objectMapper.readTree(body);
                        String message = json.path("message").asText();
                        return Mono.error(new PaymentTimeoutException(HttpStatus.resolve(response.statusCode().value()), message));
                    } catch (Exception e) {
                        return Mono.error(new PaymentUnknownException(HttpStatus.resolve(response.statusCode().value()), "Failed to parse error response"));
                    }
                });
    }

    private Mono<Void> handleFinal5xxError(Throwable throwable, String paymentKey) {
        log.error("[confirmPayment][500/timeout 재시도 실패 최종 처리][paymentKey={}]", paymentKey);

        if (throwable instanceof WebClientResponseException ex) {
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            String message;
            try {
                JsonNode json = objectMapper.readTree(ex.getResponseBodyAsString());
                message = json.path("message").asText();
            } catch (Exception parseEx) {
                message = "Failed to parse error response";
            }
            return Mono.error(new PaymentTimeoutException(status, message));
        }

        if (throwable instanceof WebClientRequestException ex) {
            return Mono.error(new PaymentTimeoutException(HttpStatus.REQUEST_TIMEOUT, ex.getMessage()));
        }

        return Mono.error(throwable);
    }




    /**
     * 결제 조회 요청
     *
     * IN_PROGRESS: 승인 요청 x인 겨
     */
    @Retryable(
            retryFor = { ResourceAccessException.class, HttpServerErrorException.class },
            maxAttempts = 3,
            backoff = @Backoff(
                    delay = 1000,
                    multiplier = 2.0,
                    random = true,
                    maxDelay = 5000
            )
    )
    public PaymentDTO getPaymentByPaymentKey(String paymentKey) {
        log.info("[getPayment][call]");

        String encodedAuth = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedAuth);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<TossPaymentResponseDTO> response = restTemplate.exchange(
                    baseUrl + TOSS_PAYMENT_GET_URL,
                    HttpMethod.GET,
                    entity,
                    TossPaymentResponseDTO.class,
                    paymentKey
            );

            TossPaymentResponseDTO body = response.getBody();
            return PaymentDTO.from(body);

        } catch (HttpClientErrorException e) {
            log.warn("[getPayment][4XX 에러][paymentKey= {}]", paymentKey, e);

            String responseBody = e.getResponseBodyAsString();
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            String code ;
            String message;
            try {
                JsonNode json = objectMapper.readTree(responseBody);
                 code = json.path("code").asText();
                 message = json.path("message").asText();


            } catch (Exception parseEx) {
                // 파싱 실패 시에도 알 수 없는 예외 던짐
                throw new PaymentUnknownException(status, "Failed to parse error response");
            }

            throw TossPaymentGetExceptionType.fromErrorCode(
                    status,
                    code,
                    message
            );
        }
    }

    @Recover
    public PaymentDTO recoverGetPaymentByPaymentKey(ResourceAccessException e, String paymentKey) {
        log.error("[getPayment][네트워크 에러로 결제 승인 실패][paymentKey= {}]", paymentKey, e);
        throw new PaymentTimeoutException(HttpStatus.REQUEST_TIMEOUT, e.getMessage());
    }

    @Recover
    public PaymentDTO recoverGetPaymentByPaymentKey(HttpServerErrorException e, String paymentKey) {
        log.error("[getPayment][서버 오류로 결제 승인 실패][paymentKey= {}]", paymentKey, e);

        String responseBody = e.getResponseBodyAsString();
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        String message;

        try {
            JsonNode json = objectMapper.readTree(responseBody);
            message = json.path("message").asText();
        } catch (Exception parseEx) {
            // 파싱 실패 시에도 알 수 없는 예외 던짐
            throw new PaymentUnknownException(status, "Failed to parse error response");
        }
        throw new PaymentTimeoutException(HttpStatus.resolve(e.getStatusCode().value()), message);
    }

    @Recover
    public PaymentDTO recoverGetPaymentByPaymentKey(PaymentException e, String paymentKey) {
        throw e;
    }


    /**
     * 결제 조회 (non blocking)
     */
    public CompletableFuture<Void> getPaymentAsync(Long memberId) {
        String paymentKey = "tviva20251022140747TrPL7";

        log.info("[getPaymentAsync][start] paymentKey={}", paymentKey);

        String encodedAuth = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        WebClient.RequestHeadersSpec<?> request = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(TOSS_PAYMENT_GET_URL)
                        .build(paymentKey))
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);


        return request.retrieve()
                .bodyToMono(TossPaymentResponseDTO.class)
                .map(PaymentDTO::from)
                .doOnNext(dto -> log.info("[getPaymentAsync][success] {}", dto))
                .doOnError(e -> log.warn("[getPaymentAsync][error] {}", e.getMessage(), e))
                .publishOn(Schedulers.boundedElastic())
                .flatMap(dto -> Mono.fromRunnable(() ->{
                    log.info("[BusinessLogic] 처리 완료, dto={}, memberId={}", dto, memberId);
                    Member findMember = memberRepository.findById(memberId).get();
                    log.info("[BusinessLogic][findMemberId= {}]", findMember.getId());
                        }

                ))

                .then()
                .toFuture();

    }



    /**
     * 결제 취소
     * TossPaymentStatus: CANCELED, ALREADY_CANCELED, RETRY
     */
    @Retryable(
            retryFor = { ResourceAccessException.class, HttpServerErrorException.class },
            maxAttempts = 3,
            backoff = @Backoff(
                    delay = 1000,
                    multiplier = 2.0,
                    random = true,
                    maxDelay = 5000
            )
    )
    public List<PaymentCancelDTO> cancelPayment(String paymentKey, String cancelReason) {
        String encodedAuth = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.setContentType(MediaType.APPLICATION_JSON);

        TossPaymentCancelRequestDTO request = new TossPaymentCancelRequestDTO(cancelReason, null);
        HttpEntity<TossPaymentCancelRequestDTO> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<TossPaymentResponseDTO> response = restTemplate.exchange(
                    baseUrl + TOSS_PAYMENT_CANCEL_URL,
                    HttpMethod.POST,
                    entity,
                    TossPaymentResponseDTO.class,
                    paymentKey
            );

            TossPaymentResponseDTO body = response.getBody();
            List<TossPaymentResponseDTO.Cancel> cancels = body.cancels();

            List<PaymentCancelDTO> paymentCancelDTOs = PaymentCancelDTO.fromList(cancels);

            return paymentCancelDTOs;

        } catch (HttpClientErrorException e) {
            log.warn("[cancelPayment][4XX 에러 ][paymentKey={}]", paymentKey, e);
            String responseBody = e.getResponseBodyAsString();
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            String code;
            String message;

            try {
                JsonNode json = objectMapper.readTree(responseBody);
                code = json.path("code").asText();
                message = json.path("message").asText();

            } catch (Exception parseEx) {
                // 파싱 실패 시에도 알 수 없는 예외 던짐
                throw new PaymentUnknownException(status, "Failed to parse error response");
            }

            throw TossPaymentCancelExceptionType.fromErrorCode(
                    status,
                    code,
                    message
            );
        }
    }

    @Recover
    public List<PaymentCancelDTO> recoverCancelPayment(ResourceAccessException e, String paymentKey, String cancelReason) {
        log.error("[recoverCancelPayment][네트워크 에러로 결제 승인 실패][paymentKey= {}]", paymentKey, e);
        throw new PaymentTimeoutException(HttpStatus.REQUEST_TIMEOUT, e.getMessage());
    }

    @Recover
    public List<PaymentCancelDTO> recoverCancelPayment(HttpServerErrorException e, String paymentKey, String cancelReason) {
        log.error("[recoverCancelPayment][서버 오류로 결제 승인 실패][paymentKey= {}]", paymentKey, e);

        String responseBody = e.getResponseBodyAsString();
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        String message;

        try {
            JsonNode json = objectMapper.readTree(responseBody);
            message = json.path("message").asText();
        } catch (Exception parseEx) {
            // 파싱 실패 시에도 알 수 없는 예외 던짐
            throw new PaymentUnknownException(status, "Failed to parse error response");
        }
        throw new PaymentTimeoutException(HttpStatus.resolve(e.getStatusCode().value()), message);
    }

    @Recover
    public List<PaymentCancelDTO> recoverCancelPayment(PaymentException e, String paymentKey, String cancelReason) {
        throw e;
    }

}

