package com.fabbitinc.server.domain.user.repository;

import com.fabbitinc.server.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    @Query("""
            select u
            from User u
            where u.id in :userIds
            order by u.fullName
            """)
    List<User> findAllByIdInOrderByFullName(Collection<UUID> userIds);
}
