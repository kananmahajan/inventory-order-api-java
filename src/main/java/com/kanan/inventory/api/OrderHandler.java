package com.kanan.inventory.api;

import com.kanan.inventory.model.CreateOrderInput;
import com.kanan.inventory.model.HttpPage;
import com.kanan.inventory.security.BasicAuth;
import com.kanan.inventory.security.Identity;
import com.kanan.inventory.service.OrderService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class OrderHandler implements HttpHandler {
    private final OrderService service;private final BasicAuth auth;
    public OrderHandler(OrderService service,BasicAuth auth){this.service=service;this.auth=auth;}
    @Override public void handle(HttpExchange x)throws IOException{try{route(x);}catch(Throwable e){HttpUtil.fail(x,e);}}
    private void route(HttpExchange x)throws IOException,SQLException{
        String path=x.getRequestURI().getPath(),method=x.getRequestMethod(),base="/api/v1/orders";
        String tail=path.length()<=base.length()?"":path.substring(base.length());
        if(tail.isEmpty()||"/".equals(tail)){
            if("POST".equals(method)){Identity who=auth.require(x);HttpUtil.json(x,201,service.create(who.username(),HttpUtil.readJson(x,CreateOrderInput.class)));return;}
            if("GET".equals(method)){auth.requireAdmin(x);Map<String,String> q=HttpUtil.query(x);HttpUtil.json(x,200,service.list(null,true,HttpPage.page(q),HttpPage.size(q)));return;}
            throw ApiException.methodNotAllowed();
        }
        if("/me".equals(tail)&&"GET".equals(method)){Identity who=auth.require(x);Map<String,String> q=HttpUtil.query(x);HttpUtil.json(x,200,service.list(who.username(),false,HttpPage.page(q),HttpPage.size(q)));return;}
        String[] part=tail.substring(1).split("/");long id=ProductHandler.parseId(part[0]);Identity who=auth.require(x);
        if(part.length==1&&"GET".equals(method)){HttpUtil.json(x,200,service.get(id,who.username(),who.isAdmin()));return;}
        if(part.length==2&&"cancel".equals(part[1])&&"POST".equals(method)){HttpUtil.json(x,200,service.cancel(id,who.username(),who.isAdmin()));return;}
        throw ApiException.notFound();
    }
}
