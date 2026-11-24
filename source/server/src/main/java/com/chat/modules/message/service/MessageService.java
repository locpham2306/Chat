package com.chat.modules.message.service;

public interface MessageService {
    MessageDelivery send(MessageSendCommand command);
}
