package org.majun.backend.config;

import lombok.RequiredArgsConstructor;
import org.majun.backend.websocket.AfterSaleMessageWebSocketHandler;
import org.majun.backend.websocket.BountyMessageWebSocketHandler;
import org.majun.backend.websocket.CustomerServiceWebSocketHandler;
import org.majun.backend.websocket.PrintProgressWebSocketHandler;
import org.majun.backend.websocket.UsedMessageWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置类
 * 注册打印进度、售后消息、悬赏消息、二手消息、客服消息等 WebSocket 端点
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class PrintWebSocketConfig implements WebSocketConfigurer {

    private final PrintProgressWebSocketHandler printProgressWebSocketHandler;
    private final AfterSaleMessageWebSocketHandler afterSaleMessageWebSocketHandler;
    private final BountyMessageWebSocketHandler bountyMessageWebSocketHandler;
    private final UsedMessageWebSocketHandler usedMessageWebSocketHandler;
    private final CustomerServiceWebSocketHandler customerServiceWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(printProgressWebSocketHandler, "/ws/print/progress").setAllowedOriginPatterns("*");
        registry.addHandler(afterSaleMessageWebSocketHandler, "/ws/after-sale/message").setAllowedOriginPatterns("*");
        registry.addHandler(bountyMessageWebSocketHandler, "/ws/bounty/message").setAllowedOriginPatterns("*");
        registry.addHandler(usedMessageWebSocketHandler, "/ws/used/message").setAllowedOriginPatterns("*");
        registry.addHandler(customerServiceWebSocketHandler, "/ws/cs/message").setAllowedOriginPatterns("*");
    }
}
