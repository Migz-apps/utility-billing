package com.miguel.app.system.repository;

import com.miguel.app.system.entity.EmailVerificationOtp;
import com.miguel.app.system.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationOtpRepository extends JpaRepository<EmailVerificationOtp, Long> {

    List<EmailVerificationOtp> findAllByUserAndUsedFalse(User user);

    Optional<EmailVerificationOtp> findFirstByUserAndUsedFalseOrderByCreatedAtDesc(User user);

    long deleteByExpiresAtBefore(LocalDateTime cutoff);
}
