package com.fabbitinc.server.domain.user.repository;

import com.fabbitinc.server.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    List<User> findByIdInOrderByFullNameAsc(Collection<UUID> userIds);
}
