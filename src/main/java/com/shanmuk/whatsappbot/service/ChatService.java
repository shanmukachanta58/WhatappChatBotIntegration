package com.shanmuk.whatsappbot.service;

import com.shanmuk.whatsappbot.entity.User;
import com.shanmuk.whatsappbot.repository.UserRepository;
import org.springframework.stereotype.Service;
import com.shanmuk.whatsappbot.entity.Conversation;
import com.shanmuk.whatsappbot.repository.ConversationRepository;
import com.shanmuk.whatsappbot.entity.Message;
import com.shanmuk.whatsappbot.repository.MessageRepository;
import com.shanmuk.whatsappbot.entity.ConversationState;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import com.shanmuk.whatsappbot.entity.Booking;
import com.shanmuk.whatsappbot.repository.BookingRepository;

@Service
public class ChatService {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final BookingRepository bookingRepository;

    public ChatService(UserRepository userRepository, ConversationRepository conversationRepository, MessageRepository messageRepository, BookingRepository bookingRepository) {

        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.bookingRepository = bookingRepository;
    }

    public String processMessage(String phoneNumber, String message) {

        if(message == null || message.isBlank()){
            return "Please enter a message";
        }

        User user = userRepository.findByPhoneNumber(phoneNumber).orElseGet(() -> {
            User newUser = new User(phoneNumber, null);
            return userRepository.save(newUser);
        });

        Conversation conversation = conversationRepository.findByUserAndStatus(user, "ACTIVE").orElseGet(() -> {
           Conversation newConversation = new Conversation(user);
           return conversationRepository.save(newConversation);
        });

        Message userMessage = new Message(conversation, "USER", message.trim());

        messageRepository.save(userMessage);

        String normalizedMessage = message.trim().toLowerCase();

        String reply;

        switch (conversation.getCurrentState()) {

            case START:

                if(normalizedMessage.equals("hello") || normalizedMessage.equals("hi") || normalizedMessage.equals("hey")) {

                    reply = "Hello! Welcome to our service. How can I help you?";

                } else if (normalizedMessage.contains("book")) {

                    conversation.setCurrentState(ConversationState.BOOKING);
                    conversation.setUpdatedAt(LocalDateTime.now());

                    reply = "Sure! What service would you like to book?";

                } else if (normalizedMessage.contains("cancel")) {

                    reply = "Sure. I can help you cancel a booking. Please provide your booking details.";

                } else {

                    reply = "I'm sorry, I didn't understand that. You can ask me about bookings or cancellations.";
                }

                break;

            case BOOKING:

                Booking booking = new Booking(conversation, message.trim());

                bookingRepository.save(booking);

                conversation.setCurrentState(ConversationState.SELECT_DATE);
                conversation.setUpdatedAt(LocalDateTime.now());

                reply = "Great! What date would you like to book?";

                break;

            case SELECT_DATE:

                try {

                    LocalDate bookingDate = LocalDate.parse(message.trim());

                    Booking pendingBooking = bookingRepository.findByConversationAndStatus(conversation, "PENDING").orElseThrow();

                    pendingBooking.setBookingDate(bookingDate);

                    bookingRepository.save(pendingBooking);

                    conversation.setCurrentState((ConversationState.SELECT_DATE));
                    conversation.setUpdatedAt(LocalDateTime.now());

                    reply = "Great! What time would you like to book?";

                } catch (DateTimeParseException e) {

                    reply = "Please enter the date in this format: YYYY-MM-DD";

                }

            case SELECT_TIME:

                try {

                    LocalTime bookingTime = LocalTime.parse(message.trim());

                    Booking timeBooking = bookingRepository.findByConversationAndStatus(conversation, "PENDING").orElseThrow();

                    timeBooking.setBookingTime(bookingTime);

                    conversation.setCurrentState(ConversationState.CONFIRMATION);
                    conversation.setUpdatedAt(LocalDateTime.now());

                    reply = "Your booking is ready. Please confirm: " + timeBooking.getService() + " on " + timeBooking.getBookingDate() + " at " + timeBooking.getBookingTime() + ". Reply YES to confirm.";

                } catch (DateTimeParseException e) {
                    reply = "Please enter the time in this format: HH:mm";
                }

                break;

            case CONFIRMATION:

                Booking confirmBooking = bookingRepository.findByConversationAndStatus(conversation, "PENDING").orElseThrow();

                if(normalizedMessage.equals("yes")) {

                    confirmBooking.setStatus("CONFIRMED");

                    bookingRepository.save(confirmBooking);

                    conversation.setCurrentState(ConversationState.COMPLETED);
                    conversation.setUpdatedAt(LocalDateTime.now());

                    reply = "Your booking has been confirmed!";

                } else {

                    reply = "Please reply YES to confirm your booking.";

                }

                break;

            default:

                reply = "I'm sorry, I didn't understand that.";
        }

        conversationRepository.save(conversation);

        Message botMessage = new Message(conversation, "BOT", reply);

        messageRepository.save(botMessage);

        return reply;
    }
}
