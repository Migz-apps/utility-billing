package com.miguel.app.system.repository;

import com.miguel.app.system.entity.PasswordResetOtp;
import com.miguel.app.system.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    List<PasswordResetOtp> findAllByUserAndUsedFalse(User user);

    Optional<PasswordResetOtp> findFirstByUserAndUsedFalseOrderByCreatedAtDesc(User user);

    long deleteByExpiresAtBefore(LocalDateTime cutoff);
}
