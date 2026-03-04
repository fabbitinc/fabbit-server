package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.PartDefaultOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartDefaultOwnerRepository extends JpaRepository<PartDefaultOwner, UUID> {

    Optional<PartDefaultOwner> findByCategory(String category);

    Optional<PartDefaultOwner> findByCategoryIsNull();

    long deleteByCategory(String category);

    long deleteByCategoryIsNull();

    @Query(
            "select p from PartDefaultOwner p " +
                    "order by case when p.category is null then 0 else 1 end, p.category asc"
    )
    List<PartDefaultOwner> findAllOrderByCategoryNullsFirst();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PartDefaultOwner p set p.category = ?2 where p.category = ?1")
    int renameCategory(String oldName, String newName);
}
