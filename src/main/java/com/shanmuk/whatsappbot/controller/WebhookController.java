package com.shanmuk.whatsappbot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanmuk.whatsappbot.service.ChatService;
import com.shanmuk.whatsappbot.service.WhatsAppService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private static final String VERIFY_TOKEN = "whatsapp_bot_verify_2026";

    private final ObjectMapper objectMapper;
    private final ChatService chatService;
    private final WhatsAppService whatsAppService;

    public WebhookController(ObjectMapper objectMapper, ChatService chatService, WhatsAppService whatsAppService) {
        this.objectMapper = objectMapper;
        this.chatService = chatService;
        this.whatsAppService = whatsAppService;
    }

    @GetMapping
    public String verifyWebHook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            return challenge;
        }

        return "Verification failed";
    }

    @GetMapping("/test-send")
    public String testSend() {

        whatsAppService.sendOrderConfirmation(
                "919948049247"
        );

        return "Template message sent";
    }

    @PostMapping
    public String receiveMessage(@RequestBody String payload) {

        try {

            JsonNode root = objectMapper.readTree(payload);

            JsonNode messages = root
                    .path("entry")
                    .path(0)
                    .path("changes")
                    .path(0)
                    .path("value")
                    .path("messages");

            if (messages.isArray() && !messages.isEmpty()) {

                JsonNode messageNode = messages.get(0);

                String phoneNumber = messageNode
                        .path("from")
                        .asText();

                String messageType = messageNode
                        .path("type")
                        .asText();

                if ("text".equals(messageType)) {

                    String messageText = messageNode
                            .path("text")
                            .path("body")
                            .asText();

                    System.out.println("========== WHATSAPP MESSAGE ==========");
                    System.out.println("Phone: " + phoneNumber);
                    System.out.println("Message: " + messageText);
                    System.out.println("======================================");

                    String reply = chatService.processMessage(
                            phoneNumber,
                            messageText
                    );

                    System.out.println("Bot Reply: " + reply);

                    whatsAppService.sendMessage(phoneNumber, reply);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "EVENT_RECEIVED";
    }
}