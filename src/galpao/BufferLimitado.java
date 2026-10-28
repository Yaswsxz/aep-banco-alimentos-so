package galpao;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Buffer circular de capacidade fixa (problema Produtor/Consumidor).
 *
 * Primitivas reais do SO, expostas pela JVM:
 *   - vazio : Semaphore de contagem (espaços livres)   -> começa em N
 *   - cheio : Semaphore de contagem (itens disponíveis) -> começa em 0
 *   - mutex : ReentrantLock (exclusão mútua na zona crítica)
 *
 * A ordem é sempre down(contagem) -> down(mutex), como na seção 5.2.2 do relatório:
 * se a thread pegasse o mutex antes e ficasse esperando o semáforo, ninguém mais
 * conseguiria entrar para liberá-lo (deadlock).
 */
public class BufferLimitado<T> {

    private final String nome;
    private final Object[] itens;
    private int posInsercao = 0;
    private int posRemocao = 0;
    private int ocupacao = 0;
    private int maiorOcupacao = 0;

    private final Semaphore vazio;
    private final Semaphore cheio;
    private final ReentrantLock mutex = new ReentrantLock();

    public BufferLimitado(String nome, int capacidade) {
        if (capacidade <= 0) {
            throw new IllegalArgumentException("Capacidade deve ser positiva");
        }
        this.nome = nome;
        this.itens = new Object[capacidade];
        this.vazio = new Semaphore(capacidade, true); // true = fila justa (FIFO de espera)
        this.cheio = new Semaphore(0, true);
    }

    /** Usado pelo produtor. Bloqueia se o buffer estiver cheio. */
    public void inserir(T item) throws InterruptedException {
        if (!vazio.tryAcquire()) {
            Log.info(nome + " CHEIO -> produtor bloqueado no semáforo vazio");
            vazio.acquire();             // down(&vazio): dorme até um consumidor liberar espaço
        }
        mutex.lock();                    // down(&mutex)
        try {
            itens[posInsercao] = item;   // zona crítica
            posInsercao = (posInsercao + 1) % itens.length;
            ocupacao++;
            maiorOcupacao = Math.max(maiorOcupacao, ocupacao);
        } finally {
            mutex.unlock();              // up(&mutex)
        }
        cheio.release();                 // up(&cheio)
    }

    /** Usado pelo consumidor. Bloqueia se o buffer estiver vazio. */
    @SuppressWarnings("unchecked")
    public T remover() throws InterruptedException {
        T item;
        if (!cheio.tryAcquire()) {
            Log.info(nome + " VAZIO -> consumidor bloqueado no semáforo cheio");
            cheio.acquire();             // down(&cheio): dorme até um produtor inserir um item
        }
        mutex.lock();                    // down(&mutex)
        try {
            item = (T) itens[posRemocao]; // zona crítica
            itens[posRemocao] = null;
            posRemocao = (posRemocao + 1) % itens.length;
            ocupacao--;
        } finally {
            mutex.unlock();              // up(&mutex)
        }
        vazio.release();                 // up(&vazio)
        return item;
    }

    public int getOcupacao() {
        mutex.lock();
        try {
            return ocupacao;
        } finally {
            mutex.unlock();
        }
    }

    public int getMaiorOcupacao() {
        mutex.lock();
        try {
            return maiorOcupacao;
        } finally {
            mutex.unlock();
        }
    }

    public int getCapacidade() { return itens.length; }
    public String getNome() { return nome; }
}
