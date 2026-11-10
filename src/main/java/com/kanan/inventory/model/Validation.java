package com.kanan.inventory.model;

import com.kanan.inventory.api.ApiException;
import java.math.BigDecimal;
import java.util.List;

public final class Validation {
    private Validation(){}
    public static ProductInput product(ProductInput in){
        if(in==null)throw ApiException.badRequest("Request body is required");
        String sku=text(in.sku(),"sku",64); if(!sku.matches("[A-Za-z0-9._-]+"))throw ApiException.badRequest("sku may contain letters, digits, dot, underscore, and hyphen only");
        String name=text(in.name(),"name",160),category=text(in.category(),"category",100);
        BigDecimal price=in.price(); if(price==null||price.signum()<=0||price.scale()>2||price.precision()-price.scale()>10)throw ApiException.badRequest("price must be positive, fit NUMERIC(12,2), and have at most two decimal places");
        if(in.stockQuantity()==null||in.stockQuantity()<0)throw ApiException.badRequest("stockQuantity must be zero or greater");
        return new ProductInput(sku,name,category,price,in.stockQuantity());
    }
    public static int adjustment(StockAdjustment in){if(in==null||in.adjustment()==null||in.adjustment()==0)throw ApiException.badRequest("adjustment must be a non-zero integer");return in.adjustment();}
    public static List<OrderLineInput> order(CreateOrderInput in){
        if(in==null||in.items()==null||in.items().isEmpty())throw ApiException.badRequest("items must contain at least one product");
        if(in.items().size()>50)throw ApiException.badRequest("An order may contain at most 50 different products");
        var seen=new java.util.HashSet<Long>();
        for(var line:in.items()){
            if(line==null||line.productId()==null||line.productId()<=0)throw ApiException.badRequest("Each item needs a positive productId");
            if(line.quantity()<=0)throw ApiException.badRequest("Each item quantity must be positive");
            if(!seen.add(line.productId()))throw ApiException.badRequest("Each product may appear only once in an order");
        }
        return in.items();
    }
    private static String text(String value,String name,int max){
        if(value==null||value.isBlank())throw ApiException.badRequest(name+" is required");
        String s=value.trim(); if(s.length()>max)throw ApiException.badRequest(name+" must be at most "+max+" characters"); return s;
    }
}
