package com.konkerlabs.platform.registry.config;

import java.net.UnknownHostException;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.konkerlabs.platform.registry.business.model.converters.InstantReadConverter;
import com.konkerlabs.platform.registry.business.model.converters.InstantWriteConverter;
import com.konkerlabs.platform.registry.business.model.converters.URIReadConverter;
import com.konkerlabs.platform.registry.business.model.converters.URIWriteConverter;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.util.StringUtils;

import java.util.*;

@Configuration
@EnableMongoRepositories(basePackages = "com.konkerlabs.platform.registry.business.repositories")
@Data
public class MongoConfig extends AbstractMongoConfiguration {

    private List<ServerAddress> hostname = new ArrayList<>();
    private Integer port;
    private String username;
    private String password;
    private static Logger LOG = LoggerFactory.getLogger(MongoConfig.class);

    public MongoConfig() {
        Map<String, Object> defaultMap = new HashMap<>();
        defaultMap.put("mongo.hostname", "localhost");
        defaultMap.put("mongo.port", 27017);
        defaultMap.put("mongo.username", "");
        defaultMap.put("mongo.password", "");
        Config defaultConf = ConfigFactory.parseMap(defaultMap);

        Config config = ConfigFactory.load().withFallback(defaultConf);
        setPort(config.getInt("mongo.port"));
        setUsername(Optional.ofNullable(config.getString("mongo.username")).isPresent()
                ? config.getString("mongo.username") : null);
        setPassword(Optional.ofNullable(config.getString("mongo.password")).isPresent()
                ? config.getString("mongo.password") : null);

        List<String> seedList = Optional.ofNullable(config.getString("mongo.hostname")).isPresent() ?
                Arrays.asList(config.getString("mongo.hostname").split(",")) : null;

        for (String seed : seedList) {
            try {
                hostname.add(new ServerAddress(seed, port));
            } catch (Exception e) {
                LOG.error("Error constructing mongo factory", e);
            }
        }

    }

    public static final List<Converter<?, ?>> converters =
            Arrays.asList(getMongoConverter());

    public static final Converter[] getMongoConverter(){
        return new Converter[]{
                new InstantReadConverter(),
                new InstantWriteConverter(),
                new URIReadConverter(),
                new URIWriteConverter()
        };
    }

    @Override
    protected String getDatabaseName() {
        return "registry";
    }


    @Bean(name = "mongoTemplate")
    public MongoTemplate mongoTemplate() throws Exception {
        return new MongoTemplate(this.mongo(), this.getDatabaseName());
    }

    @Override
    public Mongo mongo() throws Exception {
        if (!StringUtils.isEmpty(getUsername()) && !StringUtils.isEmpty(getPassword())) {
            try {
                MongoCredential credential = MongoCredential.createCredential(getUsername(), getDatabaseName(), getPassword().toCharArray());
                return new MongoClient(hostname, Collections.singletonList(credential));
            } catch (Exception e) {
                return new MongoClient(hostname);
            }
        } else {
            return new MongoClient(hostname);
        }

    }

    @Override
    public CustomConversions customConversions() {
        return new CustomConversions(this.converters);
    }

}