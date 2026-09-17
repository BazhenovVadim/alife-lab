package com.vadim.alife;

import com.vadim.alife.ui.EcosystemFxApplication;
import javafx.application.Application;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AlifeApplication {

    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        context = new SpringApplicationBuilder(AlifeApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
        Application.launch(EcosystemFxApplication.class, args);
    }

    public static <T> T getBean(Class<T> type) {
        return context.getBean(type);
    }

    public static void closeContext() {
        if (context != null) {
            context.close();
        }
    }
}
