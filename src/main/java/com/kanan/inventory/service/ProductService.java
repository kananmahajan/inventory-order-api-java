package com.kanan.inventory.service;

import com.kanan.inventory.api.ApiException;
import com.kanan.inventory.data.Database;
import com.kanan.inventory.model.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

public class ProductService {
    private final Database db;
    public ProductService(Database db){this.db=db;}

    public PageResponse<Product> search(Map<String,String> query)throws SQLException{
        int page=HttpPage.page(query),size=HttpPage.size(query); long offset=(long)page*size;
        String q=clean(query.get("q")),category=clean(query.get("category"));
        String where=" WHERE active=TRUE"; List<String> filters=new ArrayList<>();
        if(q!=null)filters.add("(LOWER(name) LIKE ? OR LOWER(sku) LIKE ?)");
        if(category!=null)filters.add("LOWER(category)=LOWER(?)");
        if(!filters.isEmpty())where+=" AND "+String.join(" AND ",filters);
        try(Connection c=db.open()){
            long total;
            try(PreparedStatement s=c.prepareStatement("SELECT COUNT(*) FROM products"+where)){
                bindFilters(s,q,category);try(ResultSet rs=s.executeQuery()){rs.next();total=rs.getLong(1);}
            }
            List<Product> values=new ArrayList<>();
            try(PreparedStatement s=c.prepareStatement("SELECT * FROM products"+where+" ORDER BY name,id LIMIT ? OFFSET ?")){
                int n=bindFilters(s,q,category);s.setInt(n++,size);s.setLong(n,offset);
                try(ResultSet rs=s.executeQuery()){while(rs.next())values.add(map(rs));}
            }
            return PageResponse.of(values,page,size,total);
        }
    }
    public Product get(long id)throws SQLException{
        try(Connection c=db.open();PreparedStatement s=c.prepareStatement("SELECT * FROM products WHERE id=? AND active=TRUE")){
            s.setLong(1,id);try(ResultSet rs=s.executeQuery()){if(!rs.next())throw ApiException.notFound();return map(rs);}
        }
    }
    public Product create(ProductInput raw){
        ProductInput in=Validation.product(raw);
        try{return db.transaction(c->{
            try(PreparedStatement s=c.prepareStatement("INSERT INTO products(sku,name,category,price,stock_quantity) VALUES(?,?,?,?,?) RETURNING *")){
                s.setString(1,in.sku());s.setString(2,in.name());s.setString(3,in.category());s.setBigDecimal(4,in.price());s.setInt(5,in.stockQuantity());
                try(ResultSet rs=s.executeQuery()){rs.next();return map(rs);}
            }
        });}catch(SQLException e){throw sqlError(e);}
    }
    public Product update(long id,ProductInput raw){
        ProductInput in=Validation.product(raw);
        try{return db.transaction(c->{
            try(PreparedStatement s=c.prepareStatement("UPDATE products SET sku=?,name=?,category=?,price=?,stock_quantity=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND active=TRUE RETURNING *")){
                s.setString(1,in.sku());s.setString(2,in.name());s.setString(3,in.category());s.setBigDecimal(4,in.price());s.setInt(5,in.stockQuantity());s.setLong(6,id);
                try(ResultSet rs=s.executeQuery()){if(!rs.next())throw ApiException.notFound();return map(rs);}
            }
        });}catch(SQLException e){throw sqlError(e);}
    }
    public Product adjustStock(long id,StockAdjustment adjustment){
        int delta=Validation.adjustment(adjustment);
        try{return db.transaction(c->{
            int current;
            try(PreparedStatement s=c.prepareStatement("SELECT stock_quantity FROM products WHERE id=? AND active=TRUE FOR UPDATE")){
                s.setLong(1,id);try(ResultSet rs=s.executeQuery()){if(!rs.next())throw ApiException.notFound();current=rs.getInt(1);}
            }
            long next=(long)current+delta;if(next<0||next>Integer.MAX_VALUE)throw ApiException.conflict("Adjustment would make stock invalid");
            try(PreparedStatement s=c.prepareStatement("UPDATE products SET stock_quantity=?,updated_at=CURRENT_TIMESTAMP WHERE id=? RETURNING *")){
                s.setInt(1,(int)next);s.setLong(2,id);try(ResultSet rs=s.executeQuery()){rs.next();return map(rs);}
            }
        });}catch(SQLException e){throw sqlError(e);}
    }
    public void deactivate(long id){
        try(Connection c=db.open();PreparedStatement s=c.prepareStatement("UPDATE products SET active=FALSE,updated_at=CURRENT_TIMESTAMP WHERE id=? AND active=TRUE")){
            s.setLong(1,id);if(s.executeUpdate()==0)throw ApiException.notFound();
        }catch(SQLException e){throw sqlError(e);}
    }
    private int bindFilters(PreparedStatement s,String q,String category)throws SQLException{
        int n=1;if(q!=null){s.setString(n++,"%"+q.toLowerCase(Locale.ROOT)+"%");s.setString(n++,"%"+q.toLowerCase(Locale.ROOT)+"%");}
        if(category!=null)s.setString(n++,category);return n;
    }
    private String clean(String v){return v==null||v.isBlank()?null:v.trim();}
    static Product map(ResultSet rs)throws SQLException{return new Product(rs.getLong("id"),rs.getString("sku"),rs.getString("name"),rs.getString("category"),rs.getBigDecimal("price"),rs.getInt("stock_quantity"),rs.getBoolean("active"),rs.getTimestamp("created_at").toInstant(),rs.getTimestamp("updated_at").toInstant());}
    static RuntimeException sqlError(SQLException e){if("23505".equals(e.getSQLState()))return ApiException.conflict("SKU is already in use");return new RuntimeException(e);}
}
