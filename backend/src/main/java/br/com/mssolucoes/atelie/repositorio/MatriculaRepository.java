package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.Enums.StatusMatricula;
import br.com.mssolucoes.atelie.dominio.Matricula;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    @EntityGraph(attributePaths = "curso")
    Optional<Matricula> findByCodigo(String codigo);

    List<Matricula> findByCursoIdOrderByCriadoEmDesc(Long cursoId);

    @EntityGraph(attributePaths = "curso")
    List<Matricula> findByStatusInOrderByCriadoEmAsc(Collection<StatusMatricula> status);

    boolean existsByCursoId(Long cursoId);

    boolean existsByCursoIdAndWhatsappAndStatusNot(Long cursoId, String whatsapp, StatusMatricula status);

    long countByCursoIdAndStatus(Long cursoId, StatusMatricula status);

    long countByCursoIdAndStatusIn(Long cursoId, Collection<StatusMatricula> status);
}
