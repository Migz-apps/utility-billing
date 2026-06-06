package com.miguel.app.system.repository;

import com.miguel.app.system.entity.User;
import com.miguel.app.system.enums.Role;
import com.miguel.app.system.enums.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByRoleIn(List<Role> roles);
    List<User> findAllByRole(Role role);
    long countByRoleAndStatus(Role role, UserStatus status);
    Page<User> findAll(Pageable pageable);
}
