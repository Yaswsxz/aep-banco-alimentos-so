package galpao;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Saldo de estoque por produto (a "base de dados", zona crítica da seção 5.1.2).
 * Triagem soma, Expedição subtrai, e o mutex impede a atualização perdida.
 */
public class Estoque {

    private final Map<String, Integer> saldo = new HashMap<>();
    private final ReentrantLock mutex = new ReentrantLock();

    public void adicionar(String produto, int quantidade) {
        mutex.lock();
        try {
            saldo.merge(produto, quantidade, Integer::sum);
        } finally {
            mutex.unlock();
        }
    }

    public void retirar(String produto, int quantidade) {
        mutex.lock();
        try {
            int atual = saldo.getOrDefault(produto, 0);
            if (atual < quantidade) {
                throw new IllegalStateException("Estoque insuficiente de " + produto
                        + ": saldo " + atual + ", pedido " + quantidade);
            }
            saldo.put(produto, atual - quantidade);
        } finally {
            mutex.unlock();
        }
    }

    public int total() {
        mutex.lock();
        try {
            return saldo.values().stream().mapToInt(Integer::intValue).sum();
        } finally {
            mutex.unlock();
        }
    }

    /** Leitura consistente para indicadores (evita a "inconsistência de leitura" da seção 5.1.3). */
    public Map<String, Integer> snapshot() {
        mutex.lock();
        try {
            return new TreeMap<>(saldo);
        } finally {
            mutex.unlock();
        }
    }
}
