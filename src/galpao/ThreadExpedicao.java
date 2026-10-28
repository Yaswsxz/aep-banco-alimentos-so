package galpao;

/** Consumidora do Buffer_Estoque: separa e despacha os lotes para as instituições parceiras. */
public class ThreadExpedicao implements Runnable {

    private static final String[] INSTITUICOES = {
            "Creche Arco-Íris", "Casa de Acolhimento Esperança", "Cozinha Solidária Vila Nova"
    };

    private final Galpao galpao;

    public ThreadExpedicao(Galpao galpao) {
        this.galpao = galpao;
    }

    @Override
    public void run() {
        try {
            int n = 0;
            while (true) {
                Lote lote = galpao.bufferEstoque.remover();
                if (lote == Lote.FIM) {
                    break;
                }
                galpao.estoque.retirar(lote.getProduto(), lote.getQuantidade());
                Galpao.pausa(120, 220); // separação e carregamento
                galpao.expedidos.incrementAndGet();
                galpao.unidadesExpedidas.addAndGet(lote.getQuantidade());
                String destino = INSTITUICOES[(n++ + lote.getId()) % INSTITUICOES.length];
                Log.info("expediu " + lote + " -> " + destino);
            }
            Log.info("encerrou a expedição");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
