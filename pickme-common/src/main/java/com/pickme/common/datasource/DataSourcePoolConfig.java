package com.pickme.common.datasource;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "pickme.datasource.pool")
public class DataSourcePoolConfig {

    private Map<String, ModulePoolConfig> modules = new HashMap<>();

    @Getter
    @Setter
    public static class ModulePoolConfig {
        private int maxPoolSize = 5;
        private int minIdle = 2;
        private long connectionTimeout = 3000;
    }

    public ModulePoolConfig getModuleConfig(String moduleName) {
        return modules.getOrDefault(moduleName, defaultConfig());
    }

    private ModulePoolConfig defaultConfig() {
        ModulePoolConfig config = new ModulePoolConfig();
        config.setMaxPoolSize(5);
        config.setMinIdle(2);
        config.setConnectionTimeout(3000);
        return config;
    }
}
