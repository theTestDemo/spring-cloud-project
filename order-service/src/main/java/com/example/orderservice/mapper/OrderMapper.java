package com.example.orderservice.mapper;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

@Mapper
public interface OrderMapper {
    @Select("SELECT * FROM orders WHERE user_id = #{userId}")
    List<Order> findByUserId(Long userId);

    @Select("SELECT * FROM orders WHERE id = #{id}")
    Order findById(Long id);

    Integer addOrder(@Param("userId") Long userId,
                     @Param("orderNo") String orderNo,
                     @Param("totalAmount") BigDecimal totalAmount,
                     @Param("status") Integer status);

    Integer addShoppingCart(@Param("userId") Long userId,
                            @Param("productId") Long productId,
                            @Param("quantity") Long quantity);

    List<ShoppingCart> shoppingCartInfo(@Param("userId") Long userId);

    Integer addOrderItem(@Param("orderNo") String orderNo,
                         @Param("productId") Long productId,
                         @Param("quantity") Integer quantity,
                         @Param("unitPrice") BigDecimal unitPrice);
    Integer deleteShoppingCart(@Param("userId") Long userId);
    Integer batchAddOrderItem(@Param("items") List<OrderItem> items);
    Order orderInfoByOrderNoUserId(@Param("userId")Long userId,
                                   @Param("orderNo") String orderNo);
    Integer updateOrderStatusToPaid(@Param("userId")Long userId,
                                    @Param("orderNo") String orderNo);
    Order orderInfoByOrderNo(@Param("orderNo") String orderNo);
    Integer updateOrderStatusToCancel(@Param("orderNo") String orderNo);
    List<OrderItem> findOrderItemsByOrderNo(@Param("orderNo") String orderNo);
    Integer addShoppingCartQuantity(@Param("userId")Long userId,
                                    @Param("productId")Long productId,
                                    @Param("quantity")Long quantity);
}
