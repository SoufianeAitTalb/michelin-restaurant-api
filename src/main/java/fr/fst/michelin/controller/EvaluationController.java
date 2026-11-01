package fr.fst.michelin.controller;

import fr.fst.michelin.dto.EvaluationDto;
import fr.fst.michelin.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/restaurants/{id}/evaluations")
    public EvaluationDto addEvaluation(@PathVariable(name = "id") Integer id,
                                       @Valid @RequestBody EvaluationDto evaluationDto,
                                       Authentication principal) {
        return evaluationService.addEvaluation(id, principal.getName(),
                evaluationDto.getCommentaire(), evaluationDto.getNote());
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/evaluations/{id}")
    public void deleteEvaluation(@PathVariable(name = "id") Integer id, Authentication principal) {
        boolean admin = principal.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        evaluationService.deleteEvaluation(id, principal.getName(), admin);
    }

    @GetMapping("/restaurants/{id}/evaluations")
    public List<EvaluationDto> searchEvaluations(@PathVariable(name = "id") Integer id,
                                                 @RequestParam(name = "motsCles") List<String> motsCles) {
        return evaluationService.searchEvaluations(id, motsCles);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/evaluations/moi")
    public List<EvaluationDto> getMesEvaluations(Authentication principal) {
        return evaluationService.getMesEvaluations(principal.getName());
    }
}
