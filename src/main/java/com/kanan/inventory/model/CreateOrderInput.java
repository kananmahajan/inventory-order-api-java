package com.kanan.inventory.model;
import java.util.List;
public record CreateOrderInput(List<OrderLineInput> items) {}
