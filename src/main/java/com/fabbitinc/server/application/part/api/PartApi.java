package com.fabbitinc.server.application.part.api;

import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PartApi {

    private final PartRepository partRepository;

    public boolean existsPart(UUID partId) {
        return partRepository.existsById(partId);
    }

    public List<Part> searchParts(String keyword, int limit) {
        return partRepository.findByPartNumberContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByPartNumberAsc(
                keyword,
                keyword,
                PageRequest.of(0, limit)
        );
    }

    public List<Part> getPartsByIdsOrdered(List<UUID> partIds) {
        return partRepository.findByIdInOrderByPartNumberAsc(partIds);
    }
}
