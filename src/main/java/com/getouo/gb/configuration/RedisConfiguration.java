package com.getouo.gb.configuration;

import com.getouo.gb.scl.io.codec.ScalaKryoBinaryRedisSerializer;
import com.twitter.chill.ScalaKryoInstantiator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class RedisConfiguration {

    @Bean
    RedisTemplate redisTemplate(LettuceConnectionFactory redisConnectionFactory) {

        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //不使用默认的序列化
        redisTemplate.setEnableDefaultSerializer(false);
        //使用StringRedisSerializer来序列化和反序列化redis的key值
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        ScalaKryoBinaryRedisSerializer serializer = new ScalaKryoBinaryRedisSerializer(ScalaKryoInstantiator.defaultPool());
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

//    /**
//     * 配置自定义redisTemplate
//     * 这个名字不能改, bean依赖这个name
//     * @return
//     */
//    @Bean
//    <T> RedisTemplate<String, T> protoredisTemplate(RedisConnectionFactory redisConnectionFactory, ObjectMapper mapper) {
//
//        RedisTemplate<String, T> template = new RedisTemplate<>();
//        template.setConnectionFactory(redisConnectionFactory);
//
//        //使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值
//        Jackson2JsonRedisSerializer serializer = new Jackson2JsonRedisSerializer(Object.class);
////        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);
//
////        ObjectMapper mapper = new ObjectMapper();
////        mapper.registerModules(DefaultScalaModule$.MODULE$);
//        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
////        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
//        serializer.setObjectMapper(mapper);
//
//
////        template.setValueSerializer(serializer);
//
////        template.setEnableDefaultSerializer(false);
//        template.setValueSerializer(new ProtobufBinaryRedisSerializer<MessageLite>());
//
//        //使用StringRedisSerializer来序列化和反序列化redis的key值
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setHashKeySerializer(new StringRedisSerializer());
//        template.setHashValueSerializer(serializer);
//        template.afterPropertiesSet();
//        return template;
//    }
}
