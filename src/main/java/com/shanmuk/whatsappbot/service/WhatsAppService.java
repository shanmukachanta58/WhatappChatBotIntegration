package com.shanmuk.whatsappbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class WhatsAppService {

    @Value("${whatsapp.phone-number-id}")
    private String phoneNumberId;

    @Value("${whatsapp.access-token}")
    private String accessToken;

    @Value("${whatsapp.api-version}")
    private String apiVersion;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendMessage(String recipientPhoneNumber, String message) {

        String url = "https://graph.facebook.com/" + apiVersion + "/" + phoneNumberId + "/messages";

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> text = Map.of("body", message);

        Map<String, Object> requestBody = Map.of("messaging_product", "whatsapp", "to", recipientPhoneNumber, "type", "text", "text", text);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        var response = restTemplate.postForEntity(
                url,
                request,
                String.class
        );

        System.out.println("========== WHATSAPP API RESPONSE ==========");
        System.out.println("Status: " + response.getStatusCode());
        System.out.println("Body: " + response.getBody());
        System.out.println("===========================================");
    }

    public void sendOrderConfirmation(String recipientPhoneNumber) {

        String url = "https://graph.facebook.com/"
                + apiVersion
                + "/"
                + phoneNumberId
                + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> bodyParameters = Map.of(
                "type", "text",
                "text", "John Doe"
        );

        Map<String, Object> orderIdParameter = Map.of(
                "type", "text",
                "text", "123456"
        );

        Map<String, Object> dateParameter = Map.of(
                "type", "text",
                "text", "Sep 29, 2026"
        );

        Map<String, Object> bodyComponent = Map.of(
                "type", "body",
                "parameters", java.util.List.of(
                        bodyParameters,
                        orderIdParameter,
                        dateParameter
                )
        );

        Map<String, Object> template = Map.of(
                "name", "jaspers_market_order_confirmation_v1",
                "language", Map.of(
                        "code", "en_US"
                ),
                "components", java.util.List.of(
                        bodyComponent
                )
        );

        Map<String, Object> requestBody = Map.of(
                "messaging_product", "whatsapp",
                "to", recipientPhoneNumber,
                "type", "template",
                "template", template
        );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(requestBody, headers);

        var response = restTemplate.postForEntity(
                url,
                request,
                String.class
        );

        System.out.println("========== WHATSAPP TEMPLATE RESPONSE ==========");
        System.out.println("Status: " + response.getStatusCode());
        System.out.println("Body: " + response.getBody());
        System.out.println("================================================");
    }
}
