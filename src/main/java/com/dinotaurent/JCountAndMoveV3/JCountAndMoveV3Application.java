package com.dinotaurent.JCountAndMoveV3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author dandazme
 */
@Configuration
@EnableScheduling
@SpringBootApplication
public class JCountAndMoveV3Application {
    public static void main(String[] args) {
        SpringApplication.run(JCountAndMoveV3Application.class, args);
        System.out.println("Se inicia el servicio en la clase main");
    }

}
