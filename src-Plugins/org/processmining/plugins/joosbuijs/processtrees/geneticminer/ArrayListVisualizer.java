package org.processmining.plugins.joosbuijs.processtrees.geneticminer;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.plugins.petrinet.PetriNetVisualization;
 
@Plugin(name = "Algoritmo genético",
        parameterLabels = { "Medias y valores" },
        returnLabels = { "Medias y valores viewer" },
        returnTypes = { JComponent.class },
        userAccessible = false)
@Visualizer
public class ArrayListVisualizer {


  /*
   * Here, we just return a passive JComponent object, but it is perfectly ok for a 
   * visualizer to include active elements including event handlers. Visualizers can
   * present different views of the same object (e.g., zooming in or making various 
   * projections) and are allowed to contain a reference to the original object. 
   * Visualizers should not, however, ever change the provided object.Visualizers are 
   * allowed to create new objects based on the input.
   * 
   * Again, we have made the visualizer static as a single visualizer may display multiple 
   * objects,and may even be instantiated multiple times for the same object. 
   */
  @PluginVariant(requiredParameterLabels = { 0 })
  public static JComponent visualize(final PluginContext context,
                                     final List<PetriFitness> gen
                                     ) {
    PetriNetVisualization pnv = new PetriNetVisualization();
    JComponent candidatos = new JPanel();
    JComponent pPetri = pnv.visualize(context, gen.get(0).getPetri());
    JPanel pMix = new JPanel(new GridLayout(1,2));
    candidatos.setLayout(new GridLayout(4, 1));
    JPanel pBotones = new JPanel();
    pBotones.setLayout(new GridLayout(gen.size(), 1));
    DecimalFormat df = new DecimalFormat("#.####");
    for (int i = 0; i<gen.size();i++) {
      JButton bSequence = new JButton(i+" Score:"+df.format(gen.get(gen.size()-1-i).getCoincidencias()));
//      bSequence.setSize(200, 100);
      bSequence.setBackground(java.awt.Color.white);
      bSequence.setForeground(java.awt.Color.black);
      bSequence.addActionListener(new MostrarPetrinet(i+" Score:"+gen.get(gen.size()-1-i).getCoincidencias(),pMix,pnv.visualize(context, gen.get(gen.size()-1-i).getPetri())));
      pBotones.add(bSequence);
    }
//    pMix.add(pnv.visualize(context, gen.get(0)), BorderLayout.CENTER);
//    pMix.add(pnv.visualize(context, gen.get(1)), BorderLayout.CENTER);
    pMix.add(pBotones, BorderLayout.WEST,0);
    pMix.add(pPetri, BorderLayout.EAST,1);
    
    
 // Always return a single parameter of type JComponent
    return pMix;
  }
}