package galpao;

/** Log sincronizado: evita que mensagens de threads diferentes se misturem na saída. */
public final class Log {
    private static long inicio = System.currentTimeMillis();

    private Log() { }

    public static void reiniciarRelogio() {
        inicio = System.currentTimeMillis();
    }

    public static synchronized void info(String mensagem) {
        long t = System.currentTimeMillis() - inicio;
        System.out.printf("[%5d ms] %-20s %s%n", t, Thread.currentThread().getName(), mensagem);
    }
}
