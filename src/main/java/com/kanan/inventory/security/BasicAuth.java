package com.kanan.inventory.security;

import com.kanan.inventory.AppConfig;
import com.kanan.inventory.api.ApiException;
import com.sun.net.httpserver.HttpExchange;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public final class BasicAuth {
    private final AppConfig config;
    public BasicAuth(AppConfig config){this.config=config;}
    public Identity require(HttpExchange exchange){
        String header=exchange.getRequestHeaders().getFirst("Authorization");
        if(header==null||!header.startsWith("Basic "))throw ApiException.unauthorized();
        try{
            String decoded=new String(Base64.getDecoder().decode(header.substring(6).trim()),StandardCharsets.UTF_8);
            int colon=decoded.indexOf(':'); if(colon<1)throw ApiException.unauthorized();
            String user=decoded.substring(0,colon),password=decoded.substring(colon+1);
            if(equal(user,config.adminUser())&&equal(password,config.adminPassword()))return new Identity(user,Identity.Role.ADMIN);
            if(equal(user,config.customerUser())&&equal(password,config.customerPassword()))return new Identity(user,Identity.Role.CUSTOMER);
        }catch(IllegalArgumentException ignored){throw ApiException.unauthorized();}
        throw ApiException.unauthorized();
    }
    public Identity requireAdmin(HttpExchange exchange){Identity who=require(exchange);if(!who.isAdmin())throw ApiException.forbidden();return who;}
    private boolean equal(String left,String right){return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8),right.getBytes(StandardCharsets.UTF_8));}
}
