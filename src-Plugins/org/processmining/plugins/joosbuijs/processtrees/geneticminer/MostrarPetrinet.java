package org.processmining.plugins.joosbuijs.processtrees.geneticminer;

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class MostrarPetrinet extends Frame implements ActionListener {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  TextField text = new TextField(20);
  Button b;
  private int numClicks = 0;
  private String title;
  private JPanel panel;
  private JComponent candidato;

  public MostrarPetrinet(String title, JPanel panel, JComponent candidato) {

    super(title);
    this.title = title;
    setLayout(new FlowLayout());
    b = new Button("Click me");
    add(b);
    add(text);
    b.addActionListener(this);
    this.panel = panel;
    this.candidato = candidato;
  }

  public void actionPerformed(ActionEvent e) {
    panel.remove(1);
    panel.add(candidato,1);
    panel.revalidate();
    panel.repaint();
    
//    panel.setVisible(false);
//    panel.setVisible(true);
    numClicks++;
    System.out.println(title + "Button Clicked " + numClicks + " times");
  }

  public JPanel getPanel() {
    return panel;
  }

  public void setPanel(JPanel panel) {
    this.panel = panel;
  }

  public JComponent getCandidato() {
    return candidato;
  }

  public void setCandidato(JComponent candidato) {
    this.candidato = candidato;
  }

}