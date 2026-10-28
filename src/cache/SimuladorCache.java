package cache;

import java.util.Locale;
import java.util.Random;

/** Cenário 4: comparação LRU x FIFO no cache das consultas de estoque. */
public class SimuladorCache {

    private static final double CUSTO_HIT_MS = 0.5;   // leitura em memória
    private static final double CUSTO_MISS_MS = 20.0; // ida ao banco de dados

    public static void executar() {
        System.out.println("\n=== CENÁRIO 4: SUBSTITUIÇÃO DE PÁGINAS NO CACHE DE CONSULTAS (LRU x FIFO) ===");
        rastreamento();
        anomaliaDeBelady();
        cargaRealista();
    }

    /** (a) Passo a passo com poucas consultas, para mostrar a diferença de decisão. */
    private static void rastreamento() {
        int[] consultas = {7, 0, 1, 2, 0, 3, 0, 4, 2, 3, 0, 3, 2};
        int quadros = 3;
        CacheConsultas fifo = new CacheFIFO(quadros);
        CacheConsultas lru = new CacheLRU(quadros);

        System.out.printf("%n(a) Passo a passo, cache de %d quadros (números = id do lote consultado)%n", quadros);
        System.out.printf("  %-8s | %-12s %-4s %-6s | %-12s %-4s %-6s%n",
                "Consulta", "FIFO", "", "Saiu", "LRU", "", "Saiu");
        int faltasFifo = 0, faltasLru = 0;
        for (int lote : consultas) {
            boolean hf = fifo.consultar(lote);
            boolean hl = lru.consultar(lote);
            if (!hf) faltasFifo++;
            if (!hl) faltasLru++;
            System.out.printf("  %-8s | %-12s %-4s %-6s | %-12s %-4s %-6s%n",
                    "Lote " + lote,
                    fifo.conteudo(), hf ? "hit" : "MISS", vit(fifo),
                    lru.conteudo(), hl ? "hit" : "MISS", vit(lru));
        }
        System.out.printf("  Total de faltas -> FIFO: %d | LRU: %d (em %d consultas)%n",
                faltasFifo, faltasLru, consultas.length);
    }

    /** (b) FIFO pode piorar ao ganhar memória; LRU não (é um algoritmo de pilha). */
    private static void anomaliaDeBelady() {
        int[] consultas = {1, 2, 3, 4, 1, 2, 5, 1, 2, 3, 4, 5};
        System.out.println("\n(b) Anomalia de Belady: mais quadros deveriam dar menos faltas...");
        for (int quadros = 3; quadros <= 4; quadros++) {
            System.out.printf("  %d quadros -> FIFO: %2d faltas | LRU: %2d faltas%n", quadros,
                    contarFaltas(new CacheFIFO(quadros), consultas),
                    contarFaltas(new CacheLRU(quadros), consultas));
        }
        System.out.println("  O FIFO teve MAIS faltas com mais memória; o LRU não sofre essa anomalia.");
    }

    /** (c) Carga de pico: poucos lotes "quentes" concentram a maioria das consultas. */
    private static void cargaRealista() {
        final int totalLotes = 100;
        final int lotesQuentes = 10;     // ex.: arroz, feijão, leite...
        final double fracaoQuente = 0.8; // 80% das consultas caem neles
        final int numConsultas = 20000;

        int[] consultas = new int[numConsultas];
        Random r = new Random(2026);
        for (int i = 0; i < numConsultas; i++) {
            consultas[i] = r.nextDouble() < fracaoQuente
                    ? r.nextInt(lotesQuentes)
                    : lotesQuentes + r.nextInt(totalLotes - lotesQuentes);
        }

        System.out.printf("%n(c) Pico de requisições: %d consultas, %d lotes, %d lotes recebem %.0f%% das consultas%n",
                numConsultas, totalLotes, lotesQuentes, fracaoQuente * 100);
        System.out.printf("  Custo estimado: hit = %.1f ms (memória), miss = %.1f ms (banco)%n",
                CUSTO_HIT_MS, CUSTO_MISS_MS);
        System.out.printf("  %-8s | %-14s %-12s | %-14s %-12s%n",
                "Quadros", "FIFO acerto", "tempo (s)", "LRU acerto", "tempo (s)");
        for (int quadros : new int[]{5, 10, 15, 20, 30}) {
            int ff = contarFaltas(new CacheFIFO(quadros), consultas);
            int fl = contarFaltas(new CacheLRU(quadros), consultas);
            System.out.printf(Locale.forLanguageTag("pt-BR"), "  %-8d | %-14s %-12.1f | %-14s %-12.1f%n",
                    quadros,
                    pct(ff, numConsultas), tempo(ff, numConsultas),
                    pct(fl, numConsultas), tempo(fl, numConsultas));
        }
    }

    private static int contarFaltas(CacheConsultas cache, int[] consultas) {
        int faltas = 0;
        for (int lote : consultas) {
            if (!cache.consultar(lote)) faltas++;
        }
        return faltas;
    }

    private static String vit(CacheConsultas c) {
        return c.ultimaVitima() == null ? "" : String.valueOf(c.ultimaVitima());
    }

    private static String pct(int faltas, int total) {
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f%%", 100.0 * (total - faltas) / total);
    }

    private static double tempo(int faltas, int total) {
        return ((total - faltas) * CUSTO_HIT_MS + faltas * CUSTO_MISS_MS) / 1000.0;
    }
}
