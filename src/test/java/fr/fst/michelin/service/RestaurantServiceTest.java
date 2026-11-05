package fr.fst.michelin.service;

import fr.fst.michelin.dto.RestaurantDto;
import fr.fst.michelin.entity.EvaluationEntity;
import fr.fst.michelin.entity.RestaurantEntity;
import fr.fst.michelin.exception.NotFoundException;
import fr.fst.michelin.repository.EvaluationRepository;
import fr.fst.michelin.repository.RestaurantRepository;
import fr.fst.michelin.repository.S3Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RestaurantServiceTest {

    @InjectMocks
    private RestaurantService restaurantService;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private S3Repository s3Repository;

    // aucun restaurant en base donc liste vide
    @Test
    void should_get_an_empty_list() {
        when(restaurantRepository.findAll()).thenReturn(Collections.emptyList());

        List<RestaurantDto> result = restaurantService.getRestaurants();

        assertEquals(0, result.size());
    }

    // chaque restaurant a sa moyenne, -1 s'il n'a pas d'évaluation
    @Test
    void should_get_restaurants_with_their_moyenne() {
        RestaurantEntity restaurant1 = creerRestaurant(1, "Chez Paul");
        RestaurantEntity restaurant2 = creerRestaurant(2, "Le Grenier");
        when(restaurantRepository.findAll()).thenReturn(List.of(restaurant1, restaurant2));
        when(evaluationRepository.findByRestaurantId(1)).thenReturn(List.of(creerEvaluation(3), creerEvaluation(2)));
        when(evaluationRepository.findByRestaurantId(2)).thenReturn(Collections.emptyList());

        List<RestaurantDto> result = restaurantService.getRestaurants();

        assertEquals(2, result.size());
        assertEquals(2.5, result.get(0).getMoyenne());
        assertEquals(-1.0, result.get(1).getMoyenne());
    }

    // pas d'évaluation donc moyenne à -1
    @Test
    void should_get_moyenne_minus_one_when_restaurant_has_no_evaluation() {
        when(restaurantRepository.findById(1)).thenReturn(Optional.of(creerRestaurant(1, "Chez Paul")));
        when(evaluationRepository.findByRestaurantId(1)).thenReturn(Collections.emptyList());

        RestaurantDto result = restaurantService.getRestaurant(1);

        assertEquals(-1.0, result.getMoyenne());
        assertEquals(0, result.getEvaluations().size());
    }

    // moyenne des notes 1 et 2 = 1.5
    @Test
    void should_get_moyenne_when_restaurant_has_evaluations() {
        when(restaurantRepository.findById(1)).thenReturn(Optional.of(creerRestaurant(1, "Chez Paul")));
        when(evaluationRepository.findByRestaurantId(1)).thenReturn(List.of(creerEvaluation(1), creerEvaluation(2)));

        RestaurantDto result = restaurantService.getRestaurant(1);

        assertEquals(1.5, result.getMoyenne());
        assertEquals(2, result.getEvaluations().size());
    }

    // une note de 0 donne une moyenne de 0 et pas -1
    @Test
    void should_get_moyenne_zero_and_not_minus_one_when_note_is_zero() {
        when(restaurantRepository.findById(1)).thenReturn(Optional.of(creerRestaurant(1, "Chez Paul")));
        when(evaluationRepository.findByRestaurantId(1)).thenReturn(List.of(creerEvaluation(0)));

        RestaurantDto result = restaurantService.getRestaurant(1);

        assertEquals(0.0, result.getMoyenne());
    }

    // un seul appel à chaque repository
    @Test
    void should_call_the_db_once_per_repository_when_getting_a_restaurant() {
        when(restaurantRepository.findById(1)).thenReturn(Optional.of(creerRestaurant(1, "Chez Paul")));
        when(evaluationRepository.findByRestaurantId(1)).thenReturn(Collections.emptyList());

        restaurantService.getRestaurant(1);

        verify(restaurantRepository, times(1)).findById(1);
        verify(evaluationRepository, times(1)).findByRestaurantId(1);
    }

    // restaurant inexistant : erreur 404
    @Test
    void should_throw_not_found_when_restaurant_does_not_exist() {
        when(restaurantRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> restaurantService.getRestaurant(99));
    }

    // création : le restaurant est sauvegardé avec une moyenne à -1
    @Test
    void should_create_a_restaurant() {
        RestaurantDto result = restaurantService.createRestaurant("Chez Paul", "1 rue de Nancy");

        assertEquals("Chez Paul", result.getNom());
        assertEquals("1 rue de Nancy", result.getAdresse());
        assertEquals(-1.0, result.getMoyenne());
        verify(restaurantRepository, times(1)).save(any(RestaurantEntity.class));
    }

    // modification du nom et de l'adresse
    @Test
    void should_update_nom_and_adresse() {
        RestaurantEntity restaurant = creerRestaurant(1, "Ancien nom");
        when(restaurantRepository.findById(1)).thenReturn(Optional.of(restaurant));

        RestaurantDto result = restaurantService.updateRestaurant(1, "Nouveau nom", "2 rue de Metz");

        assertEquals("Nouveau nom", result.getNom());
        assertEquals("2 rue de Metz", result.getAdresse());
        verify(restaurantRepository, times(1)).save(restaurant);
    }

    // restaurant inexistant : 404 et rien n'est sauvegardé
    @Test
    void should_not_save_when_updating_a_restaurant_that_does_not_exist() {
        when(restaurantRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> restaurantService.updateRestaurant(99, "Nom", "Adresse"));
        verify(restaurantRepository, never()).save(any());
    }

    // la clé de l'image est enregistrée et on récupère le lien d'upload
    @Test
    void should_save_an_image_key_and_get_the_upload_url() {
        RestaurantEntity restaurant = creerRestaurant(1, "Chez Paul");
        when(restaurantRepository.findById(1)).thenReturn(Optional.of(restaurant));
        when(s3Repository.getUploadFileUrl(any(), any())).thenReturn("https://s3/lien-upload");

        String result = restaurantService.getImageUploadUrl(1);

        assertEquals("https://s3/lien-upload", result);
        assertNotNull(restaurant.getImage());
        verify(restaurantRepository, times(1)).save(restaurant);
        verify(s3Repository, times(1)).getUploadFileUrl("restaurant", restaurant.getImage());
    }

    // pas d'image : 404 et pas d'appel à S3
    @Test
    void should_throw_not_found_when_restaurant_has_no_image() {
        when(restaurantRepository.findById(1)).thenReturn(Optional.of(creerRestaurant(1, "Chez Paul")));

        assertThrows(NotFoundException.class, () -> restaurantService.getImageUrl(1));
        verify(s3Repository, never()).getDownloadFileUrl(any(), any());
    }

    private RestaurantEntity creerRestaurant(Integer id, String nom) {
        return RestaurantEntity.builder()
                .id(id)
                .nom(nom)
                .adresse("1 rue de Nancy")
                .build();
    }

    private EvaluationEntity creerEvaluation(Integer note) {
        return EvaluationEntity.builder()
                .evaluateur("Lucien Bramard")
                .commentaire("Tres bon")
                .note(note)
                .build();
    }
}
