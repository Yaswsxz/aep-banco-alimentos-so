package galpao;

import java.time.LocalDate;

/** Lote de um produto recebido em uma doação (entidade Lote do modelo lógico). */
public class Lote {

    /** "Pílula de veneno": sinaliza às threads consumidoras que não há mais itens. */
    public static final Lote FIM = new Lote(-1, "FIM", 0, LocalDate.MAX, "-");

    private final int id;
    private final String produto;
    private final int quantidade;
    private final LocalDate validade;
    private final String doador;

    public Lote(int id, String produto, int quantidade, LocalDate validade, String doador) {
        this.id = id;
        this.produto = produto;
        this.quantidade = quantidade;
        this.validade = validade;
        this.doador = doador;
    }

    /** RN01: validade menor ou igual à data atual não pode entrar no estoque. */
    public boolean isVencido(LocalDate hoje) {
        return !validade.isAfter(hoje);
    }

    public int getId() { return id; }
    public String getProduto() { return produto; }
    public int getQuantidade() { return quantidade; }
    public LocalDate getValidade() { return validade; }
    public String getDoador() { return doador; }

    @Override
    public String toString() {
        return "Lote#" + id + " (" + produto + ", " + quantidade + " un, val. " + validade + ")";
    }
}
