package com.kanan.inventory;

import com.kanan.inventory.api.OrderHandler;
import com.kanan.inventory.api.ProductHandler;
import com.kanan.inventory.data.Database;
import com.kanan.inventory.security.BasicAuth;
import com.kanan.inventory.service.OrderService;
import com.kanan.inventory.service.ProductService;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args)throws Exception{
        AppConfig config=AppConfig.fromEnvironment();Database db=new Database(config.jdbcUrl(),config.dbUser(),config.dbPassword());
        db.initialize();BasicAuth auth=new BasicAuth(config);
        ProductService products=new ProductService(db);OrderService orders=new OrderService(db);
        HttpServer server=HttpServer.create(new InetSocketAddress(config.port()),0);
        server.createContext("/api/v1/products",new ProductHandler(products,auth));
        server.createContext("/api/v1/orders",new OrderHandler(orders,auth));
        server.createContext("/actuator/health",exchange->{try{com.kanan.inventory.api.HttpUtil.json(exchange,200,java.util.Map.of("status","UP"));}catch(Exception e){exchange.close();}});
        server.setExecutor(Executors.newFixedThreadPool(Math.max(4,Runtime.getRuntime().availableProcessors())));
        server.start();System.out.println("Inventory API listening at http://localhost:"+config.port());
    }
}
