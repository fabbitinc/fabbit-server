package com.fabbitinc.server.application.label.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.label.model.Label;
import com.fabbitinc.server.domain.label.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LabelService {

    private final LabelRepository labelRepository;

    public Label createLabel(UUID actorId, String name, String color, String description) {
        ensureNameNotExists(name, null);
        Label label = new Label(name, description, color, actorId);
        return labelRepository.save(label);
    }

    public Label updateLabel(
            UUID actorId,
            UUID labelId,
            String name,
            String description,
            String color,
            boolean unsetDescription
    ) {
        Label label = getOrThrow(labelId);

        if (name != null && !name.equals(label.getName())) {
            ensureNameNotExists(name, label.getId());
            label.changeName(name, actorId);
        }

        if (unsetDescription) {
            label.removeDescription(actorId);
        } else if (description != null) {
            label.changeDescription(description, actorId);
        }

        if (color != null) {
            label.changeColor(color, actorId);
        }

        return label;
    }

    public void deleteLabel(UUID labelId) {
        Label label = getOrThrow(labelId);
        labelRepository.delete(label);
    }

    private void ensureNameNotExists(String name, UUID excludeLabelId) {
        labelRepository.findByName(name).ifPresent(existing -> {
            if (!existing.getId().equals(excludeLabelId)) {
                throw new AppException(
                        ErrorCode.ALREADY_EXISTS,
                        "동일한 이름의 '" + name + "' 라벨이 이미 존재합니다"
                );
            }
        });
    }

    private Label getOrThrow(UUID labelId) {
        return labelRepository.findById(labelId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Label '" + labelId + "'을(를) 찾을 수 없습니다"
                ));
    }
}
