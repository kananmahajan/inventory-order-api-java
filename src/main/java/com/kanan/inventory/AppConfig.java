package com.kanan.inventory;

public record AppConfig(int port,String jdbcUrl,String dbUser,String dbPassword,
                        String adminUser,String adminPassword,String customerUser,String customerPassword) {
    public static AppConfig fromEnvironment(){
        return new AppConfig(integer("PORT",8080),value("DB_URL","jdbc:postgresql://localhost:5432/inventory_db"),
            value("DB_USERNAME","inventory"),required("DB_PASSWORD"),
            value("ADMIN_USERNAME","admin"),required("ADMIN_PASSWORD"),
            value("CUSTOMER_USERNAME","customer"),required("CUSTOMER_PASSWORD"));
    }
    private static String required(String key){String v=System.getenv(key);if(v==null||v.isBlank())throw new IllegalArgumentException("Set the required environment variable "+key);return v;}
    private static String value(String key,String fallback){String v=System.getenv(key);return v==null||v.isBlank()?fallback:v;}
    private static int integer(String key,int fallback){try{return Integer.parseInt(value(key,String.valueOf(fallback)));}catch(NumberFormatException e){throw new IllegalArgumentException(key+" must be an integer");}}
}
