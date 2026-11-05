package fr.fst.michelin.service;

import fr.fst.michelin.dto.EvaluationDto;
import fr.fst.michelin.entity.EvaluationEntity;
import fr.fst.michelin.entity.PhotoEntity;
import fr.fst.michelin.entity.RestaurantEntity;
import fr.fst.michelin.exception.NotFoundException;
import fr.fst.michelin.repository.EvaluationRepository;
import fr.fst.michelin.repository.PhotoRepository;
import fr.fst.michelin.repository.RestaurantRepository;
import fr.fst.michelin.repository.S3Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private static final String BUCKET = "evaluation";

    private final EvaluationRepository evaluationRepository;
    private final RestaurantRepository restaurantRepository;
    private final PhotoRepository photoRepository;
    private final S3Repository s3Repository;
    private final IndexService indexService;

    public EvaluationDto addEvaluation(Integer restaurantId, String evaluateur, String commentaire, Integer note) {
        RestaurantEntity restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new NotFoundException("Le restaurant " + restaurantId + " n'existe pas"));

        EvaluationEntity evaluation = new EvaluationEntity();
        evaluation.setEvaluateur(evaluateur);
        evaluation.setCommentaire(commentaire);
        evaluation.setNote(note);
        evaluation.setRestaurant(restaurant);

        evaluationRepository.save(evaluation);
        indexService.indexEvaluation(evaluation);
        return EvaluationDto.fromEntity(evaluation);
    }

    public void deleteEvaluation(Integer id, String utilisateur, boolean admin) {
        EvaluationEntity evaluation = findEvaluation(id);
        verifierDroits(evaluation, utilisateur, admin);

        photoRepository.deleteAll(photoRepository.findByEvaluationId(id));
        evaluationRepository.delete(evaluation);
        indexService.deleteEvaluation(id);
    }

    public List<EvaluationDto> searchEvaluations(Integer restaurantId, List<String> motsCles) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new NotFoundException("Le restaurant " + restaurantId + " n'existe pas");
        }

        List<EvaluationDto> resultat = new ArrayList<>();
        for (Integer id : indexService.searchEvaluations(motsCles)) {
            EvaluationEntity evaluation = evaluationRepository.findById(id).orElse(null);
            if (evaluation != null && evaluation.getRestaurant().getId().equals(restaurantId)) {
                resultat.add(EvaluationDto.fromEntity(evaluation));
            }
        }
        return resultat;
    }

    public List<EvaluationDto> getMesEvaluations(String evaluateur) {
        return evaluationRepository.findByEvaluateur(evaluateur).stream()
                .map(evaluation -> EvaluationDto.fromEntity(evaluation))
                .toList();
    }

    public String getPhotoUploadUrl(Integer id, String utilisateur, boolean admin) {
        EvaluationEntity evaluation = findEvaluation(id);
        verifierDroits(evaluation, utilisateur, admin);

        PhotoEntity photo = new PhotoEntity();
        photo.setCle(UUID.randomUUID().toString());
        photo.setEvaluation(evaluation);

        photoRepository.save(photo);
        return s3Repository.getUploadFileUrl(BUCKET, photo.getCle());
    }

    public List<String> getPhotoUrls(Integer id) {
        findEvaluation(id);

        List<String> urls = new ArrayList<>();
        for (PhotoEntity photo : photoRepository.findByEvaluationId(id)) {
            urls.add(s3Repository.getDownloadFileUrl(BUCKET, photo.getCle()));
        }
        return urls;
    }

    private EvaluationEntity findEvaluation(Integer id) {
        return evaluationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("L'évaluation " + id + " n'existe pas"));
    }

    private void verifierDroits(EvaluationEntity evaluation, String utilisateur, boolean admin) {
        if (!admin && !evaluation.getEvaluateur().equals(utilisateur)) {
            throw new AccessDeniedException("Ce n'est pas votre évaluation");
        }
    }
}
