package com.shanmuk.whatsappbot.repository;

import com.shanmuk.whatsappbot.entity.Conversation;
import com.shanmuk.whatsappbot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByUserAndStatus(User user, String status);
}

