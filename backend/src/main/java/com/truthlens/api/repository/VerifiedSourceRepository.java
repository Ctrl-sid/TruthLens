package com.truthlens.api.repository;

import com.truthlens.api.model.VerifiedSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerifiedSourceRepository extends JpaRepository<VerifiedSource, Long> {
    Optional<VerifiedSource> findByDomain(String domain);
}
