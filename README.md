<p align="center">
  <b>UNIVERSIDADE CESUMAR – UNICESUMAR</b><br>
  <b>BACHARELADO EM ENGENHARIA DE SOFTWARE</b>
</p>

<br>

<p align="center">
  FELIPE PIVA DA SILVA – 25054343-2<br>
  NICOLLAS AZALIM – 25013758-2<br>
  NIVEA SOFIA DA CAS – 25004647-2<br>
  ROBERTO MÜLLER SANCHES – 25224962-2<br>
  YASMIN FERNANDA DE CARVALHO – 25061121-2
</p>

<br><br>

<h1 align="center">SIMULADOR PRÁTICO DE NÍVEL DE SISTEMA OPERACIONAL</h1>
<h3 align="center">Banco de Alimentos de Londrina</h3>

<br>

<table align="center"><tr><td width="420">
Documentação técnica do código-fonte apresentada à disciplina de Sistemas Operacionais, como parte da segunda etapa da Atividade de Estudo Programado (AEP) do 4º semestre do curso de Engenharia de Software.
</td></tr></table>

<br><br>

<p align="center">
  LONDRINA<br>
  2026
</p>

<br>

---

## SUMÁRIO

- [1 INTRODUÇÃO](#1-introdução)
- [2 REQUISITOS E EXECUÇÃO](#2-requisitos-e-execução)
  - [2.1 Requisitos do ambiente](#21-requisitos-do-ambiente)
  - [2.2 Execução pelo terminal](#22-execução-pelo-terminal)
  - [2.3 Execução pelo Visual Studio Code](#23-execução-pelo-visual-studio-code)
- [3 ESTRUTURA DO PROJETO](#3-estrutura-do-projeto)
- [4 CENÁRIOS DE SIMULAÇÃO](#4-cenários-de-simulação)
  - [4.1 Pipeline do galpão (Produtor/Consumidor)](#41-pipeline-do-galpão-produtorconsumidor)
  - [4.2 Condição de corrida](#42-condição-de-corrida)
  - [4.3 Prevenção de deadlock](#43-prevenção-de-deadlock)
  - [4.4 Substituição de páginas no cache de consultas (LRU × FIFO)](#44-substituição-de-páginas-no-cache-de-consultas-lru--fifo)
- [5 CONSIDERAÇÕES FINAIS](#5-considerações-finais)
- [REFERÊNCIAS](#referências)

---

## 1 INTRODUÇÃO

Este documento descreve o código-fonte do simulador desenvolvido para a segunda entrega da disciplina de Sistemas Operacionais da AEP do 4º semestre, cujo tema é o Banco de Alimentos de Londrina. O simulador dá continuidade ao Relatório de Concorrência e Arquitetura de Processos apresentado na primeira entrega, transformando a modelagem teórica em um programa funcional escrito em Java.

O objetivo do simulador é demonstrar, com primitivas reais de sincronização, a operação concorrente do galpão de recebimento e expedição, evidenciando a prevenção de condições de corrida e de *deadlocks* durante requisições simultâneas. Além disso, o programa simula o comportamento dos algoritmos de substituição de páginas FIFO (*First In, First Out*) e LRU (*Least Recently Used*) aplicados a um cache em memória das consultas de estoque.

As primitivas utilizadas pertencem ao pacote `java.util.concurrent` da plataforma Java: a classe `Semaphore`, que implementa semáforos de contagem, e a classe `ReentrantLock`, que implementa o mutex (ORACLE, 2023). Ambas são executadas pela máquina virtual Java sobre os mecanismos de bloqueio de *threads* do sistema operacional hospedeiro.

## 2 REQUISITOS E EXECUÇÃO

### 2.1 Requisitos do ambiente

Para compilar e executar o simulador é necessário o *Java Development Kit* (JDK) na versão 17 ou superior. Não há dependências externas.

### 2.2 Execução pelo terminal

A partir da pasta raiz do projeto, o código é compilado e executado com os comandos a seguir. O parâmetro final é opcional e permite executar apenas um cenário (`pipeline`, `corrida`, `deadlock` ou `cache`); sem ele, todos os cenários são executados em sequência.

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out Main
java -cp out Main deadlock
```

No Windows, pelo PowerShell, a compilação é feita com:

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
```

Caso os caracteres acentuados apareçam incorretos no console do Windows, recomenda-se executar `chcp 65001` antes do programa ou utilizar `java -Dstdout.encoding=UTF-8 -cp out Main`.

### 2.3 Execução pelo Visual Studio Code

Com a extensão *Extension Pack for Java* instalada, deve-se abrir a pasta raiz do projeto, abrir o arquivo `Main.java` e acionar a opção *Run*, exibida acima do método `main`. Para executar um cenário específico, utilizam-se os comandos da seção 2.2 no terminal integrado.

## 3 ESTRUTURA DO PROJETO

O código está organizado em dois pacotes. O pacote `galpao` contém a simulação de concorrência e o pacote `cache` contém a simulação de substituição de páginas, conforme o Quadro 1.

**Quadro 1 – Organização das classes do simulador**

| Pacote | Classe | Responsabilidade |
|---|---|---|
| (raiz) | `Main` | Ponto de entrada; seleciona o cenário a ser executado |
| `galpao` | `BufferLimitado` | Buffer circular de capacidade fixa com semáforos `vazio` e `cheio` e mutex |
| `galpao` | `Estoque` | Saldo por produto, protegido por mutex (zona crítica) |
| `galpao` | `Lote` | Representa um lote doado, com produto, quantidade e validade |
| `galpao` | `Galpao` | Estado compartilhado da simulação (buffers, estoque e contadores) |
| `galpao` | `ThreadRecepcaoDoacao` | Produtora do Buffer_Triagem |
| `galpao` | `ThreadTriagem` | Consumidora do Buffer_Triagem e produtora do Buffer_Estoque |
| `galpao` | `ThreadExpedicao` | Consumidora do Buffer_Estoque |
| `galpao` | `SimuladorGalpao` | Cenário 1 – pipeline completo |
| `galpao` | `DemoCondicaoCorrida` | Cenário 2 – condição de corrida |
| `galpao` | `DemoDeadlock` | Cenário 3 – deadlock e sua prevenção |
| `galpao` | `Log` | Registro sincronizado das mensagens das *threads* |
| `cache` | `CacheConsultas` | Interface comum aos algoritmos de substituição |
| `cache` | `CacheFIFO` | Implementação do algoritmo FIFO |
| `cache` | `CacheLRU` | Implementação do algoritmo LRU |
| `cache` | `SimuladorCache` | Cenário 4 – comparação entre FIFO e LRU |

Fonte: elaborado pelos autores (2026).

## 4 CENÁRIOS DE SIMULAÇÃO

Os resultados numéricos apresentados nesta seção correspondem a uma execução de referência. Como a ordem de execução das *threads* é definida pelo escalonador do sistema operacional, os valores dos cenários 1 e 2 podem variar entre execuções, sem alterar as conclusões.

### 4.1 Pipeline do galpão (Produtor/Consumidor)

O primeiro cenário implementa o fluxo descrito na seção 5.1.2 da primeira entrega: Recepção → Buffer_Triagem → Triagem → Buffer_Estoque → Expedição. Foram configurados um Buffer_Triagem com capacidade 3, um Buffer_Estoque com capacidade 4, duas *threads* de triagem e duas de expedição, processando 20 lotes doados.

Cada buffer segue a solução clássica do problema Produtor/Consumidor, com um mutex e dois semáforos de contagem (TANENBAUM; BOS, 2016, p. XX). A ordem de aquisição é sempre *down* no semáforo de contagem seguido de *down* no mutex, pois a inversão dessa ordem pode bloquear o sistema permanentemente. Durante a execução, o registro mostra produtores bloqueados quando o Buffer_Estoque está cheio e consumidores bloqueados quando o Buffer_Triagem está vazio. Lotes com validade igual ou anterior à data atual são descartados na triagem, conforme a regra de negócio RN01.

Ao final, o simulador verifica o balanço de unidades. Na execução de referência, foram recebidas 568 unidades, das quais 476 foram expedidas e 92 descartadas, restando saldo zero, e nenhum buffer ultrapassou sua capacidade. Esse resultado indica a ausência de perdas ou duplicações por condição de corrida.

### 4.2 Condição de corrida

O segundo cenário reproduz os riscos identificados na seção 5.1.3 da primeira entrega, executando as mesmas operações sobre o saldo do estoque com e sem mutex. Uma condição de corrida ocorre quando o resultado depende da ordem exata em que os processos acessam dados compartilhados (TANENBAUM; BOS, 2016, p. XX).

No caso da atualização perdida, quatro *threads* de triagem somaram 5.000 unidades cada. Sem o mutex, o saldo final ficou próximo de 5.000 em vez de 20.000; com o mutex, o resultado foi exatamente 20.000. No caso da baixa duplicada, quatro *threads* de expedição retiraram unidades de um saldo de 100. Sem o mutex, foram entregues 103 unidades e o saldo terminou negativo; com o mutex, foram entregues exatamente 100 unidades e o saldo terminou em zero.

### 4.3 Prevenção de deadlock

O terceiro cenário trata do *deadlock* potencial apontado na primeira entrega, que ocorre quando duas *threads* precisam travar os dois buffers ao mesmo tempo em ordens diferentes. Na primeira parte, a *thread* de triagem trava o Buffer_Triagem e aguarda o Buffer_Estoque, enquanto a de expedição faz o inverso. O *deadlock* ocorre de fato e é detectado pela própria máquina virtual por meio da interface `ThreadMXBean`, que identifica a espera circular entre as duas *threads*.

Na segunda parte, aplica-se a prevenção por ordenação global dos recursos: cada buffer recebe um número de ordem e todas as *threads* adquirem os bloqueios em ordem crescente, independentemente da ordem em que a operação os solicita. Essa técnica elimina a condição de espera circular, uma das quatro condições necessárias para a ocorrência de *deadlock* (TANENBAUM; BOS, 2016, p. XX). Na execução de referência, 200 operações de cada lado foram concluídas sem bloqueio.

### 4.4 Substituição de páginas no cache de consultas (LRU × FIFO)

O quarto cenário simula um cache em memória para as consultas de estoque, com número fixo de quadros. Quando o cache está cheio, o algoritmo FIFO remove o lote que está há mais tempo na memória, enquanto o LRU remove o lote que está há mais tempo sem ser consultado (TANENBAUM; BOS, 2016, p. XX). A simulação é dividida em três partes.

A primeira apresenta o passo a passo de uma sequência curta de 13 consultas com 3 quadros, na qual o FIFO apresentou 10 faltas e o LRU, 9. A segunda demonstra a anomalia de Belady: com a sequência utilizada, o FIFO passou de 9 para 10 faltas ao aumentar o cache de 3 para 4 quadros, enquanto o LRU reduziu de 10 para 8. A terceira simula um período de pico com 20.000 consultas sobre 100 lotes, em que 10 lotes concentram 80% dos acessos, representando produtos de alta procura. Os resultados estão no Quadro 2.

**Quadro 2 – Taxa de acerto do cache em período de pico**

| Quadros | FIFO | LRU |
|---|---|---|
| 5 | 30,3% | 31,6% |
| 10 | 51,6% | 57,4% |
| 15 | 63,8% | 74,3% |
| 20 | 70,9% | 80,9% |
| 30 | 78,5% | 84,6% |

Fonte: elaborado pelos autores (2026).

O LRU obteve maior taxa de acerto em todos os tamanhos de cache, pois mantém na memória os lotes consultados com frequência, enquanto o FIFO os remove periodicamente apenas por serem os mais antigos.

## 5 CONSIDERAÇÕES FINAIS

O simulador demonstrou na prática as soluções propostas na primeira entrega. O uso combinado de mutex e semáforos de contagem manteve a integridade do estoque com múltiplos operadores simultâneos, e a ordenação global dos recursos impediu o *deadlock* na operação que envolve os dois buffers. Na comparação entre algoritmos de substituição de páginas, o LRU mostrou-se mais adequado ao cache de consultas de estoque, por explorar a concentração das consultas em poucos produtos e por não estar sujeito à anomalia de Belady.

## REFERÊNCIAS

ORACLE. **Java Platform, Standard Edition 21 API Specification**: package java.util.concurrent. [S. l.]: Oracle, 2023. Disponível em: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/package-summary.html. Acesso em: 28 out. 2026.

TANENBAUM, Andrew S.; BOS, Herbert. **Sistemas operacionais modernos**. 4. ed. São Paulo: Pearson Education do Brasil, 2016.
