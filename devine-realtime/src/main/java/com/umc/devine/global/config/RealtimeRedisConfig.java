package com.umc.devine.global.config;

import com.umc.devine.infrastructure.sse.pubsub.SseEventSubscriber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@Slf4j
public class RealtimeRedisConfig {

    @Value("${redis.channel.notification-pattern}")
    private String notificationChannelPattern;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            SseEventSubscriber sseEventSubscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(sseEventSubscriber, new PatternTopic(notificationChannelPattern));

        log.info("Redis Pub/Sub 리스너 등록 - pattern: {}", notificationChannelPattern);
        return container;
    }
}
