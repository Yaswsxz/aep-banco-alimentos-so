package cache;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

/** LRU: expulsa o lote que está há mais tempo SEM ser consultado. */
public class CacheLRU implements CacheConsultas {

    private final int capacidade;
    // Ordem de uso: o primeiro é o menos recentemente usado
    private final LinkedHashSet<Integer> ordemDeUso = new LinkedHashSet<>();
    private Integer vitima;

    public CacheLRU(int capacidade) {
        this.capacidade = capacidade;
    }

    @Override
    public boolean consultar(int idLote) {
        vitima = null;
        if (ordemDeUso.remove(idLote)) {
            ordemDeUso.add(idLote); // acerto: vai para o fim (mais recente)
            return true;
        }
        if (ordemDeUso.size() == capacidade) {
            Iterator<Integer> it = ordemDeUso.iterator();
            vitima = it.next();
            it.remove();
        }
        ordemDeUso.add(idLote);
        return false;
    }

    @Override public Integer ultimaVitima() { return vitima; }
    @Override public List<Integer> conteudo() { return new ArrayList<>(ordemDeUso); }
    @Override public String nome() { return "LRU"; }
    @Override public int capacidade() { return capacidade; }
}
