package cache;

import java.util.List;

/** Cache em memória das consultas de estoque, com capacidade fixa de "quadros" (páginas). */
public interface CacheConsultas {

    /** @return true se foi acerto (hit); false se foi falta (miss) e precisou ir ao banco. */
    boolean consultar(int idLote);

    /** Lote expulso na última consulta, ou null se ninguém saiu. */
    Integer ultimaVitima();

    List<Integer> conteudo();

    String nome();

    int capacidade();
}
