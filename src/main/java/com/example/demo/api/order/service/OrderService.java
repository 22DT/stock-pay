package com.example.demo.api.order.service;


import com.example.demo.api.item.entity.Item;
import com.example.demo.api.item.repository.ItemRepository;
import com.example.demo.api.member.entity.Member;
import com.example.demo.api.member.repository.MemberRepository;
import com.example.demo.api.order.dto.request.OrderRequestDTO;
import com.example.demo.api.order.entity.Order;
import com.example.demo.api.order.entity.OrderItem;
import com.example.demo.api.order.enums.OrderItemStatus;
import com.example.demo.api.order.enums.OrderStatus;
import com.example.demo.api.order.repository.OrderItemRepository;
import com.example.demo.api.order.repository.OrderRepository;
import com.example.demo.common.exception.BadRequestException;
import com.example.demo.common.exception.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.demo.common.response.ErrorStatus.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;
    private final OrderItemRepository orderItemRepository;

    /*
    * c
    * */

    @Transactional
    public void createOrder(Long buyerId, OrderRequestDTO orderRequestDTO) {
        Member buyer = memberRepository.findById(buyerId)
                .orElseThrow(() -> {
                    log.warn("[createOrder][멤버 없음.][buyerId={}]", buyerId);
                    return new NotFoundException(USER_NOT_FOUND_EXCEPTION.getMessage());
                });

        List<OrderRequestDTO.ItemQuantity> itemQuantities = orderRequestDTO.itemQuantities();
        List<Long> requestedIds = itemQuantities.stream()
                .map((OrderRequestDTO.ItemQuantity::itemId)).toList();
        List<Item> items = itemRepository.findAllById(requestedIds);

        // 실제 DB에 있는 ID
        List<Long> foundIds = items.stream()
                .map(Item::getId)
                .toList();

        // 없는 ID만 추출
        List<Long> missingIds = requestedIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        if (!missingIds.isEmpty()) {
            log.warn("[createOrder][이상한 passArchiveIds 넘겼음.][passArchiveIds= {}]", missingIds);
            throw new NotFoundException(PASS_ARCHIVE_NOT_FOUND_EXCEPTION.getMessage());
        }

        Long orderId;

        try {

            /*
             * 필요하면 여기서 order 관련 각종 권한 처리. update 필요한 작업은 x
             * */

            Order order = Order.builder()
                    .merchantOrderId(orderRequestDTO.merchantOrderId())
                    .amount(orderRequestDTO.amount())
                    .buyer(buyer)
                    .status(OrderStatus.PENDING)
                    .build();

            orderId = orderRepository.save(order).getId();

        } catch (DataIntegrityViolationException e) {
            log.warn("[createOrder][중복 merchantOrderId][merchantOrderId={}]", orderRequestDTO.merchantOrderId(), e);
            throw new BadRequestException(ALREADY_REGISTERED_MERCHANT_ORDER_ID_EXCEPTION.getMessage());
        }

        Order order = orderRepository.findById(orderId).get();

        // key: itemId, value: quantity
        Map<Long, Long > map=new HashMap<>();
        itemQuantities.forEach(
                (OrderRequestDTO.ItemQuantity itemQuantity) -> {
            map.put(itemQuantity.itemId(), itemQuantity.quantity());
        });


        items.forEach(
                (item) -> {
                    OrderItem orderPassArchive = OrderItem.builder()
                            .quantity(map.get(item.getId()))
                            .status(OrderItemStatus.PENDING)
                            .order(order)
                            .item(item)
                            .build();

                    orderItemRepository.save(orderPassArchive);
                });
    }
}
