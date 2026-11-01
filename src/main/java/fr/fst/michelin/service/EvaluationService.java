package fr.fst.michelin.service;

import fr.fst.michelin.dto.EvaluationDto;
import fr.fst.michelin.entity.EvaluationEntity;
import fr.fst.michelin.entity.RestaurantEntity;
import fr.fst.michelin.exception.NotFoundException;
import fr.fst.michelin.repository.EvaluationRepository;
import fr.fst.michelin.repository.PhotoRepository;
import fr.fst.michelin.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final RestaurantRepository restaurantRepository;
    private final PhotoRepository photoRepository;

    public EvaluationDto addEvaluation(Integer restaurantId, String evaluateur, String commentaire, Integer note) {
        RestaurantEntity restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new NotFoundException("Le restaurant " + restaurantId + " n'existe pas"));

        EvaluationEntity evaluation = new EvaluationEntity();
        evaluation.setEvaluateur(evaluateur);
        evaluation.setCommentaire(commentaire);
        evaluation.setNote(note);
        evaluation.setRestaurant(restaurant);

        evaluationRepository.save(evaluation);
        return EvaluationDto.fromEntity(evaluation);
    }

    public void deleteEvaluation(Integer id, String utilisateur, boolean admin) {
        EvaluationEntity evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("L'évaluation " + id + " n'existe pas"));

        if (!admin && !evaluation.getEvaluateur().equals(utilisateur)) {
            throw new AccessDeniedException("Ce n'est pas votre évaluation");
        }

        photoRepository.deleteAll(photoRepository.findByEvaluationId(id));
        evaluationRepository.delete(evaluation);
    }

    public List<EvaluationDto> searchEvaluations(Integer restaurantId, List<String> motsCles) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new NotFoundException("Le restaurant " + restaurantId + " n'existe pas");
        }

        List<EvaluationDto> resultat = new ArrayList<>();
        for (String motCle : motsCles) {
            List<EvaluationEntity> evaluations = evaluationRepository
                    .findByRestaurantIdAndCommentaireContainingIgnoreCase(restaurantId, motCle);
            for (EvaluationEntity evaluation : evaluations) {
                EvaluationDto evaluationDto = EvaluationDto.fromEntity(evaluation);
                if (!resultat.contains(evaluationDto)) {
                    resultat.add(evaluationDto);
                }
            }
        }
        return resultat;
    }

    public List<EvaluationDto> getMesEvaluations(String evaluateur) {
        return evaluationRepository.findByEvaluateur(evaluateur).stream()
                .map(evaluation -> EvaluationDto.fromEntity(evaluation))
                .toList();
    }
}
