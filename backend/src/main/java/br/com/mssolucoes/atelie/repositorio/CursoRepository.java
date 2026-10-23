package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CursoRepository extends JpaRepository<Curso, Long> {

    List<Curso> findAllByOrderByOrdemAscIdDesc();

    List<Curso> findByPublicadoTrueOrderByOrdemAscIdDesc();
}
