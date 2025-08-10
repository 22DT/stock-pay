package com.example.demo.api.item.enums;

import lombok.Getter;

@Getter
public enum SalesItemStatus {
    ON("활성"),
    OFF("비활성"),
    SOLD_OUT("품절");

    private final String description;

    SalesItemStatus(String description) {
        this.description = description;
    }

}
