package com.kanan.inventory.model;
import java.math.BigDecimal;
public record ProductInput(String sku,String name,String category,BigDecimal price,Integer stockQuantity) {}
