package sistemasOperacionais;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Random;
import java.util.Arrays;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class SimuluadorDeAlocacaoDeMemoria extends JFrame {

	private final JComboBox<String> menuEstrategia;
	private final DefaultListModel<String>ModeloListaDeProcesso;
	private final java.util.List<BlocoDeMemoria> blocosDeMemoria;
	private final java.util.List<Processos>processos;
	private final JPanel painelDeMemoria;
	private final JButton botaoDeAlocacao, botaoDeReiniciar, botaoSimularES;
	private final JTextField NomeDoCampo, tamanhoDoCampo;
	private JLabel labelStatusDaMemoria;
	private int proximoIndeceDeAjuste = 0;
	
	public SimuluadorDeAlocacaoDeMemoria() {
		super("Simulador de Alocação de Memória");
		setLayout(new BorderLayout());
		menuEstrategia = new JComboBox<>(new String[]{"First Fit","Best Fit", "Worst Fit", "Next Fit"});
		ModeloListaDeProcesso = new DefaultListModel<>();
		processos = new ArrayList<>();
		blocosDeMemoria = new ArrayList<>(Arrays.asList(
				new BlocoDeMemoria(0,100),
				new BlocoDeMemoria(1,150),
				new BlocoDeMemoria(2,200),
				new BlocoDeMemoria(3,250),
				new BlocoDeMemoria(4,300),
				new BlocoDeMemoria(5,350)
				));
		//Painel de entrada
		JPanel inputPanel = new JPanel(new GridLayout(2, 1));
		JPanel PainelDeProcesso = new JPanel();
		PainelDeProcesso.add(new JLabel("Nome:"));
		NomeDoCampo = new JTextField(5);
		PainelDeProcesso.add(new JLabel("Tamanho:"));
		tamanhoDoCampo = new JTextField(5);
		PainelDeProcesso.add(tamanhoDoCampo);
		botaoDeAlocacao = new JButton("Alocar");
		PainelDeProcesso.add(botaoDeAlocacao);
		botaoDeReiniciar = new JButton ("Reiniciar");
		PainelDeProcesso.add(botaoDeReiniciar);
		botaoSimularES = new JButton ("Simular E/S bloqueante");
		PainelDeProcesso.add(botaoSimularES);
		inputPanel.add(PainelDeProcesso);
		JPanel painelDeEstrategia = new JPanel();
		painelDeEstrategia.add(new JLabel("Estratégia:"));
		painelDeEstrategia.add(menuEstrategia);
		inputPanel.add(painelDeEstrategia);
		add(inputPanel, BorderLayout.NORTH);
		//Painel de memória
		painelDeMemoria = new JPanel() { 
			protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			desenharBlocosDeMemória(g);
			}
		};
		painelDeMemoria.setPreferredSize(new Dimension(600, 400));
		add(painelDeMemoria, BorderLayout.CENTER);
		//Lista de processos
		JList<String> processList = new JList<>(ModeloListaDeProcesso);
		add(new JScrollPane(processList), BorderLayout.EAST);
		//Exibição do status da memória 
		labelStatusDaMemoria = new JLabel("Memória Total: 0KB | Ocupados: 0KB | Livre: 0KB");
		add(labelStatusDaMemoria, BorderLayout.SOUTH);
		
		//Ações
		botaoDeAlocacao.addActionListener(e -> {
			String nome = NomeDoCampo.getText().trim();
			int tamanho;
			try {
				tamanho = Integer.parseInt(tamanhoDoCampo.getText().trim());
			}catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(this,"Tamanho inválido");
				return;
			}
		
		String strategy = (String) menuEstrategia.getSelectedItem();	
		Processos p = new Processos(nome,tamanho);
		processos.add(p);
		
		boolean sucess = switch (strategy) {
		case "First Fit" -> alocarFirstFit(p);
		case "Best Fit" -> alocarBestFit(p);
		case "Worst Fit" -> alocarWorstFit(p);
		case "Next Fit" -> alocarNextFit(p);
		default -> false;
		};
		if(sucess) {
			ModeloListaDeProcesso.addElement(p.toString());
			repaint();
		}else {
			JOptionPane.showMessageDialog(this,"Não foi possível alocar o processo.");
		}
		atualizarStatusDaMemoria();
		});
		botaoDeReiniciar.addActionListener(e->{
			processos.clear();
			ModeloListaDeProcesso.clear();
			blocosDeMemoria.forEach(BlocoDeMemoria::limpo);
			proximoIndeceDeAjuste = 0;
			atualizarStatusDaMemoria();
			repaint();
		});
		botaoSimularES.addActionListener(e->{
			if(processos.isEmpty()) {
				JOptionPane.showMessageDialog(this,"Nenhum processo em execução.");
				return;
			}
			new Thread(()->{
				Processos p = processos.get(new Random().nextInt(processos.size()));
				p.bloqueado = true;
				repaint();
				try {
					Thread.sleep(3000);
				}catch (InterruptedException ignored) {}
				p.bloqueado = false;
				repaint();
				}).start();
				
		});
		pack();
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setVisible(true);
 }
	private boolean alocarFirstFit(Processos p) {
		for(BlocoDeMemoria bloco : blocosDeMemoria) {
			if(bloco.estaLivre() && bloco.tamanho>= p.tamanho) {
					bloco.alocar(p);
				return true;
			}
		}
		 return false;
	}
	private boolean alocarBestFit(Processos p) {
		BlocoDeMemoria melhor = null;
		for(BlocoDeMemoria bloco : blocosDeMemoria) {
			if(bloco.estaLivre() && bloco.tamanho >= p.tamanho) {
				if(melhor == null || bloco.tamanho < melhor.tamanho) {
					melhor = bloco;
				}
			}
		}
		if(melhor != null) {
			melhor.alocar(p);
			return true;
		}
		else return false;
	}
	private boolean alocarWorstFit(Processos p ) {
		BlocoDeMemoria pior = null;
		for(BlocoDeMemoria bloco : blocosDeMemoria) {
			if(bloco.estaLivre() && bloco.tamanho >= p.tamanho) {
				if (pior == null || bloco.tamanho > bloco.tamanho) {
					pior = bloco;
			}
		}
	 }
	 if (pior != null) {
		 pior.alocar(p);
		  return true;
	  }
	  return false;
}
private boolean alocarNextFit(Processos p) {
	int n = blocosDeMemoria.size(); 
	for (int i = 0; i<n; i++) {
		int index = (proximoIndeceDeAjuste + i) %n;
		BlocoDeMemoria bloco = blocosDeMemoria.get(index);
		if(bloco.estaLivre() && bloco.tamanho >=p.tamanho) {
			bloco.alocar(p);
			proximoIndeceDeAjuste = (index + 1) %n;
			return true;
			}
		}
		return false;
}
 private void desenharBlocosDeMemória(Graphics g) {
	 int y = 20;
	 for(BlocoDeMemoria bloco : blocosDeMemoria) {
		 g.setColor(bloco.processos == null ? Color.LIGHT_GRAY : (bloco.processos.bloqueado ? 
				 Color.orange : Color.GREEN 
				 ));
		 g.fillRect(50, y,200, 40);
		 g.setColor(Color.BLACK);
		 g.drawRect(50, y, 200, 40);
		 g.drawString("Bloco" + bloco.id + ": " + bloco.tamanho + "KB" , 60, y + 15);
		 if (bloco.processos != null) {
			 g.drawString(bloco.processos.nome + " (" + bloco.processos.tamanho + "KB)", 60, y + 35);
		 }
		 y +=60;
	 }
 }
 
 private void atualizarStatusDaMemoria() {
	 int memoriaTotal = 0;
	 int memoriaUsada = 0;
	 for (BlocoDeMemoria block : blocosDeMemoria) {
		 memoriaTotal += block.tamanho;
		 if (block.processos != null) {
			 memoriaUsada += block.tamanho;
		 }
	 }
	 int memoriaLivre = memoriaTotal - memoriaUsada;
	 labelStatusDaMemoria.setText("Memória: Total: " + memoriaTotal + "KB | Ocupdao:" +memoriaUsada + "KB | Livre: " + memoriaLivre + "KB");
 }
 public static void main(String[]args) {
	 SwingUtilities.invokeLater(SimuluadorDeAlocacaoDeMemoria :: new);
 }
 static class BlocoDeMemoria {
	 int id, tamanho;
	 Processos processos;
	 BlocoDeMemoria (int id,int tamanho) {
		 this.id = id;
		 this.tamanho = tamanho;
	 }
	 boolean estaLivre() {
		 return processos== null;
	 }
	 void alocar (Processos p) {
		 this.processos = p;
 }
	 void limpo( ) {
		 this.processos = null;
}
}
 static class Processos {
	 String nome; 
	 int tamanho;
	 boolean bloqueado = false;
	 
	 Processos(String nome, int tamanho){
		 this.nome = nome;
		 this.tamanho = tamanho;
	 }
	 public String toString() {
		 return nome + " (" + tamanho + "KB)" + (bloqueado ? " [BLOQUEADO" : "");
	 }
 }
}
