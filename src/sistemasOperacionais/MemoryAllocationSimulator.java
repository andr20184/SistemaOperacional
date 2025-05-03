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

public class MemoryAllocationSimulator extends JFrame {

	private final JComboBox<String> strategyComboBox;
	private final DefaultListModel<String>processListModel;
	private final java.util.List<MemoryBlock> memoryBlocks;
	private final java.util.List<Process>processes;
	private final JPanel memoryPanel;
	private final JButton allocateButton, resetButton, simulateIOButton;
	private final JTextField nameField, sizeField;
	private JLabel memoryStatusLabel;
	private int nextFitIndex = 0;
	
	public MemoryAllocationSimulator() {
		super("Simulador de Alocação de Memória");
		setLayout(new BorderLayout());
		strategyComboBox = new JComboBox<>(new String[]{"First Fit","Best Fit", "Worst Fit", "Next Fit"});
		processListModel = new DefaultListModel<>();
		processes = new ArrayList<>();
		memoryBlocks = new ArrayList<>(Arrays.asList(
				new MemoryBlock(0,100),
				new MemoryBlock(1,150),
				new MemoryBlock(2,200),
				new MemoryBlock(3,250),
				new MemoryBlock(4,300),
				new MemoryBlock(5,350)
				));
		//Painel de entrada
		JPanel inputPanel = new JPanel(new GridLayout(2, 1));
		JPanel ProcessPanel = new JPanel();
		ProcessPanel.add(new JLabel("Nome:"));
		nameField = new JTextField(5);
		ProcessPanel.add(new JLabel("Tamanho:"));
		sizeField = new JTextField(5);
		ProcessPanel.add(sizeField);
		allocateButton = new JButton("Alocar");
		ProcessPanel.add(allocateButton);
		resetButton = new JButton ("Reiniciar");
		ProcessPanel.add(resetButton);
		simulateIOButton = new JButton ("Simular E/S bloqueante");
		ProcessPanel.add(simulateIOButton);
		inputPanel.add(ProcessPanel);
		JPanel strategyPanel = new JPanel();
		strategyPanel.add(new JLabel("Estratégia:"));
		strategyPanel.add(strategyComboBox);
		inputPanel.add(strategyPanel);
		add(inputPanel, BorderLayout.NORTH);
		//Painel de memória
		memoryPanel = new JPanel() { 
			protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			drawMemoryBlocks(g);
			}
		};
		memoryPanel.setPreferredSize(new Dimension(600, 400));
		add(memoryPanel, BorderLayout.CENTER);
		//Lista de processos
		JList<String> processList = new JList<>(processListModel);
		add(new JScrollPane(processList), BorderLayout.EAST);
		//Exibição do status da memória 
		memoryStatusLabel = new JLabel("Memória Total: 0KB | Ocupados: 0KB | Livre: 0KB");
		add(memoryStatusLabel, BorderLayout.SOUTH);
		
		//Ações
		allocateButton.addActionListener(e -> {
			String name = nameField.getText().trim();
			int size;
			try {
				size = Integer.parseInt(sizeField.getText().trim());
			}catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(this,"Tamanho inválid");
				return;
			}
		
		String strategy = (String) strategyComboBox.getSelectedItem();	
		Process p = new Process(name,size);
		processes.add(p);
		
		boolean sucess = switch (strategy) {
		case "First Fit" -> allocateFirstFit(p);
		case "Best Fit" -> allocateBestFit(p);
		case "Worst Fit" -> allocateWorstFit(p);
		case "Next Fit" -> allocateNextFit(p);
		default -> false;
		};
		if(sucess) {
			processListModel.addElement(p.toString());
			repaint();
		}else {
			JOptionPane.showMessageDialog(this,"Não foi possível alocar o processo.");
		}
		updateMemoryStatus();
		});
		resetButton.addActionListener(e->{
			processes.clear();
			processListModel.clear();
			memoryBlocks.forEach(MemoryBlock::clear);
			nextFitIndex = 0;
			updateMemoryStatus();
			repaint();
		});
		simulateIOButton.addActionListener(e->{
			if(processes.isEmpty()) {
				JOptionPane.showMessageDialog(this,"Nenhum processo em execução.");
				return;
			}
			new Thread(()->{
				Process p = processes.get(new Random().nextInt(processes.size()));
				p.blocked = true;
				repaint();
				try {
					Thread.sleep(3000);
				}catch (InterruptedException ignored) {}
				p.blocked = false;
				repaint();
				}).start();
				
		});
		pack();
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setVisible(true);
 }
	private boolean allocateFirstFit(Process p) {
		for(MemoryBlock block : memoryBlocks) {
			if(block.isFree() && block.size>= p.size) {
					block.allocate(p);
				return true;
			}
		}
		 return false;
	}
	private boolean allocateBestFit(Process p) {
		MemoryBlock best = null;
		for(MemoryBlock block : memoryBlocks) {
			if(block.isFree() && block.size >= p.size) {
				if(best == null || block.size < best.size) {
					best = block;
				}
			}
		}
		if(best != null) {
			best.allocate(p);
			return true;
		}
		else return false;
	}
	private boolean allocateWorstFit(Process p ) {
		MemoryBlock worst = null;
		for(MemoryBlock block : memoryBlocks) {
			if(block.isFree() && block.size >= p.size) {
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
private boolean allocateNextFit(Process p) {
	int n = memoryBlocks.size(); 
	for (int i = 0; i<n; i++) {
		int index = (nextFitIndex + i) %n;
		MemoryBlock block = memoryBlocks.get(index);
		if(block.isFree() && block.size >=p.size) {
			block.allocate(p);
			nextFitIndex = (index + 1) %n;
			return true;
			}
		}
		return false;
}
 private void drawMemoryBlocks(Graphics g) {
	 int y = 20;
	 for(MemoryBlock block : memoryBlocks) {
		 g.setColor(block.processes == null ? Color.LIGHT_GRAY : (block.processes.blocked ? 
				 Color.orange : Color.GREEN 
				 ));
		 g.fillRect(50, y,200, 40);
		 g.setColor(Color.BLACK);
		 g.drawRect(50, y, 200, 40);
		 g.drawString("Bloco" + block.id + ": " + block.size + "KB" , 60, y + 15);
		 if (block.processes != null) {
			 g.drawString(block.processes.name + " (" + block.processes.size + "KB)", 60, y + 35);
		 }
		 y +=60;
	 }
 }
 
 private void updateMemoryStatus() {
	 int totalMemory = 0;
	 int usedMemory = 0;
	 for (MemoryBlock block : memoryBlocks) {
		 totalMemory += block.size;
		 if (block.processes != null) {
			 usedMemory += block.size;
		 }
	 }
	 int freeMemory = totalMemory - usedMemory;
	 memoryStatusLabel.setText("Memória: Total: " + totalMemory + "KB | Ocupdao:" +usedMemory + "KB | Livre: " + freeMemory + "KB");
 }
 public static void main(String[]args) {
	 SwingUtilities.invokeLater(MemoryAllocationSimulator :: new);
 }
 static class MemoryBlock {
	 int id, size;
	 Process processes;
	 MemoryBlock (int id,int size) {
		 this.id = id;
		 this.size = size;
	 }
	 boolean isFree() {
		 return processes== null;
	 }
	 void allocate (Process p) {
		 this.processes = p;
 }
	 void clear( ) {
		 this.processes = null;
}
}
 static class Process {
	 String name; 
	 int size;
	 boolean blocked = false;
	 
	 Process(String name, int size){
		 this.name = name;
		 this.size = size;
	 }
	 public String toString() {
		 return name + " (" + size + "KB)" + (blocked ? " [BLOQUEADO" : "");
	 }
 }
}
