package com.docuvra.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .load();
    }

    @Bean
    static BeanFactoryPostProcessor entityManagerFactoryDependsOnFlyway() {
        return beanFactory -> {
            if (!beanFactory.containsBeanDefinition("entityManagerFactory")) {
                return;
            }

            BeanDefinition definition = beanFactory.getBeanDefinition("entityManagerFactory");
            Set<String> dependencies = new LinkedHashSet<>();
            String[] existingDependencies = definition.getDependsOn();
            if (existingDependencies != null) {
                dependencies.addAll(Set.of(existingDependencies));
            }
            dependencies.add("flyway");
            definition.setDependsOn(dependencies.toArray(String[]::new));
        };
    }
}
