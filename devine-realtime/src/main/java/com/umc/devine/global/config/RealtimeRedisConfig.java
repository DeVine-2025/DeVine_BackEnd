package com.umc.devine.global.config;

import com.umc.devine.infrastructure.chat.pubsub.ChatEventSubscriber;
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

    @Value("${redis.channel.chat-pattern}")
    private String chatChannelPattern;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            SseEventSubscriber sseEventSubscriber,
            ChatEventSubscriber chatEventSubscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(sseEventSubscriber, new PatternTopic(notificationChannelPattern));
        log.info("Redis Pub/Sub 리스너 등록 - pattern: {}", notificationChannelPattern);

        container.addMessageListener(chatEventSubscriber, new PatternTopic(chatChannelPattern));
        log.info("Redis Pub/Sub 리스너 등록 - pattern: {}", chatChannelPattern);

        return container;
    }
}
