package galpao;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Cenário 3: o deadlock potencial da seção 5.1.3.
 * Uma operação de remanejamento precisa travar os DOIS buffers ao mesmo tempo:
 *   - Triagem remaneja um lote:      trava Buffer_Triagem e depois Buffer_Estoque
 *   - Expedição devolve para reinspeção: trava Buffer_Estoque e depois Buffer_Triagem
 * Em ordens diferentes, cada uma fica esperando o recurso da outra (espera circular).
 * Prevenção: ordem global fixa de aquisição, quebrando a condição de espera circular.
 */
public class DemoDeadlock {

    /** Recurso com posição fixa na ordem global de aquisição. */
    static class Recurso {
        final String nome;
        final int ordem;
        final ReentrantLock lock = new ReentrantLock();
        Recurso(String nome, int ordem) { this.nome = nome; this.ordem = ordem; }
    }

    private static final Recurso TRIAGEM = new Recurso("Buffer_Triagem", 1);
    private static final Recurso ESTOQUE = new Recurso("Buffer_Estoque", 2);

    public static void executar() throws InterruptedException {
        System.out.println("\n=== CENÁRIO 3: DEADLOCK AO TRAVAR DOIS BUFFERS ===");

        System.out.println("\n(a) SEM prevenção: cada thread trava os buffers em uma ordem diferente");
        Log.reiniciarRelogio();
        Thread t1 = new Thread(() -> semOrdem(TRIAGEM, ESTOQUE, "remanejar lote"), "Triagem-1");
        Thread t2 = new Thread(() -> semOrdem(ESTOQUE, TRIAGEM, "devolver p/ reinspeção"), "Expedicao-1");
        t1.start();
        t2.start();

        Thread.sleep(1000);
        long[] travadas = ManagementFactory.getThreadMXBean().findDeadlockedThreads();
        if (travadas != null) {
            System.out.println("  >>> DEADLOCK DETECTADO pela JVM:");
            ThreadMXBean mx = ManagementFactory.getThreadMXBean();
            for (ThreadInfo info : mx.getThreadInfo(travadas)) {
                System.out.printf("      %s está bloqueada esperando um lock que pertence a %s%n",
                        info.getThreadName(), info.getLockOwnerName());
            }
            System.out.println("  Interrompendo as threads para liberar o sistema...");
        } else {
            System.out.println("  (desta vez o escalonador evitou o deadlock por sorte)");
        }
        t1.interrupt();
        t2.interrupt();
        t1.join();
        t2.join();

        System.out.println("\n(b) COM prevenção: ordem global fixa (Buffer_Triagem sempre antes de Buffer_Estoque)");
        Log.reiniciarRelogio();
        final int repeticoes = 200;
        Thread p1 = new Thread(() -> repetirComOrdem(TRIAGEM, ESTOQUE, repeticoes), "Triagem-1");
        Thread p2 = new Thread(() -> repetirComOrdem(ESTOQUE, TRIAGEM, repeticoes), "Expedicao-1");
        p1.start();
        p2.start();
        p1.join(5000);
        p2.join(5000);
        boolean terminaram = !p1.isAlive() && !p2.isAlive();
        System.out.printf("  %d operações de cada lado, mesmo pedindo os buffers em ordens opostas: %s%n",
                repeticoes, terminaram ? "[OK] todas concluídas, sem deadlock" : "[ERRO] travou");
    }

    /** Versão ingênua: trava na ordem em que a operação "pede". */
    private static void semOrdem(Recurso primeiro, Recurso segundo, String operacao) {
        try {
            primeiro.lock.lockInterruptibly();
            try {
                Log.info("travou " + primeiro.nome + " para " + operacao);
                Thread.sleep(100); // janela em que a outra thread trava o outro buffer
                Log.info("aguardando " + segundo.nome + "...");
                segundo.lock.lockInterruptibly();
                try {
                    Log.info("concluiu " + operacao);
                } finally {
                    segundo.lock.unlock();
                }
            } finally {
                primeiro.lock.unlock();
            }
        } catch (InterruptedException e) {
            Log.info("interrompida, liberando o que segurava");
        }
    }

    /** Versão segura: independentemente do pedido, adquire em ordem crescente. */
    private static void comOrdem(Recurso a, Recurso b) throws InterruptedException {
        Recurso[] ordenados = {a, b};
        Arrays.sort(ordenados, Comparator.comparingInt(r -> r.ordem));
        ordenados[0].lock.lockInterruptibly();
        try {
            Thread.sleep(1);
            ordenados[1].lock.lockInterruptibly();
            try {
                // zona crítica com os dois buffers travados
            } finally {
                ordenados[1].lock.unlock();
            }
        } finally {
            ordenados[0].lock.unlock();
        }
    }

    private static void repetirComOrdem(Recurso a, Recurso b, int vezes) {
        try {
            for (int i = 0; i < vezes; i++) {
                comOrdem(a, b);
            }
            Log.info("concluiu " + vezes + " operações");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
