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
import java.util.Optional;

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

                    conversation.setCurrentState(ConversationState.CANCELLATION);
                    conversation.setUpdatedAt(LocalDateTime.now());

                    reply = "Sure. Let me check your booking.";
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

            case CANCELLATION:

                Optional<Booking> cancellationBooking = bookingRepository.findByConversationAndStatus(conversation, "CONFIRMED");

                if(cancellationBooking.isEmpty()) {
                    reply = "You don't have any active booking to cancel.";

                    conversation.setCurrentState(ConversationState.START);
                    conversation.setUpdatedAt(LocalDateTime.now());
                } else {

                    Booking bookingToCancel = cancellationBooking.get();

                    conversation.setCurrentState(ConversationState.CANCELLATION_CONFIRMATION);
                    conversation.setUpdatedAt((LocalDateTime.now()));

                    reply = "I found your booking for " + bookingToCancel.getService() + " on " + bookingToCancel.getBookingDate() + " at " + bookingToCancel.getBookingTime() + ". Do you want to cancel it? Reply YES or NO.";

                    break;
                }

            case CANCELLATION_CONFIRMATION:

                Optional<Booking> cancellationConfirmationBooking = bookingRepository.findByConversationAndStatus(conversation, "CONFIRMED");

                if(cancellationConfirmationBooking.isEmpty()) {

                    reply = "I couldn't find an active booking to cancel.";

                    conversation.setCurrentState(ConversationState.START);
                    conversation.setUpdatedAt(LocalDateTime.now());

                    break;
                }

                Booking bookingToCancel = cancellationConfirmationBooking.get();

                if(normalizedMessage.equals("yes")) {

                    bookingToCancel.setStatus("CANCELLED");

                    bookingRepository.save(bookingToCancel);

                    conversation.setCurrentState(ConversationState.START);
                    conversation.setUpdatedAt(LocalDateTime.now());

                    reply = "Your booking has been cancelled successfully.";

                } else if(normalizedMessage.equals("no")) {

                    conversation.setCurrentState(ConversationState.COMPLETED);
                    conversation.setUpdatedAt(LocalDateTime.now());

                    reply = "Okay. Your booking has not been cancelled";

                } else {

                    reply = "Please reply YES to cancel your booking or NO to keep it.";
                }

                break;

            case SELECT_DATE:

                try {

                    LocalDate bookingDate = LocalDate.parse(message.trim());

                    if(bookingDate.isBefore(LocalDate.now())) {
                        reply = "You cannot book a date in the past. Please choose a future date.";
                        break;
                    }

                    Booking pendingBooking = bookingRepository.findByConversationAndStatus(conversation, "PENDING").orElseThrow();

                    pendingBooking.setBookingDate(bookingDate);

                    bookingRepository.save(pendingBooking);

                    conversation.setCurrentState((ConversationState.SELECT_TIME));
                    conversation.setUpdatedAt(LocalDateTime.now());

                    reply = "Great! What time would you like to book?";

                } catch (DateTimeParseException e) {

                    reply = "Please enter the date in this format: YYYY-MM-DD";

                    break;
                }

            case SELECT_TIME:

                try {

                    LocalTime bookingTime = LocalTime.parse(message.trim());

                    Booking timeBooking = bookingRepository.findByConversationAndStatus(conversation, "PENDING").orElseThrow();

                    if(timeBooking.getBookingDate().isEqual(LocalDate.now()) && bookingTime.isBefore(LocalTime.now().withSecond(0).withNano(0))) {

                        reply = "You cannot book a time that has already passed. Please choose another time.";
                        break;
                    }

                    timeBooking.setBookingTime(bookingTime);

                    boolean slotTaken = bookingRepository.existsByBookingDateAndBookingTimeAndStatus(timeBooking.getBookingDate(), bookingTime, "CONFIRMED");

                    if(slotTaken) {
                        reply = "Sorry, that time is already booked. Please choose another time.";
                        break;
                    }
                    bookingRepository.save(timeBooking);

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

                    conversation.setCurrentState(ConversationState.START);
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
