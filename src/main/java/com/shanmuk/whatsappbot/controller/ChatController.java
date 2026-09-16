package com.shanmuk.whatsappbot.controller;

import com.shanmuk.whatsappbot.dto.ChatRequest;
import com.shanmuk.whatsappbot.dto.ChatResponse;
import com.shanmuk.whatsappbot.service.ChatService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService){
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request){

        String reply = chatService.processMessage(
                request.getPhoneNumber(),
                request.getMessage()
        );

        return new ChatResponse(reply);
    }
}
