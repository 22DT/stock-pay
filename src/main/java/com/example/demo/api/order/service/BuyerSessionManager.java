package com.example.demo.api.order.service;

import com.example.demo.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BuyerSessionManager {

    private final Set<Long> activeBuyers = ConcurrentHashMap.newKeySet();

    // 시작
    public void startSession(Long buyerId) {
        boolean added = activeBuyers.add(buyerId);
        if (!added) {
            throw new BadRequestException("이미 진행 중인 재고 처리 요청이 있습니다.");
        }
    }

    // 종료
    public void endSession(Long buyerId) {
        activeBuyers.remove(buyerId);
    }

    // 테스트용
    public boolean isSessionActive(Long buyerId) {
        return activeBuyers.contains(buyerId);
    }
}


