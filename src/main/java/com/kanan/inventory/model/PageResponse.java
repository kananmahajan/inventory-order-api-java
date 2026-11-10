package com.kanan.inventory.model;
import java.util.List;
public record PageResponse<T>(List<T> content,int page,int size,long totalElements,int totalPages) {
    public static <T> PageResponse<T> of(List<T> content,int page,int size,long total){int pages=(int)Math.ceil((double)total/size);return new PageResponse<>(content,page,size,total,pages);}
}
