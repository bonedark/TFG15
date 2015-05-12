package org.processmining.plugins.wpp.implementations;

import java.util.ArrayList;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.ui.widgets.ProMComboBox;
import org.processmining.framework.util.ui.widgets.ProMPropertiesPanel;
import org.processmining.framework.util.ui.widgets.ProMTextArea;
import org.processmining.plugins.wpp.objects.Genetic;
import org.processmining.plugins.wpp.objects.Output;

import weka.core.Instances;

//Esta anotacion indica que esta clase es un plugin
@Plugin(name = "Algoritmo genético",
        parameterLabels = { "Arff Instances" },
        returnLabels = { "Medias y valores" },
        returnTypes = { Output.class })
/**
 * @author Mauro
 * Simple plug-in allowing two persons to have a child
 */
public class GeneticPlugin {
  
  /*
   * The main idea of the plug-in is to expose two variants: one does the actual work and 
   * one populates the configurations.  
   * The 1st variant do the actual work, takes all parameters and the configuration,
   * and the other takes all parameters except the configuration. 
   * 
   * If a plug-in returns multiple parameters, they should be returned in an 
   * Object[14] array
   */

  @UITopiaVariant(affiliation = "Universidad de la Laguna",
      author = "Daniel Nicolás Fernández del Castilllo Salazar",
      email = "danibone@icloud.com",
      uiLabel = UITopiaVariant.USEPLUGIN)
  @PluginVariant(requiredParameterLabels = { 0 })
  public static Output build(final UIPluginContext context,
                                 final Instances arff) {
    
    GeneticConfiguration config = new GeneticConfiguration(arff);
    config = populate(context, arff, config);
    Genetic genetic = new Genetic(arff, config.isDebug(), config.getDividendo(), config.getDivisor(), config.getNumAtributos());
    return new Output(genetic);
  }
  
  /* Populate es el que crea y lanza las ventanas de prom para pedir una configuracion
   * al usuario.
   * You always want to only use widgets from either of these sources.  
   * You never want to use a JPanel and manually set the colors or any other old-fashioned 
   * hacks performed in ProM.  If a particular widget you need is not available, add it to 
   * the Widgets package and use it, never create your own local widget.
   * 
   * If a plug-in has more settings than sensible fit on a single page, the Widgets packet 
   * also provides a ProMWizard which should be used. (This is not the case)
   */
  public static GeneticConfiguration populate(final UIPluginContext context, 
                                  final Instances arff,
          									      final GeneticConfiguration config) {
    
	  ProMPropertiesPanel panel = new ProMPropertiesPanel("Configure Genetic Algorith");
	  
    //JButton b = SlickerFactory.instance().createButton("prueba");
    //panel.add(b);
	  
	  ArrayList<String> attrExtra = config.getAttributes();
	  attrExtra.remove(0);
    ProMComboBox dividendo = new ProMComboBox(attrExtra);
    dividendo.setToolTipText("Atributos a relacionar");
    panel.addProperty("Atributo", dividendo);
    
    
    /*
    attrExtra.add(0, "All");
    */
    ProMComboBox filterAttr = new ProMComboBox(attrExtra);
    filterAttr.setToolTipText("Atributos a relacionar");
    panel.addProperty("Atributos a relacionar", filterAttr);
 
	  ProMTextArea data = new ProMTextArea(false);
    data.setText(arff.toString());
    //SlickerTabbedPane tabP = SlickerFactory.instance().createTabbedPane("Dataset");
    //tabP.add(data);
    panel.addProperty("Datos de polución", data);
    /*
	  ProMTextField minSupport = panel.addTextField("Min. Support (0.5-0.9): ", 
	      Double.toString(config.getSupport()));
	  ProMTextField idData = panel.addTextField("Sequence ID number: ",
	  		Integer.toString(config.getIdData()));
	  ProMTextField filter = panel.addTextField("Filtering Attribute: ", 
        config.getFilterAttribute());
	  */
	  final InteractionResult interactionResult = context.showConfiguration("Setups", panel);
	  
	  if (interactionResult == InteractionResult.FINISHED ||
			  interactionResult == InteractionResult.CONTINUE ||
			  interactionResult == InteractionResult.NEXT) {
		  config.setDividendo(dividendo.getSelectedIndex());
		  config.setDivisor(filterAttr.getSelectedIndex());
		  return config;
	  }
	  //Este metodo populate retorna null si l configuracion fue cancelada
	  return null;
  }
}
