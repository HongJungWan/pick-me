package com.pickme.common.datasource;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "pickme.datasource.routing.enabled", havingValue = "true")
public class DataSourceRoutingConfig {

    @Value("${spring.datasource.url}")
    private String primaryUrl;

    @Value("${pickme.datasource.replica.url:${spring.datasource.url}}")
    private String replicaUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource() {
        RoutingDataSource routingDataSource = new RoutingDataSource();

        DataSource primaryDs = createDataSource(primaryUrl, "primary-pool", 20);
        DataSource replicaDs = createDataSource(replicaUrl, "replica-pool", 20);

        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put("primary", primaryDs);
        dataSources.put("replica", replicaDs);

        routingDataSource.setTargetDataSources(dataSources);
        routingDataSource.setDefaultTargetDataSource(primaryDs);

        return new LazyConnectionDataSourceProxy(routingDataSource);
    }

    private DataSource createDataSource(String url, String poolName, int maxPoolSize) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setPoolName(poolName);
        ds.setMaximumPoolSize(maxPoolSize);
        ds.setMinimumIdle(5);
        ds.setConnectionTimeout(3000);
        return ds;
    }
}
