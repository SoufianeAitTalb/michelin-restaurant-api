package fr.fst.michelin.dto;

import fr.fst.michelin.entity.EvaluationEntity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotBlank
    @Size(max = 255)
    private String commentaire;

    @NotNull
    @Min(0)
    @Max(3)
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
