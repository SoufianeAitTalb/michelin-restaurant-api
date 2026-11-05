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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EvaluationServiceTest {

    @InjectMocks
    private EvaluationService evaluationService;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private S3Repository s3Repository;

    @Mock
    private IndexService indexService;

    // ajout d'une évaluation, elle est sauvegardée et indexée dans lucene
    @Test
    void should_add_an_evaluation() {
        when(restaurantRepository.findById(1)).thenReturn(Optional.of(RestaurantEntity.builder().id(1).build()));

        EvaluationDto result = evaluationService.addEvaluation(1, "lucien", "Tres bon", 3);

        assertEquals("lucien", result.getEvaluateur());
        assertEquals("Tres bon", result.getCommentaire());
        assertEquals(3, result.getNote());
        verify(evaluationRepository, times(1)).save(any(EvaluationEntity.class));
        verify(indexService, times(1)).indexEvaluation(any(EvaluationEntity.class));
    }

    // restaurant inexistant : l'évaluation n'est pas sauvegardée
    @Test
    void should_not_save_when_adding_an_evaluation_to_a_restaurant_that_does_not_exist() {
        when(restaurantRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> evaluationService.addEvaluation(99, "lucien", "Tres bon", 3));
        verify(evaluationRepository, never()).save(any());
    }

    // l'auteur modifie son évaluation et l'index est mis à jour
    @Test
    void should_update_my_evaluation() {
        EvaluationEntity evaluation = creerEvaluation(1, "lucien", "Tres bon");
        when(evaluationRepository.findById(1)).thenReturn(Optional.of(evaluation));

        EvaluationDto result = evaluationService.updateEvaluation(1, "lucien", false, "Moyen finalement", 1);

        assertEquals("Moyen finalement", result.getCommentaire());
        assertEquals(1, result.getNote());
        verify(evaluationRepository, times(1)).save(evaluation);
        verify(indexService, times(1)).indexEvaluation(evaluation);
    }

    // on ne peut pas modifier l'évaluation d'un autre utilisateur
    @Test
    void should_not_update_the_evaluation_of_another_user() {
        when(evaluationRepository.findById(1)).thenReturn(Optional.of(creerEvaluation(1, "noel", "Correct")));

        assertThrows(AccessDeniedException.class,
                () -> evaluationService.updateEvaluation(1, "lucien", false, "Nouveau commentaire", 0));
        verify(evaluationRepository, never()).save(any());
    }

    // l'auteur supprime son évaluation, elle est aussi enlevée de l'index
    @Test
    void should_delete_my_evaluation() {
        EvaluationEntity evaluation = creerEvaluation(1, "lucien", "Tres bon");
        when(evaluationRepository.findById(1)).thenReturn(Optional.of(evaluation));

        evaluationService.deleteEvaluation(1, "lucien", false);

        verify(evaluationRepository, times(1)).delete(evaluation);
        verify(indexService, times(1)).deleteEvaluation(1);
    }

    // un admin peut supprimer l'évaluation de n'importe qui
    @Test
    void should_delete_the_evaluation_of_another_user_when_admin() {
        EvaluationEntity evaluation = creerEvaluation(1, "lucien", "Tres bon");
        when(evaluationRepository.findById(1)).thenReturn(Optional.of(evaluation));

        evaluationService.deleteEvaluation(1, "noel", true);

        verify(evaluationRepository, times(1)).delete(evaluation);
    }

    // un USER ne peut pas supprimer l'évaluation d'un autre
    @Test
    void should_not_delete_the_evaluation_of_another_user() {
        when(evaluationRepository.findById(1)).thenReturn(Optional.of(creerEvaluation(1, "noel", "Correct")));

        assertThrows(AccessDeniedException.class, () -> evaluationService.deleteEvaluation(1, "lucien", false));
        verify(evaluationRepository, never()).delete(any());
        verify(photoRepository, never()).deleteAll(any());
    }

    // suppression d'une évaluation qui n'existe pas : 404
    @Test
    void should_throw_not_found_when_deleting_an_evaluation_that_does_not_exist() {
        when(evaluationRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> evaluationService.deleteEvaluation(99, "lucien", false));
    }

    // les photos sont supprimées avec l'évaluation
    @Test
    void should_delete_the_photos_of_the_evaluation() {
        EvaluationEntity evaluation = creerEvaluation(1, "lucien", "Tres bon");
        List<PhotoEntity> photos = List.of(PhotoEntity.builder().cle("photo1.jpg").build(),
                PhotoEntity.builder().cle("photo2.jpg").build());
        when(evaluationRepository.findById(1)).thenReturn(Optional.of(evaluation));
        when(photoRepository.findByEvaluationId(1)).thenReturn(photos);

        evaluationService.deleteEvaluation(1, "lucien", false);

        verify(photoRepository, times(1)).deleteAll(photos);
        verify(evaluationRepository, times(1)).delete(evaluation);
    }

    // recherche avec un seul mot clé
    @Test
    void should_search_evaluations_with_one_keyword() {
        EvaluationEntity evaluation = creerEvaluation(1, "lucien", "Les frites sont bonnes");
        evaluation.setRestaurant(RestaurantEntity.builder().id(1).build());
        when(restaurantRepository.existsById(1)).thenReturn(true);
        when(indexService.searchEvaluations(List.of("frites"))).thenReturn(List.of(1));
        when(evaluationRepository.findById(1)).thenReturn(Optional.of(evaluation));

        List<EvaluationDto> result = evaluationService.searchEvaluations(1, List.of("frites"));

        assertEquals(1, result.size());
        assertEquals("Les frites sont bonnes", result.get(0).getCommentaire());
    }

    // lucene trouve 2 évaluations mais une seule est de ce restaurant
    @Test
    void should_not_return_the_evaluations_of_another_restaurant() {
        EvaluationEntity evaluation1 = creerEvaluation(1, "lucien", "Les frites sont bonnes");
        evaluation1.setRestaurant(RestaurantEntity.builder().id(1).build());
        EvaluationEntity evaluation2 = creerEvaluation(2, "noel", "Frites trop salees");
        evaluation2.setRestaurant(RestaurantEntity.builder().id(2).build());
        when(restaurantRepository.existsById(1)).thenReturn(true);
        when(indexService.searchEvaluations(List.of("frites"))).thenReturn(List.of(1, 2));
        when(evaluationRepository.findById(1)).thenReturn(Optional.of(evaluation1));
        when(evaluationRepository.findById(2)).thenReturn(Optional.of(evaluation2));

        List<EvaluationDto> result = evaluationService.searchEvaluations(1, List.of("frites"));

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());
    }

    // aucun résultat pour ce mot clé
    @Test
    void should_get_an_empty_list_when_no_evaluation_matches() {
        when(restaurantRepository.existsById(1)).thenReturn(true);
        when(indexService.searchEvaluations(List.of("pizza"))).thenReturn(Collections.emptyList());

        List<EvaluationDto> result = evaluationService.searchEvaluations(1, List.of("pizza"));

        assertEquals(0, result.size());
    }

    // recherche dans un restaurant qui n'existe pas : 404
    @Test
    void should_throw_not_found_when_searching_in_a_restaurant_that_does_not_exist() {
        when(restaurantRepository.existsById(99)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> evaluationService.searchEvaluations(99, List.of("bon")));
    }

    // retourne les évaluations de l'utilisateur connecté
    @Test
    void should_get_my_evaluations() {
        when(evaluationRepository.findByEvaluateur("lucien"))
                .thenReturn(List.of(creerEvaluation(1, "lucien", "Tres bon"), creerEvaluation(2, "lucien", "Moyen")));

        List<EvaluationDto> result = evaluationService.getMesEvaluations("lucien");

        assertEquals(2, result.size());
        assertEquals("lucien", result.get(0).getEvaluateur());
        assertEquals("lucien", result.get(1).getEvaluateur());
    }

    // la photo est enregistrée et on récupère le lien d'upload
    @Test
    void should_save_a_photo_and_get_the_upload_url() {
        when(evaluationRepository.findById(1)).thenReturn(Optional.of(creerEvaluation(1, "lucien", "Tres bon")));
        when(s3Repository.getUploadFileUrl(any(), any())).thenReturn("https://s3/lien-upload");

        String result = evaluationService.getPhotoUploadUrl(1, "lucien", false);

        assertEquals("https://s3/lien-upload", result);
        verify(photoRepository, times(1)).save(any(PhotoEntity.class));
    }

    // pas de photo sur l'évaluation d'un autre utilisateur
    @Test
    void should_not_add_a_photo_to_the_evaluation_of_another_user() {
        when(evaluationRepository.findById(1)).thenReturn(Optional.of(creerEvaluation(1, "noel", "Correct")));

        assertThrows(AccessDeniedException.class, () -> evaluationService.getPhotoUploadUrl(1, "lucien", false));
        verify(photoRepository, never()).save(any());
        verify(s3Repository, never()).getUploadFileUrl(any(), any());
    }

    private EvaluationEntity creerEvaluation(Integer id, String evaluateur, String commentaire) {
        return EvaluationEntity.builder()
                .id(id)
                .evaluateur(evaluateur)
                .commentaire(commentaire)
                .note(2)
                .build();
    }
}
