package fr.fst.michelin.service;

import fr.fst.michelin.dto.EvaluationDto;
import fr.fst.michelin.entity.EvaluationEntity;
import fr.fst.michelin.entity.PhotoEntity;
import fr.fst.michelin.entity.RestaurantEntity;
import fr.fst.michelin.exception.NotFoundException;
import fr.fst.michelin.repository.EvaluationRepository;
import fr.fst.michelin.repository.PhotoRepository;
import fr.fst.michelin.repository.RestaurantRepository;
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

    @Test
    void should_add_an_evaluation() {
        when(restaurantRepository.findById(1)).thenReturn(Optional.of(RestaurantEntity.builder().id(1).build()));

        EvaluationDto result = evaluationService.addEvaluation(1, "lucien", "Tres bon", 3);

        assertEquals("lucien", result.getEvaluateur());
        assertEquals("Tres bon", result.getCommentaire());
        assertEquals(3, result.getNote());
        verify(evaluationRepository, times(1)).save(any(EvaluationEntity.class));
    }

    @Test
    void should_not_save_when_adding_an_evaluation_to_a_restaurant_that_does_not_exist() {
        when(restaurantRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> evaluationService.addEvaluation(99, "lucien", "Tres bon", 3));
        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void should_delete_my_evaluation() {
        EvaluationEntity evaluation = creerEvaluation(1, "lucien", "Tres bon");
        when(evaluationRepository.findById(1)).thenReturn(Optional.of(evaluation));

        evaluationService.deleteEvaluation(1, "lucien", false);

        verify(evaluationRepository, times(1)).delete(evaluation);
    }

    @Test
    void should_delete_the_evaluation_of_another_user_when_admin() {
        EvaluationEntity evaluation = creerEvaluation(1, "lucien", "Tres bon");
        when(evaluationRepository.findById(1)).thenReturn(Optional.of(evaluation));

        evaluationService.deleteEvaluation(1, "noel", true);

        verify(evaluationRepository, times(1)).delete(evaluation);
    }

    @Test
    void should_not_delete_the_evaluation_of_another_user() {
        when(evaluationRepository.findById(1)).thenReturn(Optional.of(creerEvaluation(1, "noel", "Correct")));

        assertThrows(AccessDeniedException.class, () -> evaluationService.deleteEvaluation(1, "lucien", false));
        verify(evaluationRepository, never()).delete(any());
        verify(photoRepository, never()).deleteAll(any());
    }

    @Test
    void should_throw_not_found_when_deleting_an_evaluation_that_does_not_exist() {
        when(evaluationRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> evaluationService.deleteEvaluation(99, "lucien", false));
    }

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

    @Test
    void should_search_evaluations_with_one_keyword() {
        when(restaurantRepository.existsById(1)).thenReturn(true);
        when(evaluationRepository.findByRestaurantIdAndCommentaireContainingIgnoreCase(1, "frites"))
                .thenReturn(List.of(creerEvaluation(1, "lucien", "Les frites sont bonnes")));

        List<EvaluationDto> result = evaluationService.searchEvaluations(1, List.of("frites"));

        assertEquals(1, result.size());
        assertEquals("Les frites sont bonnes", result.get(0).getCommentaire());
    }

    @Test
    void should_not_return_duplicates_when_searching_with_several_keywords() {
        EvaluationEntity evaluation1 = creerEvaluation(1, "lucien", "Bon restaurant, les frites sont bonnes");
        EvaluationEntity evaluation2 = creerEvaluation(2, "noel", "Bon accueil");
        when(restaurantRepository.existsById(1)).thenReturn(true);
        when(evaluationRepository.findByRestaurantIdAndCommentaireContainingIgnoreCase(1, "bon"))
                .thenReturn(List.of(evaluation1, evaluation2));
        when(evaluationRepository.findByRestaurantIdAndCommentaireContainingIgnoreCase(1, "frites"))
                .thenReturn(List.of(evaluation1));

        List<EvaluationDto> result = evaluationService.searchEvaluations(1, List.of("bon", "frites"));

        assertEquals(2, result.size());
    }

    @Test
    void should_get_an_empty_list_when_no_evaluation_matches() {
        when(restaurantRepository.existsById(1)).thenReturn(true);
        when(evaluationRepository.findByRestaurantIdAndCommentaireContainingIgnoreCase(1, "pizza"))
                .thenReturn(Collections.emptyList());

        List<EvaluationDto> result = evaluationService.searchEvaluations(1, List.of("pizza"));

        assertEquals(0, result.size());
    }

    @Test
    void should_throw_not_found_when_searching_in_a_restaurant_that_does_not_exist() {
        when(restaurantRepository.existsById(99)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> evaluationService.searchEvaluations(99, List.of("bon")));
    }

    @Test
    void should_get_my_evaluations() {
        when(evaluationRepository.findByEvaluateur("lucien"))
                .thenReturn(List.of(creerEvaluation(1, "lucien", "Tres bon"), creerEvaluation(2, "lucien", "Moyen")));

        List<EvaluationDto> result = evaluationService.getMesEvaluations("lucien");

        assertEquals(2, result.size());
        assertEquals("lucien", result.get(0).getEvaluateur());
        assertEquals("lucien", result.get(1).getEvaluateur());
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
