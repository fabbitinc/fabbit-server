package com.fabbitinc.server.application.label.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.label.model.Label;
import com.fabbitinc.server.domain.label.repository.LabelRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LabelService {

    private static final List<DefaultLabel> DEFAULT_LABELS = List.of(
            new DefaultLabel("우선순위:높음", "즉시 처리 필요", "#b60205"),
            new DefaultLabel("우선순위:중간", "일반 처리", "#fbca04"),
            new DefaultLabel("우선순위:낮음", "여유 시 처리", "#0e8a16"),
            new DefaultLabel("설계변경", "설계 도면 또는 사양 변경", "#0075ca"),
            new DefaultLabel("품질", "품질 불량 및 결함 보고", "#d73a4a"),
            new DefaultLabel("개선", "기존 부품·공정 개선", "#a2eeef"),
            new DefaultLabel("원가절감", "원가 절감 활동", "#c5def5"),
            new DefaultLabel("공급사", "공급사 관련 문제", "#f9d0c4"),
            new DefaultLabel("시험검증", "시험·검증 요청", "#bfd4f2")
    );

    private final LabelRepository labelRepository;

    public Label createLabel(UUID actorId, String name, String color, String description) {
        ensureNameNotExists(name, null);
        Label label = Label.create(name, description, color, actorId);
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

    public void ensureDefaultLabelsExist() {
        for (DefaultLabel defaultLabel : DEFAULT_LABELS) {
            if (labelRepository.findByName(defaultLabel.name()).isPresent()) {
                continue;
            }
            labelRepository.save(Label.createSystemDefault(
                    defaultLabel.name(),
                    defaultLabel.description(),
                    defaultLabel.color()
            ));
        }
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

    private record DefaultLabel(String name, String description, String color) {
    }
}
