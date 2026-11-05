package fr.fst.michelin.service;

import fr.fst.michelin.entity.EvaluationEntity;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IndexServiceTest {

    // la recherche ignore les majuscules (FRITES trouve frites)
    @Test
    void should_find_an_evaluation_with_one_keyword() throws IOException {
        IndexService indexService = creerIndexService();
        indexService.indexEvaluation(creerEvaluation(1, "Les frites sont excellentes"));
        indexService.indexEvaluation(creerEvaluation(2, "Service lent"));

        List<Integer> result = indexService.searchEvaluations(List.of("FRITES"));

        assertEquals(List.of(1), result);
    }

    // avec 2 mots clés on trouve les 2 évaluations
    @Test
    void should_find_evaluations_with_several_keywords() throws IOException {
        IndexService indexService = creerIndexService();
        indexService.indexEvaluation(creerEvaluation(1, "Les frites sont excellentes"));
        indexService.indexEvaluation(creerEvaluation(2, "Service lent"));
        indexService.indexEvaluation(creerEvaluation(3, "Bon dessert"));

        List<Integer> result = indexService.searchEvaluations(List.of("frites", "service"));

        assertEquals(2, result.size());
    }

    // après modification l'ancien commentaire n'est plus trouvé
    @Test
    void should_find_the_new_comment_after_an_update() throws IOException {
        IndexService indexService = creerIndexService();
        indexService.indexEvaluation(creerEvaluation(1, "Les frites sont excellentes"));

        indexService.indexEvaluation(creerEvaluation(1, "Le dessert est excellent"));

        assertEquals(0, indexService.searchEvaluations(List.of("frites")).size());
        assertEquals(List.of(1), indexService.searchEvaluations(List.of("dessert")));
    }

    // une évaluation supprimée n'est plus trouvée
    @Test
    void should_not_find_a_deleted_evaluation() throws IOException {
        IndexService indexService = creerIndexService();
        indexService.indexEvaluation(creerEvaluation(1, "Les frites sont excellentes"));

        indexService.deleteEvaluation(1);

        assertEquals(0, indexService.searchEvaluations(List.of("frites")).size());
    }

    private IndexService creerIndexService() throws IOException {
        return new IndexService("target/index-test/" + UUID.randomUUID());
    }

    private EvaluationEntity creerEvaluation(Integer id, String commentaire) {
        return EvaluationEntity.builder()
                .id(id)
                .commentaire(commentaire)
                .build();
    }
}
