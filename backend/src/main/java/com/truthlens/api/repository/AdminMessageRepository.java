package com.truthlens.api.repository;

import com.truthlens.api.model.AdminMessage;
import com.truthlens.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminMessageRepository extends JpaRepository<AdminMessage, Long> {
    List<AdminMessage> findByReceiverOrderByCreatedAtDesc(User receiver);
    List<AdminMessage> findBySenderOrReceiverOrderByCreatedAtDesc(User sender, User receiver);
    List<AdminMessage> findTop50ByOrderByCreatedAtDesc();
    long countByReceiverAndIsReadFalse(User receiver);
}
