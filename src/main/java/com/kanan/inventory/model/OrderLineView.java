package com.kanan.inventory.model;
import java.math.BigDecimal;
public record OrderLineView(long productId,String sku,String productName,BigDecimal unitPrice,int quantity,BigDecimal lineTotal) {}
