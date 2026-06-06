package com.miguel.app.system.service.impl;

import com.miguel.app.system.entity.RevokedToken;
import com.miguel.app.system.exception.UnauthorizedException;
import com.miguel.app.system.repository.RevokedTokenRepository;
import com.miguel.app.system.security.JwtService;
import com.miguel.app.system.service.interfaces.TokenRevocationService;
import java.time.ZoneId;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenRevocationServiceImpl implements TokenRevocationService {

    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtService jwtService;

    @Override
    @Transactional
    public void revoke(String token) {
        if (revokedTokenRepository.existsByToken(token)) {
            return;
        }

        try {
            RevokedToken revokedToken = new RevokedToken();
            revokedToken.setToken(token);
            revokedToken.setExpiresAt(
                    jwtService.extractExpiration(token).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            );
            revokedTokenRepository.save(revokedToken);
        } catch (Exception ex) {
            throw new UnauthorizedException("Invalid or expired token");
        }
    }

    @Override
    public boolean isRevoked(String token) {
        return revokedTokenRepository.existsByToken(token);
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    public void deleteExpiredTokens() {
        revokedTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
