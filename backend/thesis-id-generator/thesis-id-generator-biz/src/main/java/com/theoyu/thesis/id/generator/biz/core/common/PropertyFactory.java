package com.theoyu.thesis.id.generator.biz.core.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyFactory {
    private static final Logger logger = LoggerFactory.getLogger(PropertyFactory.class);
    private static final Properties prop = new Properties();

    static {
        try (InputStream inputStream = PropertyFactory.class.getClassLoader().getResourceAsStream("leaf.properties")) {
            if (inputStream != null) {
                prop.load(inputStream);
            }
        } catch (IOException e) {
            logger.warn("Load Properties Ex", e);
        }
        override("leaf.name", "LEAF_NAME");
        override("leaf.segment.enable", "LEAF_SEGMENT_ENABLE");
        override("leaf.jdbc.url", "LEAF_JDBC_URL");
        override("leaf.jdbc.username", "LEAF_JDBC_USERNAME");
        override("leaf.jdbc.password", "LEAF_JDBC_PASSWORD");
        override("leaf.jdbc.initialSize", "LEAF_JDBC_INITIAL_SIZE");
        override("leaf.jdbc.minIdle", "LEAF_JDBC_MIN_IDLE");
        override("leaf.jdbc.maxActive", "LEAF_JDBC_MAX_ACTIVE");
        override("leaf.jdbc.maxWait", "LEAF_JDBC_MAX_WAIT");
        override("leaf.snowflake.enable", "LEAF_SNOWFLAKE_ENABLE");
        override("leaf.snowflake.zk.address", "LEAF_SNOWFLAKE_ZK_ADDRESS");
        override("leaf.snowflake.port", "LEAF_SNOWFLAKE_PORT");
    }

    private static void override(String propertyKey, String envKey) {
        String value = System.getProperty(propertyKey);
        if (value == null || value.isBlank()) {
            value = System.getenv(envKey);
        }
        if (value != null && !value.isBlank()) {
            prop.setProperty(propertyKey, value);
        }
    }

    public static Properties getProperties() {
        return prop;
    }
}
