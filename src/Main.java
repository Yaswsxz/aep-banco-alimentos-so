import cache.SimuladorCache;
import galpao.DemoCondicaoCorrida;
import galpao.DemoDeadlock;
import galpao.SimuladorGalpao;

/**
 * Simulador de SO - Banco de Alimentos de Londrina (AEP 4º semestre, 2ª entrega).
 *
 * Uso: java -cp out Main [pipeline | corrida | deadlock | cache | todos]
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        String opcao = args.length > 0 ? args[0].toLowerCase() : "todos";
        switch (opcao) {
            case "pipeline" -> SimuladorGalpao.executar();
            case "corrida" -> DemoCondicaoCorrida.executar();
            case "deadlock" -> DemoDeadlock.executar();
            case "cache" -> SimuladorCache.executar();
            case "todos" -> {
                SimuladorGalpao.executar();
                DemoCondicaoCorrida.executar();
                DemoDeadlock.executar();
                SimuladorCache.executar();
            }
            default -> System.out.println("Opções: pipeline | corrida | deadlock | cache | todos");
        }
    }
}
