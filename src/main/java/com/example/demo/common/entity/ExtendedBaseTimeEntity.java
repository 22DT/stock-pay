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
 *
 *
 * SELECT
 *     index_name,
 *     ROUND(SUM(stat_value * @@innodb_page_size)/1024/1024, 2) AS index_size_mb
 * FROM mysql.innodb_index_stats
 * WHERE database_name = 'test_db'
 *   AND table_name = 'payment'
 * GROUP BY index_name;
 *
 * mysql.innodb_index_stats
 * → InnoDB가 내부적으로 유지하는 통계 테이블
 * → 페이지 수, 리프 노드 개수, 레코드 수 등 정보 포함
 *
 * stat_value * @@innodb_page_size
 * → 인덱스 통계에 있는 페이지 수를 바이트 단위로 환산
 *
 * /1024/1024 → MB 단위로 변환
 *
 * GROUP BY index_name → 각 인덱스별 크기 계산
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
