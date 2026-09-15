package com.okerry.okerry_diagram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.okerry.okerry_diagram")
public class OkerryDiagramApplication {

    public static void main(String[] args) {
        SpringApplication.run(OkerryDiagramApplication.class, args);
    }

}
