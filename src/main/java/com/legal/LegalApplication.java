package com.legal;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.legal.**.mapper")
public class LegalApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegalApplication.class, args);
    }

}
