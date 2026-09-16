package com.shanmuk.whatsappbot.repository;

import com.shanmuk.whatsappbot.entity.Conversation;
import com.shanmuk.whatsappbot.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long>{

    List<Message> findByConversationOrderByTimestampAsc(Conversation conversation);
}
