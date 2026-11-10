package com.kanan.inventory.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HttpUtil {
    public static final int MAX_BODY=1_000_000;
    private static final ObjectMapper JSON=new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private HttpUtil(){}
    public static <T>T readJson(HttpExchange x,Class<T> type)throws IOException{
        byte[] data=x.getRequestBody().readNBytes(MAX_BODY+1);
        if(data.length>MAX_BODY)throw ApiException.badRequest("Request body exceeds 1 MB");
        try{return JSON.readValue(data,type);}catch(JsonProcessingException e){throw ApiException.badRequest("Request body is not valid JSON");}
    }
    public static Map<String,String> query(HttpExchange x){
        Map<String,String> out=new LinkedHashMap<>(); String raw=x.getRequestURI().getRawQuery();
        if(raw==null||raw.isBlank())return out;
        try{for(String part:raw.split("&")){String[] pair=part.split("=",2);out.put(decode(pair[0]),pair.length==1?"":decode(pair[1]));}}catch(IllegalArgumentException e){throw ApiException.badRequest("Invalid URL encoding in query string");}
        return out;
    }
    private static String decode(String s){return URLDecoder.decode(s,StandardCharsets.UTF_8);}
    public static int integer(Map<String,String> q,String key,int fallback,int min,int max){
        try{int n=Integer.parseInt(q.getOrDefault(key,String.valueOf(fallback)));if(n<min||n>max)throw new NumberFormatException();return n;}
        catch(NumberFormatException e){throw ApiException.badRequest(key+" must be between "+min+" and "+max);}
    }
    public static void json(HttpExchange x,int status,Object body)throws IOException{
        byte[] bytes=JSON.writeValueAsBytes(body);x.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");
        x.getResponseHeaders().set("Cache-Control","no-store");x.sendResponseHeaders(status,bytes.length);
        try(var out=x.getResponseBody()){out.write(bytes);}
    }
    public static void empty(HttpExchange x,int status)throws IOException{x.sendResponseHeaders(status,-1);x.close();}
    public static void fail(HttpExchange x,Throwable error){
        if(x.getResponseCode()!=-1){x.close();return;}
        ApiException api=error instanceof ApiException a?a:ApiException.internal();
        if(api.status()==401)x.getResponseHeaders().set("WWW-Authenticate","Basic realm=\"Inventory API\", charset=\"UTF-8\"");
        Map<String,Object> body=Map.of("type","about:blank","title",title(api.status()),"status",api.status(),"detail",api.getMessage(),"timestamp",Instant.now());
        try{json(x,api.status(),body);}catch(IOException ignored){x.close();}
        if(!(error instanceof ApiException))error.printStackTrace(System.err);
    }
    private static String title(int status){return switch(status){case 400->"Bad Request";case 401->"Unauthorized";case 403->"Forbidden";case 404->"Not Found";case 405->"Method Not Allowed";case 409->"Conflict";default->"Internal Server Error";};}
}
