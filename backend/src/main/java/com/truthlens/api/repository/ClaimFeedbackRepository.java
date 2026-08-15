package com.truthlens.api.repository;

import com.truthlens.api.model.ClaimFeedback;
import com.truthlens.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimFeedbackRepository extends JpaRepository<ClaimFeedback, Long> {
    List<ClaimFeedback> findByUserOrderByCreatedAtDesc(User user);
    List<ClaimFeedback> findTop50ByOrderByCreatedAtDesc();
}
