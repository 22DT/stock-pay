package com.example.demo.api.item.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@NoArgsConstructor
@Getter
public class Item {
    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name="item_id")
    private Long id;


    private String itemName;
}
