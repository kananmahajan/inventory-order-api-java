package com.kanan.inventory;

import com.kanan.inventory.model.CreateOrderInput;
import com.kanan.inventory.model.OrderLineInput;
import com.kanan.inventory.model.ProductInput;
import com.kanan.inventory.model.Validation;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ValidationTest {
    @Test void trimsProductText(){
        var p=Validation.product(new ProductInput(" SKU-1 "," Coffee "," Grocery ",new BigDecimal("4.50"),2));
        assertEquals("SKU-1",p.sku());assertEquals("Coffee",p.name());
    }
    @Test void rejectsZeroPriceAndNegativeInventory(){
        assertThrows(RuntimeException.class,()->Validation.product(new ProductInput("A","A","A",new BigDecimal("0.00"),0)));
        assertThrows(RuntimeException.class,()->Validation.product(new ProductInput("A","A","A",new BigDecimal("1.00"),-1)));
    }
    @Test void rejectsDuplicateOrderLines(){
        var order=new CreateOrderInput(List.of(new OrderLineInput(1L,1),new OrderLineInput(1L,2)));
        assertThrows(RuntimeException.class,()->Validation.order(order));
    }
}
