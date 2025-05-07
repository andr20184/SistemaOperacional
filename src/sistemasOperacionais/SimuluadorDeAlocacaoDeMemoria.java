package sistemasOperacionais;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.*;
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
                p.state = ProcessState.PRONTO;
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
            blocosDeMemoria.forEach(BlocoDeMemoria::clear);
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
                if (p.state == ProcessState.EXECUTANDO || p.state == ProcessState.PRONTO) {
                    p.blocked = true;
                    p.state = ProcessState.BLOQUEADO;
                    atualizarTabelaDeProcessos();
                    repaint();
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ignored) {}
                    p.blocked = false;
                    p.state = ProcessState.PRONTO;
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
        for (BlocoDeMemoria block : blocosDeMemoria) {
            if (block.isFree() && block.size >= p.size) {
                block.allocate(p);
                return true;
            }
        }
        return false;
    }

    private boolean alocarBestFit(Processos p) {
        BlocoDeMemoria best = null;
        for (BlocoDeMemoria block : blocosDeMemoria) {
            if (block.isFree() && block.size >= p.size) {
                if (best == null || block.size < best.size) {
                    best = block;
                }
            }
        }
        if (best != null) {
            best.allocate(p);
            return true;
        }
        return false;
    }

    private boolean alocarWorstFit(Processos p) {
        BlocoDeMemoria worst = null;
        for (BlocoDeMemoria block : blocosDeMemoria) {
            if (block.isFree() && block.size >= p.size) {
                if (worst == null || block.size > worst.size) {
                    worst = block;
                }
            }
        }
        if (worst != null) {
            worst.allocate(p);
            return true;
        }
        return false;
    }

    private boolean alocarNextFit(Processos p) {
        int n = blocosDeMemoria.size();
        for (int i = 0; i < n; i++) {
            int index = (nextFitIndex + i) % n;
            BlocoDeMemoria block = blocosDeMemoria.get(index);
            if (block.isFree() && block.size >= p.size) {
                block.allocate(p);
                nextFitIndex = (index + 1) % n;
                return true;
            }
        }
        return false;
    }

    private void startScheduler() {
    new Thread(() -> {
        while (true) {
            List<Processos> readyProcesses = new ArrayList<>();
            for (BlocoDeMemoria block : blocosDeMemoria) {
                if (block.process != null && !block.process.blocked && block.process.state != ProcessState.FINALIZADO) {
                    readyProcesses.add(block.process);
                }
            }

            // Escalonamento por prioridade (menor número = maior prioridade)
            readyProcesses.sort(Comparator.comparingInt(p -> p.priority));

            for (Processos p : readyProcesses) {
                p.state = ProcessState.EXECUTANDO;
                atualizarTabelaDeProcessos();
                repaint();
                try {
                    Thread.sleep(2000); // tempo de execução simulado
                } catch (InterruptedException ignored) {}
                p.state = ProcessState.PRONTO;
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
            g.setColor(block.process == null ? Color.LIGHT_GRAY : (block.process.blocked ? Color.ORANGE : Color.GREEN));
            g.fillRect(50, y, 200, 40);
            g.setColor(Color.BLACK);
            g.drawRect(50, y, 200, 40);
            g.drawString("Bloco " + block.id + ": " + block.size + "KB", 60, y + 15);
            if (block.process != null) {
                g.drawString(block.process.name + " (" + block.process.size + "KB)", 60, y + 35);
            }
            y += 60;
        }
    }
    private void atualizarTabelaDeProcessos() {
        SwingUtilities.invokeLater(() -> {
            ModeloTabela.setRowCount(0);
            for (Processos p : processos) {
                ModeloTabela.addRow(new Object[]{p.name, p.priority, p.state});
            }
        });
    }

    private void atualiarStatusDeMemoria() {
        int totalMemory = 0;
        int usedMemory = 0;

        for (BlocoDeMemoria block : blocosDeMemoria) {
            totalMemory += block.size;
            if (block.process != null) {
                usedMemory += block.size;
            }
        }

        int freeMemory = totalMemory - usedMemory;
        labelStatusDaMemoria.setText("Memória: Total: " + totalMemory + "KB | Ocupado: " + usedMemory + "KB | Livre: " + freeMemory + "KB");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SimuluadorDeAlocacaoDeMemoria::new);
    }

    // Classes auxiliares

    static class BlocoDeMemoria {
        int id, size;
        Processos process;

        BlocoDeMemoria(int id, int size) {
            this.id = id;
            this.size = size;
        }

        boolean isFree() {
            return process == null;
        }

        void allocate(Processos p) {
            this.process = p;
        }

        void clear() {
            this.process = null;
        }
    }
    enum ProcessState {
        NOVO, PRONTO, EXECUTANDO, BLOQUEADO, FINALIZADO
    }
    
    static class Processos {
        String name;
        int size;
        boolean blocked = false;
        int priority;// menor valor = maior prioridade
        ProcessState state = ProcessState.NOVO;

        Processos(String name, int size, int priority) {
            this.name = name;
            this.size = size;
            this.priority = priority;
        }

        public String toString() {
            return name + " (" + size + "KB, P=" + priority + ") [" + state + "]";
        }
        
    }
}
