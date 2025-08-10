package com.example.demo.common.entity;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 카디널리티를 낮추기 위해 도입.
 *
 * LocalDateTime createdAt 는  sequential 하지만 카디널리티가 높아 DB 부하 증가할 수 있음.
 * 따라서 카디널리티를 낮추기 위해 LocalDate createdDate 이용
 */

@MappedSuperclass
@Getter
public abstract class ExtendedBaseTimeEntity extends BaseTimeEntity {

    protected LocalDate createdDate;

    @PrePersist
    protected void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.createdDate = this.createdAt.toLocalDate();
    }
}
