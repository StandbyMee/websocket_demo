package com.practice.websocket_demo.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
//节点配置类

@Configuration
//Configuration是一个类级别的注解，指明对象时bean定义的来源
public class WebSocketStompConfig {
    @Bean
    //这个bean的注册，用于扫描带有@ServerEndpoint的注解成为websocket
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
