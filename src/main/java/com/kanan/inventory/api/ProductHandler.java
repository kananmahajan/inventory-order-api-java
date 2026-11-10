package com.kanan.inventory.api;

import com.kanan.inventory.model.*;
import com.kanan.inventory.security.BasicAuth;
import com.kanan.inventory.service.ProductService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class ProductHandler implements HttpHandler {
    private final ProductService service;private final BasicAuth auth;
    public ProductHandler(ProductService service,BasicAuth auth){this.service=service;this.auth=auth;}
    @Override public void handle(HttpExchange x)throws IOException{
        try{route(x);}catch(Throwable e){HttpUtil.fail(x,e);}
    }
    private void route(HttpExchange x)throws IOException,SQLException{
        String path=x.getRequestURI().getPath(),method=x.getRequestMethod();String base="/api/v1/products";
        String tail=path.length()<=base.length()?"":path.substring(base.length());
        if(tail.isEmpty()||"/".equals(tail)){
            if("GET".equals(method)){HttpUtil.json(x,200,service.search(HttpUtil.query(x)));return;}
            if("POST".equals(method)){auth.requireAdmin(x);ProductInput in=HttpUtil.readJson(x,ProductInput.class);HttpUtil.json(x,201,service.create(in));return;}
            throw ApiException.methodNotAllowed();
        }
        String[] part=tail.substring(1).split("/");long id=parseId(part[0]);
        if(part.length==1&&"GET".equals(method)){HttpUtil.json(x,200,service.get(id));return;}
        if(part.length==1&&"PUT".equals(method)){auth.requireAdmin(x);HttpUtil.json(x,200,service.update(id,HttpUtil.readJson(x,ProductInput.class)));return;}
        if(part.length==1&&"DELETE".equals(method)){auth.requireAdmin(x);service.deactivate(id);HttpUtil.empty(x,204);return;}
        if(part.length==2&&"stock".equals(part[1])&&"PATCH".equals(method)){auth.requireAdmin(x);HttpUtil.json(x,200,service.adjustStock(id,HttpUtil.readJson(x,StockAdjustment.class)));return;}
        throw ApiException.notFound();
    }
    static long parseId(String value){try{long id=Long.parseLong(value);if(id<=0)throw new NumberFormatException();return id;}catch(NumberFormatException e){throw ApiException.badRequest("ID must be a positive integer");}}
}
