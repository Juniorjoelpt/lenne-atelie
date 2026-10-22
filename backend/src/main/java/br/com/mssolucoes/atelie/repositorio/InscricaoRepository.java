package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.Enums.StatusInscricao;
import br.com.mssolucoes.atelie.dominio.Inscricao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {

    List<Inscricao> findByOficinaIdOrderByCriadoEmAsc(Long oficinaId);

    @EntityGraph(attributePaths = "oficina")
    java.util.Optional<Inscricao> findByCodigo(String codigo);

    boolean existsByOficinaIdAndWhatsappAndStatusNot(Long oficinaId, String whatsapp, StatusInscricao status);

    @EntityGraph(attributePaths = "oficina")
    List<Inscricao> findByStatusInOrderByCriadoEmAsc(Collection<StatusInscricao> status);

    @EntityGraph(attributePaths = "oficina")
    List<Inscricao> findByStatusOrderByCriadoEmAsc(StatusInscricao status);

    @EntityGraph(attributePaths = "oficina")
    List<Inscricao> findByClienteIdOrderByCriadoEmDesc(Long clienteId);

    long countByOficinaIdAndStatusIn(Long oficinaId, Collection<StatusInscricao> status);

    long countByOficinaIdAndStatus(Long oficinaId, StatusInscricao status);

    long countByStatus(StatusInscricao status);
}
