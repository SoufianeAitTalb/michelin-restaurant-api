package fr.fst.michelin.repository;

import fr.fst.michelin.entity.PhotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhotoRepository extends JpaRepository<PhotoEntity, Integer> {

    List<PhotoEntity> findByEvaluationId(Integer evaluationId);
}
