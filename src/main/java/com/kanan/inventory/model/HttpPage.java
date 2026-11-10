package com.kanan.inventory.model;
import com.kanan.inventory.api.ApiException;
import java.util.Map;
public final class HttpPage {
    private HttpPage(){}
    public static int page(Map<String,String> q){return read(q,"page",0,0,Integer.MAX_VALUE);}
    public static int size(Map<String,String> q){return read(q,"size",20,1,100);}
    private static int read(Map<String,String> q,String key,int def,int min,int max){
        try{int n=Integer.parseInt(q.getOrDefault(key,String.valueOf(def)));if(n<min||n>max)throw new NumberFormatException();return n;}
        catch(NumberFormatException e){throw ApiException.badRequest(key+" must be between "+min+" and "+max);}
    }
}
