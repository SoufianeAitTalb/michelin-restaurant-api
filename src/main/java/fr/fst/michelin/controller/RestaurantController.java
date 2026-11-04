package fr.fst.michelin.controller;

import fr.fst.michelin.dto.RestaurantDto;
import fr.fst.michelin.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @GetMapping("/restaurants")
    public List<RestaurantDto> getRestaurants() {
        return restaurantService.getRestaurants();
    }

    @GetMapping("/restaurants/{id}")
    public RestaurantDto getRestaurant(@PathVariable(name = "id") Integer id) {
        return restaurantService.getRestaurant(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/restaurants")
    public RestaurantDto createRestaurant(@Valid @RequestBody RestaurantDto restaurantDto) {
        return restaurantService.createRestaurant(restaurantDto.getNom(), restaurantDto.getAdresse());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/restaurants/{id}")
    public RestaurantDto updateRestaurant(@PathVariable(name = "id") Integer id,
                                          @Valid @RequestBody RestaurantDto restaurantDto) {
        return restaurantService.updateRestaurant(id, restaurantDto.getNom(), restaurantDto.getAdresse());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/restaurants/{id}/image")
    public String getImageUploadUrl(@PathVariable(name = "id") Integer id) {
        return restaurantService.getImageUploadUrl(id);
    }

    @GetMapping("/restaurants/{id}/image")
    public String getImageUrl(@PathVariable(name = "id") Integer id) {
        return restaurantService.getImageUrl(id);
    }
}
