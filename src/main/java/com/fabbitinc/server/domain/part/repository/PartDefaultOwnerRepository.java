package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.PartDefaultOwner;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PartDefaultOwnerRepository extends JpaRepository<PartDefaultOwner, UUID> {

    Optional<PartDefaultOwner> findByCategory(String category);

    Optional<PartDefaultOwner> findByCategoryIsNull();

    long deleteByCategory(String category);

    long deleteByCategoryIsNull();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PartDefaultOwner p set p.category = ?2 where p.category = ?1")
    int renameCategory(String oldName, String newName);
}
