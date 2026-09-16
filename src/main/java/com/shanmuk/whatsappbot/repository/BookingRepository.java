package com.shanmuk.whatsappbot.repository;

import com.shanmuk.whatsappbot.entity.Booking;
import com.shanmuk.whatsappbot.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByConversationAndStatus(Conversation conversation, String status);

    boolean existsByBookingDateAndBookingTimeAndStatus( LocalDate bookingDate, LocalTime bookingTime, String status );
}
