package com.gallop.wechat.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.gallop.wechat.service.RedisReceiver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


@Slf4j
@Configuration
@EnableAutoConfiguration
@EnableCaching
public class RedisConfig extends CachingConfigurerSupport {
    public static final String AUTH_CODE_TOPIC = "authCode-queue";

    @Autowired
    private RedisProperties redisProperties;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory lettuceConnectionFactory) {

        //创建Redis缓存操作助手RedisTemplate对象
        RedisTemplate<String, Object> template = new RedisTemplate();
        template.setConnectionFactory(lettuceConnectionFactory);
        //以下代码为将RedisTemplate的Value序列化方式由JdkSerializationRedisSerializer更换为Jackson2JsonRedisSerializer
        //此种序列化方式结果清晰、容易阅读、存储字节少、速度快，所以推荐更换

        /*Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);*/

        //template.setKeySerializer(keyPrefixRedisSerializer);
        //template.setHashKeySerializer(keyPrefixRedisSerializer);
        //template.setValueSerializer(jackson2JsonRedisSerializer);
        RedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);//key序列化
        template.setValueSerializer(valueSerializer());//value序列化
        template.setHashKeySerializer(stringSerializer);//Hash key序列化
        template.setHashValueSerializer(valueSerializer());//Hash value序列化

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 值采用JSON序列化
     * @return
     */
    private RedisSerializer<Object> valueSerializer() {
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        //当实体类存于redis中的json 含有非set 字段值，反系列化会抛出Unrecognized field 的错误，比如SysUser类的admin字段，需要做如下配置：
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        //LocalDate、LocalDateTime序列化器
        JavaTimeModule timeModule = new JavaTimeModule();
        timeModule.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        timeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        timeModule.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        timeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.registerModule(timeModule);

        jackson2JsonRedisSerializer.setObjectMapper(om);
        return jackson2JsonRedisSerializer;
    }

    /**
     * date @2021-06-02
     * Description: 获取缓存连接
     * return:
     **/
    @Bean(value = "lettuceConnectionFactory")
    public LettuceConnectionFactory getConnectionFactory(GenericObjectPoolConfig genericPoolConfig) {
        //单机模式
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(redisProperties.getHost());
        configuration.setPort(redisProperties.getPort());
        configuration.setDatabase(redisProperties.getDatabase());
        configuration.setPassword(RedisPassword.of(redisProperties.getPassword()));

        LettuceConnectionFactory factory = new LettuceConnectionFactory(configuration, getPoolConfig(genericPoolConfig));
        //factory.setShareNativeConnection(false);//是否允许多个线程操作共用同一个缓存连接，默认true，false时每个操作都将开辟新的连接
        return factory;
    }

    @Bean
    @Scope(value = "prototype")
    public GenericObjectPoolConfig genericPoolConfig() {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(redisProperties.getLettuce().getPool().getMaxActive());
        config.setMaxWaitMillis(redisProperties.getLettuce().getPool().getMaxWait().toMillis());
        config.setMaxIdle(redisProperties.getLettuce().getPool().getMaxIdle());
        config.setMinIdle(redisProperties.getLettuce().getPool().getMinIdle());
        return config;
    }

    /**
     * 获取缓存连接池
     **/
    @Bean
    public LettucePoolingClientConfiguration getPoolConfig(GenericObjectPoolConfig genericPoolConfig) {

        LettucePoolingClientConfiguration pool = LettucePoolingClientConfiguration.builder()
                .poolConfig(genericPoolConfig)
                .commandTimeout(redisProperties.getTimeout())
                .shutdownTimeout(redisProperties.getLettuce().getShutdownTimeout())
                .build();

        return pool;
    }

    @Override
    public KeyGenerator keyGenerator() {
        return new KeyGenerator() {
            @Override
            public Object generate(Object target, Method method, Object... params) {
                StringBuilder sb = new StringBuilder();
                sb.append(target.getClass().getName());
                sb.append(method.getName());
                for (Object obj : params) {
                    sb.append(obj.toString());
                }
                return sb.toString();
            }
        };
    }

    @Override
    public CacheErrorHandler errorHandler() {
        // 异常处理，当Redis发生异常时，打印日志，但是程序正常走
        log.info("初始化 -> [{}]", "Redis CacheErrorHandler");
        CacheErrorHandler cacheErrorHandler = new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.error("Redis occur handleCacheGetError：key -> [{}]", key, e);
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.error("Redis occur handleCachePutError：key -> [{}]；value -> [{}]", key, value, e);
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key)    {
                log.error("Redis occur handleCacheEvictError：key -> [{}]", key, e);
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.error("Redis occur handleCacheClearError：", e);
            }
        };
        return cacheErrorHandler;
    }

    @Bean(value = "cacheManager")
    public CacheManager cacheManager(LettuceConnectionFactory lettuceConnectionFactory) {
        RedisSerializer stringSerializer = new StringRedisSerializer();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig() // 生成一个默认配置，通过config对象即可对缓存进行自定义配置
                // 设置key为String
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                // 设置value 为自动转Json的Object
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer()))
                // 不缓存null
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(120));     // 设置缓存的默认过期时间，也是使用Duration设置


        // 设置一个初始化的缓存空间set集合
        Set<String> cacheNames = new HashSet<>();
        cacheNames.add("third-company");
        cacheNames.add("corp-access-token");

        // 对每个缓存空间应用不同的配置,可设置不同的过期时间
        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
        configMap.put("third-company", config.entryTtl(Duration.ofHours(8)));
        configMap.put("corp-access-token", config.entryTtl(Duration.ofMinutes(115)));//115分钟

        RedisCacheManager cacheManager = RedisCacheManager.builder(lettuceConnectionFactory)     // 使用自定义的缓存配置初始化一个cacheManager
                .initialCacheNames(cacheNames)  // 注意这两句的调用顺序，一定要先调用该方法设置初始化的缓存名，再初始化相关的配置
                .withInitialCacheConfigurations(configMap)
                .build();
        return cacheManager;
    }

   /*@Bean
   public CacheManager cacheManager(LettuceConnectionFactory lettuceConnectionFactory) {
       RedisCacheConfiguration config =
               RedisCacheConfiguration.defaultCacheConfig()
                       //设置缓存有效时间(1小时)
                       .entryTtl(Duration.ofHours(12))
                       //不缓存null结果，若出现null结果时会报异常
                       .disableCachingNullValues()
                       //以json形式序列化对象
                       .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer()));


       return RedisCacheManager.builder(lettuceConnectionFactory).cacheDefaults(config).build();
   }*/

    /**
     * date @2022-03-31
     * Description: redis消息订阅发布的相关配置
     **/
    @Bean
    RedisMessageListenerContainer container(LettuceConnectionFactory lettuceConnectionFactory,
                                            MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(lettuceConnectionFactory);

        //可以添加多个 messageListener
        //可以对 messageListener对应的适配器listenerAdapter  指定本适配器 适配的消息类型  是什么
        //在发布的地方 对应发布的redisTemplate.convertAndSend("user-queue",msg);  那这边的就对应的可以消费到指定类型的 订阅消息
        container.addMessageListener(listenerAdapter, new PatternTopic(AUTH_CODE_TOPIC));
        return container;
    }

    @Bean
    @Autowired
    MessageListenerAdapter listenerAdapter(RedisReceiver redisReceiver) {
        return new MessageListenerAdapter(redisReceiver, "receiveMessage");
    }
}
