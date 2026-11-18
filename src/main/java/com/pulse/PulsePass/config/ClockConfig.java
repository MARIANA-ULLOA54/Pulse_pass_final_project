package com.pulse.PulsePass.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** Reloj inyectable: permite fijar la fecha actual en los unit tests. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
