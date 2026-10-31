package fr.fst.michelin.service;

import fr.fst.michelin.dto.RestaurantDto;
import fr.fst.michelin.entity.EvaluationEntity;
import fr.fst.michelin.entity.RestaurantEntity;
import fr.fst.michelin.exception.NotFoundException;
import fr.fst.michelin.repository.EvaluationRepository;
import fr.fst.michelin.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final EvaluationRepository evaluationRepository;

    public List<RestaurantDto> getRestaurants() {
        return restaurantRepository.findAll().stream()
                .map(restaurant -> toDto(restaurant))
                .toList();
    }

    public RestaurantDto getRestaurant(Integer id) {
        return toDto(findRestaurant(id));
    }

    public RestaurantDto createRestaurant(String nom, String adresse) {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setNom(nom);
        restaurant.setAdresse(adresse);

        restaurantRepository.save(restaurant);
        return toDto(restaurant);
    }

    public RestaurantDto updateRestaurant(Integer id, String nom, String adresse) {
        RestaurantEntity restaurant = findRestaurant(id);
        restaurant.setNom(nom);
        restaurant.setAdresse(adresse);

        restaurantRepository.save(restaurant);
        return toDto(restaurant);
    }

    private RestaurantEntity findRestaurant(Integer id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Le restaurant " + id + " n'existe pas"));
    }

    private RestaurantDto toDto(RestaurantEntity restaurant) {
        List<EvaluationEntity> evaluations = evaluationRepository.findByRestaurantId(restaurant.getId());
        return RestaurantDto.fromEntity(restaurant, evaluations, calculerMoyenne(evaluations));
    }

    private double calculerMoyenne(List<EvaluationEntity> evaluations) {
        if (evaluations.isEmpty()) {
            return -1;
        }

        double somme = 0;
        for (EvaluationEntity evaluation : evaluations) {
            somme += evaluation.getNote();
        }
        return somme / evaluations.size();
    }
}
