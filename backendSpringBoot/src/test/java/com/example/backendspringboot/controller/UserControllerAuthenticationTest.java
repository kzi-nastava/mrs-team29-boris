package com.example.backendspringboot.controller;

import com.example.backendspringboot.model.Driver;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserControllerAuthenticationTest {

    @Test
    void extractsEmailFromAuthenticatedDriverPrincipal() {
        Driver driver = new Driver();
        driver.setEmail("driver@example.com");
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(driver, null, List.of());

        assertEquals("driver@example.com",
                UserController.authenticatedEmail(authentication));
    }
}
