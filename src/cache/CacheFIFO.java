package cache;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** FIFO: expulsa o lote que está há mais tempo no cache, mesmo que seja muito consultado. */
public class CacheFIFO implements CacheConsultas {

    private final int capacidade;
    private final ArrayDeque<Integer> filaDeChegada = new ArrayDeque<>();
    private final Set<Integer> presentes = new HashSet<>();
    private Integer vitima;

    public CacheFIFO(int capacidade) {
        this.capacidade = capacidade;
    }

    @Override
    public boolean consultar(int idLote) {
        vitima = null;
        if (presentes.contains(idLote)) {
            return true; // acerto: a ordem de chegada NÃO muda
        }
        if (filaDeChegada.size() == capacidade) {
            vitima = filaDeChegada.pollFirst();
            presentes.remove(vitima);
        }
        filaDeChegada.addLast(idLote);
        presentes.add(idLote);
        return false;
    }

    @Override public Integer ultimaVitima() { return vitima; }
    @Override public List<Integer> conteudo() { return new ArrayList<>(filaDeChegada); }
    @Override public String nome() { return "FIFO"; }
    @Override public int capacidade() { return capacidade; }
}
