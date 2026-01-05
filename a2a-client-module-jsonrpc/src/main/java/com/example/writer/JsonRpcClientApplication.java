package com.example.writer;

import com.example.writer.service.A2ADemoService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class JsonRpcClientApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(JsonRpcClientApplication.class, args);
        //调用 A2ADemoService
        A2ADemoService a2aDemoService =  context.getBean(A2ADemoService.class);
        a2aDemoService.a2aDemo();
        System.exit(SpringApplication.exit(context, () -> 0));
    }
}

