package com.chat.integration.websocket.service;

import com.chat.integration.websocket.dto.ReadReceiptData;
import com.chat.integration.websocket.handler.ChatWebSocketHandler;
import com.chat.modules.message.service.MessageReadEvent;
import com.chat.modules.message.dto.response.ConversationReadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// File này gửi READ_RECEIPT lên FE sau khi mốc đọc đã được lưu thành công vào DB.
@Component
@RequiredArgsConstructor
public class ReadReceiptEventListener {
    private final WebSocketSessionRegistry webSocketSessionRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(MessageReadEvent event) {
        ConversationReadResponse response = event.response();
        ReadReceiptData data = new ReadReceiptData(
                response.getConversationId(),
                response.getUserId(),
                response.getLastReadMessageId());
        webSocketSessionRegistry.broadcast(
                event.activeMemberIds(), // gui cho nhung ai
                ChatWebSocketHandler.READ_RECEIPT, // ten event
                data); // user da doc den dau
    }
}
