package nnu.mnr.satellite.config.log;

import jakarta.annotation.PostConstruct;
import nnu.mnr.satellite.utils.log.LogCaptureStream;
import org.springframework.context.annotation.Configuration;

import java.io.PrintStream;

@Configuration
public class LogRedirectConfig {
    @PostConstruct
    public void init() {
        System.setOut(new PrintStream(new LogCaptureStream(), true));
    }
}
