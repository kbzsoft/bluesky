package com.bluesky.zoom.configclient.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        return new RedisCacheManager(
                RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory),
                this.getRedisCacheConfigurationWithTtl(30*60), // 默认策略，未配置的 key 会使用这个
                this.getRedisCacheConfigurationMap() // 指定 key 策略
        );
    }

    private Map<String, RedisCacheConfiguration> getRedisCacheConfigurationMap() {
        Map<String, RedisCacheConfiguration> redisCacheConfigurationMap = new HashMap<>();
        //SsoCache和BasicDataCache进行过期时间配置
        redisCacheConfigurationMap.put("SsoCache", this.getRedisCacheConfigurationWithTtl(24*60*60));
        redisCacheConfigurationMap.put("BasicDataCache", this.getRedisCacheConfigurationWithTtl(30*60));
        return redisCacheConfigurationMap;
    }

    private RedisCacheConfiguration getRedisCacheConfigurationWithTtl(Integer seconds) {
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig();
        redisCacheConfiguration = redisCacheConfiguration.serializeValuesWith(
                RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(jackson2JsonRedisSerializer)
        ).entryTtl(Duration.ofSeconds(seconds));

        return redisCacheConfiguration;
    }

    /**
     * 缓存的key生成器
     * key有两种生成方式：
     * 1.直接通过key属性使用spel表达式指定
     * 2.通过KeyGenerator这个Bean来实现
     * 当前的效果是，如果两者都不设置，则使用一种默认的策略生成SimpleKeyGenerator
     * 如果只设置其一，则使用设置的方式
     * 如果两者同时设置,就会出错
     */
    @Bean
    public KeyGenerator keyGenerator(){

        /**
         * 最简单的key生成器，当参数只有一个时，只是按照参数拼凑出key，当参数为多个时，会进行拼接
         * SimpleKeyGenerator keyGenerator = new SimpleKeyGenerator();
         */
        /**
         * 自定义key生成器，可以按照调用对象、方法、参数生成key
         */
        KeyGenerator keyGenerator = new KeyGenerator() {
            @Override
            public Object generate(Object target, Method method, Object... params) {
                StringBuilder builder = new StringBuilder();
                builder.append(target.getClass().getName()+":");
                builder.append(method.getName()+":");
                for(Object object:params){
                    builder.append(object+",");
                }
                return builder.toString();
            }
        };
        return keyGenerator;
    }

    /**
     * 配置缓存错误处理器，当获取、设置、清除三种缓存动作出错时，处理错误
     */
    @Bean
    public CacheErrorHandler cacheErrorHandler(){
        CacheErrorHandler handler = new CacheErrorHandler() {

            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                logger.error("获取缓存出现异常");
                logger.error("缓存名称："+cache.getName()+",缓存key："+key+",异常："+exception.getLocalizedMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                logger.error("设置缓存出现异常");
                logger.error("缓存名称："+cache.getName()+",缓存key："+key+",异常："+exception.getLocalizedMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                logger.error("清除缓存出现异常");
                logger.error("缓存名称："+cache.getName()+",缓存key："+key+",异常："+exception.getLocalizedMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                logger.error("清理所有缓存出现异常");
                logger.error("缓存名称："+cache.getName()+",异常："+exception.getLocalizedMessage());
            }
        };
        return handler;
    }

}
