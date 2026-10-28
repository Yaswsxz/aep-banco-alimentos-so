package galpao;

import java.util.List;

/** Produtora do Buffer_Triagem: registra as doações que chegam às docas. */
public class ThreadRecepcaoDoacao implements Runnable {

    private final Galpao galpao;
    private final List<Lote> doacoes;

    public ThreadRecepcaoDoacao(Galpao galpao, List<Lote> doacoes) {
        this.galpao = galpao;
        this.doacoes = doacoes;
    }

    @Override
    public void run() {
        try {
            for (Lote lote : doacoes) {
                Galpao.pausa(20, 60); // tempo de descarga do caminhão
                galpao.bufferTriagem.inserir(lote);
                galpao.recebidos.incrementAndGet();
                galpao.unidadesRecebidas.addAndGet(lote.getQuantidade());
                Log.info("recebeu " + lote + " de " + lote.getDoador()
                        + " | Buffer_Triagem: " + galpao.bufferTriagem.getOcupacao()
                        + "/" + galpao.bufferTriagem.getCapacidade());
            }
            // Uma pílula de fim para cada thread de triagem
            for (int i = 0; i < galpao.numTriagem; i++) {
                galpao.bufferTriagem.inserir(Lote.FIM);
            }
            Log.info("fim das doações do dia");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
