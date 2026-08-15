package com.truthlens.api.repository;

import com.truthlens.api.model.FactCheckHistory;
import com.truthlens.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FactCheckHistoryRepository extends JpaRepository<FactCheckHistory, Long> {
    List<FactCheckHistory> findByUserOrderByCreatedAtDesc(User user);
    List<FactCheckHistory> findTop10ByOrderByCreatedAtDesc();
}
