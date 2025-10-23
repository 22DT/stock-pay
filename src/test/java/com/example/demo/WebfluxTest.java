package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class WebfluxTest {


    @Test
    public void test() {
        Mono.just("A")
                .doOnNext(v -> log.info("[1] just emit: {} (thread={})", v, Thread.currentThread().getName()))
                .subscribeOn(Schedulers.boundedElastic()) //  체인의 '시작' 스레드 변경
                .map(v -> {
                    log.info("[2] map to lower (thread={})", Thread.currentThread().getName());
                    return v.toLowerCase();
                })
                .publishOn(Schedulers.parallel()) //  이후 Operator 들은 parallel 스레드에서 실행
                .flatMap(v -> {
                    log.info("[3] flatMap start (thread={})", Thread.currentThread().getName());
                    return Mono.fromCallable(() -> {
                        log.info("[4] inside flatMap callable (thread={})", Thread.currentThread().getName());
                        return v.repeat(3);
                    });
                })
                .publishOn(Schedulers.single()) //  이후 단계는 single 스레드에서 실행
                .map(v -> {
                    log.info("[5] map again (thread={})", Thread.currentThread().getName());
                    return v + "_done";
                })
                .subscribeOn(Schedulers.parallel()) //  이미 subscribeOn 한 번 썼기 때문에 무시됨
                .doOnNext(v -> log.info("[6] before subscribe (thread={})", Thread.currentThread().getName()))
//                .subscribe(v -> log.info("[7] subscriber received: {} (thread={})", v, Thread.currentThread().getName()));

                .block();
    }
}
