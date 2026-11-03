package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.MaterialAmostra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialAmostraRepository extends JpaRepository<MaterialAmostra, Long> {
    List<MaterialAmostra> findByMaterialIdOrderByNomeNoAscCriadoEmDesc(Long materialId);

    @org.springframework.data.jpa.repository.Query("select a.material.id, count(a) from MaterialAmostra a group by a.material.id")
    List<Object[]> contarPorMaterial();
}
