package galpao;

/** Consumidora do Buffer_Triagem e produtora do Buffer_Estoque. */
public class ThreadTriagem implements Runnable {

    private final Galpao galpao;

    public ThreadTriagem(Galpao galpao) {
        this.galpao = galpao;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Lote lote = galpao.bufferTriagem.remover();
                if (lote == Lote.FIM) {
                    break;
                }
                Galpao.pausa(40, 100); // conferência de validade e integridade

                if (lote.isVencido(galpao.hoje)) {
                    galpao.descartados.incrementAndGet();
                    galpao.unidadesDescartadas.addAndGet(lote.getQuantidade());
                    Log.info("DESCARTOU " + lote + " -> vencido (RN01), registrar perda");
                    continue;
                }

                // Primeiro grava o saldo (base de dados), depois disponibiliza o lote fisicamente.
                galpao.estoque.adicionar(lote.getProduto(), lote.getQuantidade());
                galpao.bufferEstoque.inserir(lote);
                galpao.aprovados.incrementAndGet();
                Log.info("aprovou " + lote + " | Buffer_Estoque: "
                        + galpao.bufferEstoque.getOcupacao() + "/" + galpao.bufferEstoque.getCapacidade());
            }
            // A última triagem a terminar avisa todas as expedições
            if (galpao.triagensAtivas.decrementAndGet() == 0) {
                for (int i = 0; i < galpao.numExpedicao; i++) {
                    galpao.bufferEstoque.inserir(Lote.FIM);
                }
            }
            Log.info("encerrou a triagem");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
