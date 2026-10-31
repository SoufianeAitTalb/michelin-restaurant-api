package fr.fst.michelin.dto;

import fr.fst.michelin.entity.EvaluationEntity;
import fr.fst.michelin.entity.RestaurantEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantDto {

    private Integer id;

    @NotBlank
    @Size(max = 90)
    private String nom;

    @NotBlank
    @Size(max = 255)
    private String adresse;

    private Double moyenne;

    private List<EvaluationDto> evaluations;

    public static RestaurantDto fromEntity(RestaurantEntity restaurant, List<EvaluationEntity> evaluations, double moyenne) {
        return RestaurantDto.builder()
                .id(restaurant.getId())
                .nom(restaurant.getNom())
                .adresse(restaurant.getAdresse())
                .moyenne(moyenne)
                .evaluations(evaluations.stream()
                        .map(evaluation -> EvaluationDto.fromEntity(evaluation))
                        .toList())
                .build();
    }
}
