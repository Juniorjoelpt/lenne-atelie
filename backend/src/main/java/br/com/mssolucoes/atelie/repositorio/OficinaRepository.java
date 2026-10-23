package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.Enums.StatusOficina;
import br.com.mssolucoes.atelie.dominio.Oficina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OficinaRepository extends JpaRepository<Oficina, Long> {

    List<Oficina> findAllByOrderByInicioDesc();

    List<Oficina> findByInicioBetweenOrderByInicioAsc(LocalDateTime de, LocalDateTime ate);

    List<Oficina> findByPublicadaTrueAndStatusAndInicioAfterOrderByInicioAsc(StatusOficina status, LocalDateTime depois);

    Optional<Oficina> findFirstByStatusAndInicioAfterOrderByInicioAsc(StatusOficina status, LocalDateTime depois);
}
