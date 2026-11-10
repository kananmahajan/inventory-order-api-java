package com.kanan.inventory.model;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
public record OrderView(long id,String orderNumber,String customerUsername,String status,BigDecimal totalAmount,
                        Instant createdAt,Instant cancelledAt,List<OrderLineView> items) {}
