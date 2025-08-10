package com.example.demo.api.item.enums;

public enum StockHistoryType {
    PLUS("재고 증가"),
    MINUS("재고 감소");

    private final String description;

    StockHistoryType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

