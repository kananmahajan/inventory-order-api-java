package com.kanan.inventory.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private final String url,user,password;
    public Database(String url,String user,String password){this.url=url;this.user=user;this.password=password;}
    public Connection open() throws SQLException{return DriverManager.getConnection(url,user,password);}
    @FunctionalInterface public interface Work<T>{T run(Connection connection)throws SQLException;}
    public <T>T transaction(Work<T> work)throws SQLException{
        try(Connection c=open()){
            c.setAutoCommit(false);
            try{T value=work.run(c);c.commit();return value;}
            catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}throw e;}
        }
    }
    public void initialize() throws IOException,SQLException{
        String sql;
        try(var in=Database.class.getResourceAsStream("/schema.sql")){
            if(in==null)throw new IOException("schema.sql is missing from the application resources");
            sql=new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
        try(Connection c=open();Statement statement=c.createStatement()){
            for(String command:sql.split(";"))if(!command.isBlank())statement.execute(command);
        }
    }
}
