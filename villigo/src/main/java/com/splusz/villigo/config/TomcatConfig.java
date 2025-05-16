package com.splusz.villigo.config;

import org.apache.catalina.valves.RemoteIpValve;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return (factory) -> {
            RemoteIpValve valve = new RemoteIpValve();
            valve.setProtocolHeader("X-Forwarded-Proto");
            valve.setProtocolHeaderHttpsValue("https");
            valve.setPortHeader("X-Forwarded-Port"); // 선택사항
            valve.setRemoteIpHeader("X-Forwarded-For"); // 선택사항
            factory.addEngineValves(valve);
        };
    }
}
