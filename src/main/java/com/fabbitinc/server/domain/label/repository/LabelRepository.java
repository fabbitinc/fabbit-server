package com.fabbitinc.server.domain.label.repository;

import com.fabbitinc.server.domain.label.model.Label;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<Label, UUID> {

    Optional<Label> findByName(String name);

    List<Label> findAllByOrderByNameAsc();

    List<Label> findAllByOrderByNameAsc(Pageable pageable);

    List<Label> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);
}
