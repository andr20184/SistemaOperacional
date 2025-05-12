package sistemasOperacionais;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.util.*;
import java.util.List;

public class SimuluadorDeAlocacaoDeMemoria extends JFrame {
   
	private final JComboBox<String> menuEstrategia;
    private final DefaultListModel<String> ModeloListaDeProcesso;
    private final java.util.List<BlocoDeMemoria> blocosDeMemoria;
    private final java.util.List<Processos> processos;
    private final JPanel painelDeMemoria;
    private final JButton botaoDeAlocacao, botaoDeReiniciar, botaoSimularES;
    private final JTextField NomeDoCampo, tamanhoDoCampo;
    private JLabel labelStatusDaMemoria;
    private final DefaultTableModel ModeloTabela;
    private static final int tamanhoDaPagina= 50;
    private int contagemDeFalhasDePagina = 0;

    
    private int nextFitIndex = 0;

    public SimuluadorDeAlocacaoDeMemoria() {
        super("Simulador de Alocação de Memória");
    
        setLayout(new BorderLayout());
    
        menuEstrategia = new JComboBox<>(new String[]{
            "First Fit", "Best Fit", "Worst Fit", "Next Fit"
        });
    
        ModeloListaDeProcesso = new DefaultListModel<>();
        processos = new ArrayList<>();
    
        blocosDeMemoria = new ArrayList<>(Arrays.asList(
            new BlocoDeMemoria(0, 100),
            new BlocoDeMemoria(1, 150),
            new BlocoDeMemoria(2, 200),
            new BlocoDeMemoria(3, 250),
            new BlocoDeMemoria(4, 300),
            new BlocoDeMemoria(5, 350)
        ));
    
        // Painel de entrada
        JPanel painelDeEntrada = new JPanel(new GridLayout(2, 1));
        JPanel PainelDeProcessos = new JPanel();
    
        PainelDeProcessos.add(new JLabel("Nome:"));
        NomeDoCampo = new JTextField(5);
        PainelDeProcessos.add(NomeDoCampo);
    
        PainelDeProcessos.add(new JLabel("Tamanho:"));
        tamanhoDoCampo = new JTextField(5);
        PainelDeProcessos.add(tamanhoDoCampo);
    
        PainelDeProcessos.add(new JLabel("Prioridade:"));
        JTextField priorityField = new JTextField(3);
        PainelDeProcessos.add(priorityField);
    
        botaoDeAlocacao = new JButton("Alocar");
        PainelDeProcessos.add(botaoDeAlocacao);
    
        botaoDeReiniciar = new JButton("Reiniciar");
        PainelDeProcessos.add(botaoDeReiniciar);
    
        botaoSimularES = new JButton("Simular E/S bloqueante");
        PainelDeProcessos.add(botaoSimularES);
    
        painelDeEntrada.add(PainelDeProcessos);
    
        JPanel painelDeMetodo = new JPanel();
        painelDeMetodo.add(new JLabel("Estratégia:"));
        painelDeMetodo.add(menuEstrategia);
        painelDeEntrada.add(painelDeMetodo);
    
        add(painelDeEntrada, BorderLayout.NORTH);
    
        // Painel de memória
        painelDeMemoria = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawMemoryBlocks(g);
            }
        };
        painelDeMemoria.setPreferredSize(new Dimension(600, 400));
        add(painelDeMemoria, BorderLayout.CENTER);
    
        // Lista de processos
        JList<String> listaDeProcessos = new JList<>(ModeloListaDeProcesso);
        add(new JScrollPane(listaDeProcessos), BorderLayout.EAST);
    
        // Tabela de estados
        String[] colunaDeNomes = {"Nome", "Prioridade", "Estado"};
        ModeloTabela = new DefaultTableModel(colunaDeNomes, 0);
        JTable stateTable = new JTable(ModeloTabela);
        add(new JScrollPane(stateTable), BorderLayout.WEST);
    
        // Status da memória
        labelStatusDaMemoria = new JLabel("Memória: Total: 0KB | Ocupado: 0KB | Livre: 0KB");
        add(labelStatusDaMemoria, BorderLayout.SOUTH);
    
        // Ações dos botões
        botaoDeAlocacao.addActionListener(e -> {
            String nome = NomeDoCampo.getText().trim();
            int tamnaho, priridade;
            try {
                tamnaho = Integer.parseInt(tamanhoDoCampo.getText().trim());
                priridade = Integer.parseInt(priorityField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Tamanho ou prioridade inválido.");
                return;
            }
    
            String strategy = (String) menuEstrategia.getSelectedItem();
            Processos p = new Processos(nome, tamnaho, priridade);
            processos.add(p);
    
            boolean success = switch (strategy) {
                case "First Fit" -> alocarFirstFit(p);
                case "Best Fit" -> alocarBestFit(p);
                case "Worst Fit" -> alocarWorstFit(p);
                case "Next Fit" -> alocarNextFit(p);
                default -> false;
            };
    
            if (success) {
                p.estado = StatusDoProcesso.PRONTO;
                ModeloListaDeProcesso.addElement(p.toString());
                atualizarTabelaDeProcessos();
                repaint();
            } else {
                JOptionPane.showMessageDialog(this, "Não foi possível alocar o processo.");
            }
    
            atualiarStatusDeMemoria();
        });
    
        botaoDeReiniciar.addActionListener(e -> {
            processos.clear();
            ModeloListaDeProcesso.clear();
            blocosDeMemoria.forEach(BlocoDeMemoria::limpo);
            nextFitIndex = 0;
            atualizarTabelaDeProcessos();
            atualiarStatusDeMemoria();
            repaint();
        });
    
        botaoSimularES.addActionListener(e -> {
            if (processos.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nenhum processo em execução.");
                return;
            }
            new Thread(() -> {
                Processos p = processos.get(new Random().nextInt(processos.size()));
                if (p.estado == StatusDoProcesso.EXECUTANDO || p.estado == StatusDoProcesso.PRONTO) {
                    p.bloqueado = true;
                    p.estado = StatusDoProcesso.BLOQUEADO;
                    atualizarTabelaDeProcessos();
                    repaint();
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ignored) {}
                    p.bloqueado = false;
                    p.estado = StatusDoProcesso.PRONTO;
                    atualizarTabelaDeProcessos();
                    repaint();
                }
            }).start();
        });
    
        pack();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    
        // Inicia o escalonador automático
        startScheduler();
    }

    private boolean alocarFirstFit(Processos p) {
        return alocarPorOrdem(p, blocosDeMemoria);
    }

    private boolean alocarBestFit(Processos p) {
        int paginasAlocadas = 0;
        for (Pagina pagina : p.paginas) {
            boolean alocada = false;
            for (BlocoDeMemoria bloco : blocosDeMemoria) {
                if (bloco.temQuadroLivre()) {
                    bloco.addPagina(pagina);
                    alocada = true;
                    break;
                }
            }
            if (!alocada) {
            	contagemDeFalhasDePagina++;
                return false; // Falha de alocação
            }
            paginasAlocadas++;
        }
        return true;
    }

    private boolean alocarWorstFit(Processos p) {
        List<BlocoDeMemoria> ordenado = new ArrayList<>(blocosDeMemoria);
        ordenado.sort((b1, b2) -> Integer.compare(b2.tamanho, b1.tamanho));
        return alocarPorOrdem(p, ordenado);
    }

    private boolean alocarNextFit(Processos p) {
        int startIndex = nextFitIndex;
        for (Pagina pagina : p.paginas) {
            boolean alocada = false;
            int tentativas = 0;
            while (tentativas < blocosDeMemoria.size()) {
                BlocoDeMemoria bloco = blocosDeMemoria.get(nextFitIndex);
                if (bloco.temQuadroLivre()) {
                    bloco.addPagina(pagina);
                    alocada = true;
                    break;
                }
                nextFitIndex = (nextFitIndex + 1) % blocosDeMemoria.size();
                tentativas++;
            }
            if (!alocada) {
                contagemDeFalhasDePagina++;
                return false;
            }
            nextFitIndex = (nextFitIndex + 1) % blocosDeMemoria.size();
        }
        return true;
    }

    private void startScheduler() {
    new Thread(() -> {
        while (true) {
            List<Processos> processosProntos = new ArrayList<>();
            for (Processos p : processos) {
                if (!p.bloqueado && p.estado != StatusDoProcesso.FINALIZADO) {
                    processosProntos.add(p);
                }
            }

            // Escalonamento por prioridade (menor número = maior prioridade)
            processosProntos.sort(Comparator.comparingInt(p -> p.prioridade));

            for (Processos p : processosProntos) {
                p.estado = StatusDoProcesso.EXECUTANDO;
                atualizarTabelaDeProcessos();
                repaint();
                try {
                    Thread.sleep(2000); // tempo de execução simulado
                } catch (InterruptedException ignored) {}
                p.estado = StatusDoProcesso.PRONTO;
            }

            atualizarTabelaDeProcessos();
            repaint();

            try {
                Thread.sleep(500); // pausa entre ciclos
            } catch (InterruptedException ignored) {}
        }
    }).start();
}

    private void drawMemoryBlocks(Graphics g) {
        int y = 20;
        for (BlocoDeMemoria block : blocosDeMemoria) {
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(50, y, 200, 40);
            g.setColor(Color.BLACK);
            g.drawRect(50, y, 200, 40);
            g.drawString("Bloco " + block.id + ": " + block.tamanho + "KB", 60, y + 15);
            int py = y + 20;
            for (Pagina pagina : block.paginas) {
                g.setColor(pagina.processo.bloqueado ? Color.ORANGE : Color.GREEN);
                g.fillRect(60, py, 180, 10);
                g.setColor(Color.BLACK);
                g.drawRect(60, py, 180, 10);
                g.drawString(pagina.processo.nome + " P" + pagina.NumeroDaPagina, 65, py + 9);
                py += 12;
            }
            y += 60;
        }
    }
    private void atualizarTabelaDeProcessos() {
        SwingUtilities.invokeLater(() -> {
            ModeloTabela.setRowCount(0);
            for (Processos p : processos) {
                ModeloTabela.addRow(new Object[]{p.nome, p.prioridade, p.estado});
            }
        });
    }

    private void atualiarStatusDeMemoria() {
        int memoriaTotal = 0;
        int memoriaUsada = 0;

        for (BlocoDeMemoria block : blocosDeMemoria) {
            memoriaTotal += block.tamanho;
            memoriaUsada += block.paginas.size() * tamanhoDaPagina;
            }
        

        int memoriaLivre = memoriaTotal - memoriaUsada;
        labelStatusDaMemoria.setText(
        	    "Memória: Total: " + memoriaTotal + "KB | Ocupado: " + memoriaUsada + "KB | Livre: " + memoriaLivre + "KB | Paginas Faltantes: " + contagemDeFalhasDePagina
        	);
    }
    
    private boolean alocarPorOrdem(Processos p, List<BlocoDeMemoria> blocosOrdenados) {
        for (Pagina pagina : p.paginas) {
            boolean alocada = false;
            for (BlocoDeMemoria bloco : blocosOrdenados) {
                if (bloco.temQuadroLivre()) {
                    bloco.addPagina(pagina);
                    alocada = true;
                    break;
                }
            }
            if (!alocada) {
                contagemDeFalhasDePagina++;
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SimuluadorDeAlocacaoDeMemoria::new);
    }

    // Classes auxiliares

    static class BlocoDeMemoria {
        int id, tamanho;
        List<Pagina> paginas = new ArrayList<>();

        BlocoDeMemoria(int id, int tamanho) {
            this.id = id;
            this.tamanho = tamanho;
        }

        boolean temQuadroLivre() {
            return paginas.size() < (tamanho / tamanhoDaPagina);
        }

        void addPagina(Pagina p) {
            paginas.add(p);
        }

        void limpo() {
            paginas.clear();
        }

        boolean containsProcess(Processos p) {
            return paginas.stream().anyMatch(pg -> pg.processo == p);
        }
       }  
    
    static class Pagina {
        Processos processo;
        int NumeroDaPagina;

        Pagina(Processos processo, int NumeroDaPagina) {
            this.processo = processo;
            this.NumeroDaPagina = NumeroDaPagina;
        }
    }
    enum StatusDoProcesso {
        NOVO, PRONTO, EXECUTANDO, BLOQUEADO, FINALIZADO
    }
    
    static class Processos {
        String nome;
        int tamanho;
        boolean bloqueado = false;
        int prioridade;
        StatusDoProcesso estado = StatusDoProcesso.NOVO;
        List<Pagina> paginas = new ArrayList<>();

        Processos(String nome, int tamanho, int prioridade) {
            this.nome = nome;
            this.tamanho = tamanho;
            this.prioridade = prioridade;
            int numPaginas = (int) Math.ceil(tamanho / (double) tamanhoDaPagina);
            for (int i = 0; i < numPaginas; i++) {
                paginas.add(new Pagina(this, i));
            }
        }

        public String toString() {
            return nome + " (" + tamanho + "KB, P=" + prioridade + ") [" + estado + "]";
        }
    }
}
