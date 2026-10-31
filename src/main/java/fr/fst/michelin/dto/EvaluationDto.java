package fr.fst.michelin.dto;

import fr.fst.michelin.entity.EvaluationEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationDto {

    private Integer id;
    private String evaluateur;
    private String commentaire;
    private Integer note;

    public static EvaluationDto fromEntity(EvaluationEntity evaluation) {
        return EvaluationDto.builder()
                .id(evaluation.getId())
                .evaluateur(evaluation.getEvaluateur())
                .commentaire(evaluation.getCommentaire())
                .note(evaluation.getNote())
                .build();
    }
}
