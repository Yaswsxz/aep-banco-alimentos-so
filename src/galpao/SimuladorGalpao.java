package galpao;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Cenário 1: pipeline completo Recepção -> Triagem -> Expedição com múltiplos operadores. */
public class SimuladorGalpao {

    private static final String[] PRODUTOS = {"Arroz", "Feijão", "Leite", "Macarrão", "Tomate", "Banana"};
    private static final String[] DOADORES = {"Mercado Bom Preço", "Atacadão Norte", "Sítio São José"};

    public static void executar() throws InterruptedException {
        final int capTriagem = 3;   // M: espaço físico das docas
        final int capEstoque = 4;   // N: capacidade de armazenamento
        final int numTriagem = 2;   // operadores de triagem simultâneos
        final int numExpedicao = 2; // operadores de expedição simultâneos
        final int numDoacoes = 20;

        System.out.println("\n=== CENÁRIO 1: PIPELINE DO GALPÃO (Produtor/Consumidor com 2 buffers) ===");
        System.out.printf("Buffer_Triagem = %d | Buffer_Estoque = %d | %d triagem | %d expedição | %d doações%n%n",
                capTriagem, capEstoque, numTriagem, numExpedicao, numDoacoes);

        Galpao galpao = new Galpao(capTriagem, capEstoque, numTriagem, numExpedicao);
        List<Lote> doacoes = gerarDoacoes(galpao, numDoacoes);

        List<Thread> threads = new ArrayList<>();
        threads.add(new Thread(new ThreadRecepcaoDoacao(galpao, doacoes), "Recepcao"));
        for (int i = 1; i <= numTriagem; i++) {
            threads.add(new Thread(new ThreadTriagem(galpao), "Triagem-" + i));
        }
        for (int i = 1; i <= numExpedicao; i++) {
            threads.add(new Thread(new ThreadExpedicao(galpao), "Expedicao-" + i));
        }

        Log.reiniciarRelogio();
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }

        imprimirResumo(galpao);
    }

    private static List<Lote> gerarDoacoes(Galpao galpao, int quantidade) {
        Random r = new Random(42); // semente fixa: mesmos lotes em toda execução
        List<Lote> lotes = new ArrayList<>();
        for (int i = 1; i <= quantidade; i++) {
            // a cada 6 lotes, um chega vencido para exercitar o descarte (RN01)
            int dias = (i % 6 == 0) ? -(1 + r.nextInt(5)) : 3 + r.nextInt(60);
            lotes.add(new Lote(i,
                    PRODUTOS[r.nextInt(PRODUTOS.length)],
                    5 + r.nextInt(46),
                    galpao.hoje.plusDays(dias),
                    DOADORES[r.nextInt(DOADORES.length)]));
        }
        return lotes;
    }

    private static void imprimirResumo(Galpao g) {
        int recebidas = g.unidadesRecebidas.get();
        int expedidas = g.unidadesExpedidas.get();
        int descartadas = g.unidadesDescartadas.get();
        int saldoFinal = g.estoque.total();

        System.out.println("\n--- Resumo ---");
        System.out.printf("Lotes recebidos: %d | aprovados: %d | descartados: %d | expedidos: %d%n",
                g.recebidos.get(), g.aprovados.get(), g.descartados.get(), g.expedidos.get());
        System.out.printf("Unidades: recebidas %d = expedidas %d + descartadas %d + saldo %d%n",
                recebidas, expedidas, descartadas, saldoFinal);
        System.out.printf("Maior ocupação Buffer_Triagem: %d/%d | Buffer_Estoque: %d/%d%n",
                g.bufferTriagem.getMaiorOcupacao(), g.bufferTriagem.getCapacidade(),
                g.bufferEstoque.getMaiorOcupacao(), g.bufferEstoque.getCapacidade());

        boolean balancoOk = recebidas == expedidas + descartadas + saldoFinal;
        boolean limitesOk = g.bufferTriagem.getMaiorOcupacao() <= g.bufferTriagem.getCapacidade()
                && g.bufferEstoque.getMaiorOcupacao() <= g.bufferEstoque.getCapacidade();
        System.out.println(balancoOk ? "[OK] Nenhuma unidade perdida ou duplicada (sem condição de corrida)"
                : "[ERRO] Balanço inconsistente!");
        System.out.println(limitesOk ? "[OK] Nenhum buffer passou da capacidade (semáforos funcionando)"
                : "[ERRO] Buffer estourou a capacidade!");
        System.out.println("[OK] Todas as threads terminaram (sem deadlock)");
    }
}
