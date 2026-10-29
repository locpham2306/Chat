package com.chat.modules.websocket.service;

import com.chat.modules.message.service.MessageReadEvent;
import com.chat.modules.websocket.dto.ReadReceiptData;
import com.chat.modules.websocket.handler.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ReadReceiptEventListener {
    private final WebSocketSessionRegistry webSocketSessions;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(MessageReadEvent event) {
        var response = event.response();
        webSocketSessions.broadcast(
                event.activeMemberIds(),
                ChatWebSocketHandler.READ_RECEIPT,
                new ReadReceiptData(
                        response.conversationId(), response.userId(), response.lastReadMessageId()));
    }
}
