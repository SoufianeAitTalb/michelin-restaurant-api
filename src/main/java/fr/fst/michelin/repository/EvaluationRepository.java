package fr.fst.michelin.repository;

import fr.fst.michelin.entity.EvaluationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationRepository extends JpaRepository<EvaluationEntity, Integer> {

    List<EvaluationEntity> findByRestaurantId(Integer restaurantId);

    List<EvaluationEntity> findByEvaluateur(String evaluateur);

    List<EvaluationEntity> findByRestaurantIdAndCommentaireContainingIgnoreCase(Integer restaurantId, String motCle);
}
