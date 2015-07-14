package org.processmining.plugins.wpp.visualizers;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.plugins.wpp.objects.Output;
 
@Plugin(name = "Algoritmo genético",
        parameterLabels = { "Medias y valores" },
        returnLabels = { "Medias y valores viewer" },
        returnTypes = { JComponent.class },
        userAccessible = false)
@Visualizer
public class OutputVisualizer {

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
                                     final Output gen
                                     ) {
    JPanel pMedias = new JPanel();
    JPanel pDivision = new JPanel();
    JPanel pMix = new JPanel();
    pDivision.setLayout(new GridLayout(gen.getGenetic().getNumAtributos(), gen.getGenetic().getCiudades().size()+1));
    pMedias.setLayout(new GridLayout(gen.getGenetic().getNumAtributos(), 9));
    JButton leyenda = new JButton("Medias");
    leyenda.setBackground(java.awt.Color.darkGray);
    leyenda.setForeground(java.awt.Color.white);
    pMedias.add(leyenda);
    for (int i = 0; i<gen.getGenetic().getNumAtributos()-1; i++) {
      JButton bSequence = new JButton(gen.getGenetic().getAtributos().get(i) +": "+gen.getGenetic().getMedias().get(i).toString());
      bSequence.setBackground(java.awt.Color.darkGray);
      bSequence.setForeground(java.awt.Color.white);
      pMedias.add(bSequence);
    }
    leyenda = new JButton("División");
    leyenda.setBackground(java.awt.Color.darkGray);
    leyenda.setForeground(java.awt.Color.white);
    pDivision.add(leyenda);
    for (int i = 0; i<gen.getGenetic().getCiudades().size(); i++) {
      JButton bSequence = new JButton(gen.getGenetic().getCiudades().get(i) +": "+gen.getGenetic().getDivision().get(i).toString());
      bSequence.setBackground(java.awt.Color.darkGray);
      bSequence.setForeground(java.awt.Color.white);
      pDivision.add(bSequence);
    }
    pMix.setLayout(new BorderLayout());
//    pMix.add(pPetri, BorderLayout.CENTER);
    pMix.add(pMedias, BorderLayout.WEST);
    pMix.add(pDivision, BorderLayout.EAST);
    
 // Always return a single parameter of type JComponent
    return pMix;
  }
}