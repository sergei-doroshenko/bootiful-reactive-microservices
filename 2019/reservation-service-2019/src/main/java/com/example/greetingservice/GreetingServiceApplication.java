package com.example.greetingservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@SpringBootApplication
public class GreetingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreetingServiceApplication.class, args);
    }
}

@Configuration
class WebsocketConfigurationM {

    @Bean
    WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

    @Bean
    WebSocketHandler webSocketHandler(TimedGreetingsProducerM tgp) {
        return webSocketSession -> webSocketSession.send(tgp.greet().map(webSocketSession::textMessage));
    }

    @Bean
    SimpleUrlHandlerMapping simpleUrlHandlerMapping(WebSocketHandler wsh) {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/greetings", wsh);
        return new SimpleUrlHandlerMapping() {
            {
                setOrder(10);
                setUrlMap(map);
            }
        };
    }
}

@Component
class TimedGreetingsProducerM {

    Flux<String> greet() {
        return this.greet("World");
    }

    Flux<String> greet(String name) {
        return Flux
                .fromStream(Stream.generate(() -> "Hello " + name + " @ " + Instant.now()))
                .delayElements(Duration.ofSeconds(1))
                .log();
    }
}


@Controller
class RSocketGreetingServiceM {

    private final TimedGreetingsProducerM timedGreetingsProducer;

    RSocketGreetingServiceM(TimedGreetingsProducerM timedGreetingsProducer) {
        this.timedGreetingsProducer = timedGreetingsProducer;
    }

    @MessageMapping("greetings")
    Flux<GreetingsResponseM> greet(GreetingsRequestM name) {
        return this.timedGreetingsProducer
                .greet(name.getName())
                .map(GreetingsResponseM::new);
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingsRequestM {
    private String name;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingsResponseM {
    private String greeting;
}