package com.miguel.app.system.service.interfaces;

public interface TokenRevocationService {
    void revoke(String token);
    boolean isRevoked(String token);
    void deleteExpiredTokens();
}
