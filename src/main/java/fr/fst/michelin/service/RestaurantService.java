package fr.fst.michelin.service;

import fr.fst.michelin.dto.RestaurantDto;
import fr.fst.michelin.entity.EvaluationEntity;
import fr.fst.michelin.entity.RestaurantEntity;
import fr.fst.michelin.exception.NotFoundException;
import fr.fst.michelin.repository.EvaluationRepository;
import fr.fst.michelin.repository.RestaurantRepository;
import fr.fst.michelin.repository.S3Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private static final String BUCKET = "restaurant";

    private final RestaurantRepository restaurantRepository;
    private final EvaluationRepository evaluationRepository;
    private final S3Repository s3Repository;

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

    public String getImageUploadUrl(Integer id) {
        RestaurantEntity restaurant = findRestaurant(id);
        restaurant.setImage(UUID.randomUUID().toString());

        restaurantRepository.save(restaurant);
        return s3Repository.getUploadFileUrl(BUCKET, restaurant.getImage());
    }

    public String getImageUrl(Integer id) {
        RestaurantEntity restaurant = findRestaurant(id);
        if (restaurant.getImage() == null) {
            throw new NotFoundException("Le restaurant " + id + " n'a pas d'image");
        }
        return s3Repository.getDownloadFileUrl(BUCKET, restaurant.getImage());
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
