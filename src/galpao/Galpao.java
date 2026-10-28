package galpao;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/** Estado compartilhado do galpão: os dois buffers, o estoque e os contadores da simulação. */
public class Galpao {

    final BufferLimitado<Lote> bufferTriagem;
    final BufferLimitado<Lote> bufferEstoque;
    final Estoque estoque = new Estoque();
    final LocalDate hoje = LocalDate.now();

    final int numTriagem;
    final int numExpedicao;
    final AtomicInteger triagensAtivas;

    final AtomicInteger recebidos = new AtomicInteger();
    final AtomicInteger unidadesRecebidas = new AtomicInteger();
    final AtomicInteger aprovados = new AtomicInteger();
    final AtomicInteger descartados = new AtomicInteger();
    final AtomicInteger unidadesDescartadas = new AtomicInteger();
    final AtomicInteger expedidos = new AtomicInteger();
    final AtomicInteger unidadesExpedidas = new AtomicInteger();

    public Galpao(int capacidadeTriagem, int capacidadeEstoque, int numTriagem, int numExpedicao) {
        this.bufferTriagem = new BufferLimitado<>("Buffer_Triagem", capacidadeTriagem);
        this.bufferEstoque = new BufferLimitado<>("Buffer_Estoque", capacidadeEstoque);
        this.numTriagem = numTriagem;
        this.numExpedicao = numExpedicao;
        this.triagensAtivas = new AtomicInteger(numTriagem);
    }

    static void pausa(int minMs, int maxMs) throws InterruptedException {
        Thread.sleep(minMs + (long) (Math.random() * (maxMs - minMs)));
    }
}
