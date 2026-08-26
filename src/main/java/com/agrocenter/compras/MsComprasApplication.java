package com.agrocenter.compras;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MsComprasApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsComprasApplication.class, args);
    }

}