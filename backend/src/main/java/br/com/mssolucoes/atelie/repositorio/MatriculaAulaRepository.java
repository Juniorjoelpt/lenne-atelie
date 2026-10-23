package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.MatriculaAula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatriculaAulaRepository extends JpaRepository<MatriculaAula, Long> {

    List<MatriculaAula> findByMatriculaId(Long matriculaId);

    Optional<MatriculaAula> findByMatriculaIdAndAulaId(Long matriculaId, Long aulaId);

    long countByMatriculaId(Long matriculaId);
}
