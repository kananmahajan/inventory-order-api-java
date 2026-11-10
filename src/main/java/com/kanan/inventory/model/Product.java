package com.kanan.inventory.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Product(long id,String sku,String name,String category,BigDecimal price,int stockQuantity,
                      boolean active,Instant createdAt,Instant updatedAt) {}
