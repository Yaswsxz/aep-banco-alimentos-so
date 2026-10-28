package galpao;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Cenário 2: mostra os riscos da seção 5.1.3 acontecendo de verdade
 * (versão sem mutex) e sendo eliminados (versão com mutex).
 */
public class DemoCondicaoCorrida {

    interface Saldo {
        void adicionar(int q);
        boolean retirarSeHouver(int q);
        int valor();
    }

    /** Sem sincronização: ler -> calcular -> gravar pode ser interrompido no meio. */
    static class SaldoSemMutex implements Saldo {
        private int saldo;
        SaldoSemMutex(int inicial) { saldo = inicial; }

        public void adicionar(int q) {
            int lido = saldo;
            Thread.yield();          // troca de contexto entre a leitura e a escrita
            saldo = lido + q;
        }

        public boolean retirarSeHouver(int q) {
            if (saldo >= q) {        // checagem...
                Thread.yield();      // ...outra thread retira no meio...
                saldo -= q;          // ...e esta retira também
                return true;
            }
            return false;
        }

        public int valor() { return saldo; }
    }

    /** Com mutex: a operação inteira vira uma zona crítica. */
    static class SaldoComMutex implements Saldo {
        private int saldo;
        private final ReentrantLock mutex = new ReentrantLock();
        SaldoComMutex(int inicial) { saldo = inicial; }

        public void adicionar(int q) {
            mutex.lock();
            try {
                int lido = saldo;
                Thread.yield();
                saldo = lido + q;
            } finally {
                mutex.unlock();
            }
        }

        public boolean retirarSeHouver(int q) {
            mutex.lock();
            try {
                if (saldo >= q) {
                    Thread.yield();
                    saldo -= q;
                    return true;
                }
                return false;
            } finally {
                mutex.unlock();
            }
        }

        public int valor() {
            mutex.lock();
            try { return saldo; } finally { mutex.unlock(); }
        }
    }

    private static final int OPERADORES = 4;
    private static final int LOTES_POR_OPERADOR = 5000;
    private static final int SALDO_INICIAL_EXPEDICAO = 100;

    public static void executar() throws InterruptedException {
        System.out.println("\n=== CENÁRIO 2: CONDIÇÃO DE CORRIDA NO SALDO DO ESTOQUE ===");

        System.out.printf("%n(a) Atualização perdida: %d operadores de triagem somando %d unidades cada%n",
                OPERADORES, LOTES_POR_OPERADOR);
        int esperado = OPERADORES * LOTES_POR_OPERADOR;
        for (int rodada = 1; rodada <= 3; rodada++) {
            int obtido = triagemConcorrente(new SaldoSemMutex(0));
            System.out.printf("  SEM mutex, rodada %d: esperado %d, obtido %d -> %d unidades sumiram%n",
                    rodada, esperado, obtido, esperado - obtido);
        }
        int seguro = triagemConcorrente(new SaldoComMutex(0));
        System.out.printf("  COM mutex:            esperado %d, obtido %d -> %s%n",
                esperado, seguro, seguro == esperado ? "correto" : "ERRO");

        System.out.printf("%n(b) Baixa duplicada: saldo de %d unidades, %d operadores de expedição retirando%n",
                SALDO_INICIAL_EXPEDICAO, OPERADORES);
        for (int rodada = 1; rodada <= 3; rodada++) {
            Saldo s = new SaldoSemMutex(SALDO_INICIAL_EXPEDICAO);
            int entregues = expedicaoConcorrente(s);
            System.out.printf("  SEM mutex, rodada %d: %d unidades entregues, saldo final %d%s%n",
                    rodada, entregues, s.valor(),
                    entregues > SALDO_INICIAL_EXPEDICAO ? " -> entregou o que não existia!" : "");
        }
        Saldo s = new SaldoComMutex(SALDO_INICIAL_EXPEDICAO);
        int entregues = expedicaoConcorrente(s);
        System.out.printf("  COM mutex:            %d unidades entregues, saldo final %d -> %s%n",
                entregues, s.valor(),
                entregues == SALDO_INICIAL_EXPEDICAO && s.valor() == 0 ? "correto" : "ERRO");
    }

    private static int triagemConcorrente(Saldo saldo) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < OPERADORES; i++) {
            threads.add(new Thread(() -> {
                for (int j = 0; j < LOTES_POR_OPERADOR; j++) {
                    saldo.adicionar(1);
                }
            }, "Triagem-" + (i + 1)));
        }
        rodar(threads);
        return saldo.valor();
    }

    private static int expedicaoConcorrente(Saldo saldo) throws InterruptedException {
        AtomicInteger entregues = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < OPERADORES; i++) {
            threads.add(new Thread(() -> {
                for (int j = 0; j < SALDO_INICIAL_EXPEDICAO; j++) {
                    if (saldo.retirarSeHouver(1)) {
                        entregues.incrementAndGet();
                    }
                }
            }, "Expedicao-" + (i + 1)));
        }
        rodar(threads);
        return entregues.get();
    }

    private static void rodar(List<Thread> threads) throws InterruptedException {
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
    }
}
