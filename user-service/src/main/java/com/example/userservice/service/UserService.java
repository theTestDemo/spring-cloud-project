package com.example.userservice.service;

import com.example.userservice.dto.OrderDTO;
import com.example.userservice.entity.User;
import org.springframework.data.domain.jaxb.SpringDataJaxb;

import java.util.List;


public interface UserService {
    User getUserById(Long id);
    User getUserByUsername(String username);
    User register(User user);
    String login(String username, String password);
    List<OrderDTO> getOrders(Long userId);
}
