package com.kanan.inventory.service;

import com.kanan.inventory.api.ApiException;
import com.kanan.inventory.data.Database;
import com.kanan.inventory.model.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.*;

public class OrderService {
    private final Database db;
    public OrderService(Database db){this.db=db;}

    public OrderView create(String username,CreateOrderInput input){
        List<OrderLineInput> lines=Validation.order(input);
        try{return db.transaction(c->{
            Map<Long,Product> locked=new LinkedHashMap<>();
            for(long id:lines.stream().map(OrderLineInput::productId).sorted().toList()){
                Product p=lockProduct(c,id);if(!p.active())throw ApiException.conflict("Product "+p.sku()+" is inactive");locked.put(id,p);
            }
            String orderNumber=UUID.randomUUID().toString();long orderId;Instant created;
            try(PreparedStatement s=c.prepareStatement("INSERT INTO purchase_orders(order_number,customer_username,status,total_amount) VALUES(?,?,'PLACED',0) RETURNING id,created_at")){
                s.setObject(1,UUID.fromString(orderNumber));s.setString(2,username);try(ResultSet rs=s.executeQuery()){rs.next();orderId=rs.getLong("id");created=rs.getTimestamp("created_at").toInstant();}
            }
            BigDecimal total=BigDecimal.ZERO;List<OrderLineView> views=new ArrayList<>();
            for(OrderLineInput line:lines){
                Product p=locked.get(line.productId());if(p.stockQuantity()<line.quantity())throw ApiException.conflict("Insufficient stock for SKU "+p.sku());
                int remaining=p.stockQuantity()-line.quantity();
                try(PreparedStatement s=c.prepareStatement("UPDATE products SET stock_quantity=?,updated_at=CURRENT_TIMESTAMP WHERE id=?")){s.setInt(1,remaining);s.setLong(2,p.id());s.executeUpdate();}
                BigDecimal lineTotal=p.price().multiply(BigDecimal.valueOf(line.quantity()));total=total.add(lineTotal);
                try(PreparedStatement s=c.prepareStatement("INSERT INTO order_items(order_id,product_id,sku_at_purchase,product_name_at_purchase,unit_price,quantity,line_total) VALUES(?,?,?,?,?,?,?)")){
                    s.setLong(1,orderId);s.setLong(2,p.id());s.setString(3,p.sku());s.setString(4,p.name());s.setBigDecimal(5,p.price());s.setInt(6,line.quantity());s.setBigDecimal(7,lineTotal);s.executeUpdate();
                }
                views.add(new OrderLineView(p.id(),p.sku(),p.name(),p.price(),line.quantity(),lineTotal));
            }
            try(PreparedStatement s=c.prepareStatement("UPDATE purchase_orders SET total_amount=? WHERE id=?")){s.setBigDecimal(1,total);s.setLong(2,orderId);s.executeUpdate();}
            return new OrderView(orderId,orderNumber,username,"PLACED",total,created,null,List.copyOf(views));
        });}catch(SQLException e){throw new RuntimeException(e);}
    }

    public PageResponse<OrderView> list(String username,boolean admin,int page,int size)throws SQLException{
        long offset=(long)page*size;String where=admin?"":" WHERE customer_username=?";
        try(Connection c=db.open()){
            long total;try(PreparedStatement s=c.prepareStatement("SELECT COUNT(*) FROM purchase_orders"+where)){
                if(!admin)s.setString(1,username);try(ResultSet rs=s.executeQuery()){rs.next();total=rs.getLong(1);}
            }
            List<Long> ids=new ArrayList<>();try(PreparedStatement s=c.prepareStatement("SELECT id FROM purchase_orders"+where+" ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?")){
                int n=1;if(!admin)s.setString(n++,username);s.setInt(n++,size);s.setLong(n,offset);try(ResultSet rs=s.executeQuery()){while(rs.next())ids.add(rs.getLong(1));}
            }
            List<OrderView> views=new ArrayList<>();for(long id:ids)views.add(find(c,id));return PageResponse.of(views,page,size,total);
        }
    }
    public OrderView get(long id,String username,boolean admin)throws SQLException{
        try(Connection c=db.open()){OrderView o=find(c,id);checkOwner(o,username,admin);return o;}
    }
    public OrderView cancel(long id,String username,boolean admin){
        try{return db.transaction(c->{
            OrderView o=find(c,id,true);checkOwner(o,username,admin);
            if("CANCELLED".equals(o.status()))throw ApiException.conflict("Order is already cancelled");
            List<Long> productIds=o.items().stream().map(OrderLineView::productId).distinct().sorted().toList();Map<Long,Product> locked=new HashMap<>();
            for(long productId:productIds)locked.put(productId,lockProduct(c,productId));
            for(OrderLineView line:o.items()){
                Product p=locked.get(line.productId());long qty=(long)p.stockQuantity()+line.quantity();
                if(qty>Integer.MAX_VALUE)throw ApiException.conflict("Stock quantity would exceed the supported limit");
                try(PreparedStatement s=c.prepareStatement("UPDATE products SET stock_quantity=?,updated_at=CURRENT_TIMESTAMP WHERE id=?")){s.setInt(1,(int)qty);s.setLong(2,p.id());s.executeUpdate();}
            }
            try(PreparedStatement s=c.prepareStatement("UPDATE purchase_orders SET status='CANCELLED',cancelled_at=CURRENT_TIMESTAMP WHERE id=?")){s.setLong(1,id);s.executeUpdate();}
            return find(c,id);
        });}catch(SQLException e){throw new RuntimeException(e);}
    }
    private OrderView find(Connection c,long id)throws SQLException{return find(c,id,false);}
    private OrderView find(Connection c,long id,boolean forUpdate)throws SQLException{
        String sql="SELECT * FROM purchase_orders WHERE id=?"+(forUpdate?" FOR UPDATE":"");
        long orderId;String number,user,status;BigDecimal total;Instant created,cancelled;
        try(PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,id);try(ResultSet rs=s.executeQuery()){
            if(!rs.next())throw ApiException.notFound();orderId=rs.getLong("id");number=rs.getObject("order_number").toString();user=rs.getString("customer_username");status=rs.getString("status");total=rs.getBigDecimal("total_amount");created=rs.getTimestamp("created_at").toInstant();Timestamp ct=rs.getTimestamp("cancelled_at");cancelled=ct==null?null:ct.toInstant();
        }}
        List<OrderLineView> lines=new ArrayList<>();try(PreparedStatement s=c.prepareStatement("SELECT product_id,sku_at_purchase,product_name_at_purchase,unit_price,quantity,line_total FROM order_items WHERE order_id=? ORDER BY id")){
            s.setLong(1,id);try(ResultSet rs=s.executeQuery()){while(rs.next())lines.add(new OrderLineView(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getBigDecimal(4),rs.getInt(5),rs.getBigDecimal(6)));}
        }
        return new OrderView(orderId,number,user,status,total,created,cancelled,List.copyOf(lines));
    }
    private Product lockProduct(Connection c,long id)throws SQLException{
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM products WHERE id=? FOR UPDATE")){s.setLong(1,id);try(ResultSet rs=s.executeQuery()){if(!rs.next())throw ApiException.notFound();return ProductService.map(rs);}}
    }
    private void checkOwner(OrderView o,String username,boolean admin){if(!admin&&!o.customerUsername().equals(username))throw ApiException.forbidden();}
}
