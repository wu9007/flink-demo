package org.sword;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.Objects;

/**
 * @author sword
 */
@SpringBootApplication
public class LogProducerApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(LogProducerApplication.class)
                .beanNameGenerator(((definition, registry) -> Objects.requireNonNull(definition.getBeanClassName())))
                .run(args);
    }
}
