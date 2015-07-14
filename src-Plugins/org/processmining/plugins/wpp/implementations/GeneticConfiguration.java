package org.processmining.plugins.wpp.implementations;

import java.util.ArrayList;
import java.util.Enumeration;

import weka.core.Instances;


public class GeneticConfiguration {
  private ArrayList<String> attributes;
  private boolean debug;
  private int dividendo;
  private int divisor;
  private int numAtributos;
  
  public GeneticConfiguration(int i, int f) {
    setDebug(false);
    setDividendo(i);
    setDivisor(f);
  }

  public GeneticConfiguration(Instances instances) {
    findAttributes(instances);
    setDebug(false);
    setDividendo(0);
    setDivisor(0);
  }

  protected void findAttributes(Instances instances) {
    ArrayList<String> attr = new ArrayList<String>();
    Enumeration e = instances.enumerateAttributes();
    int i = 0;
    try {
      while (i != -1) {
        String cad = e.nextElement().toString();
        String cads[] = cad.split(" ");
        attr.add(cads[1]);
        i++;
        }
    } catch (Exception exec) {
      setAttributes(attr);
      setNumAtributos(i);
    }
  }
  public ArrayList<String> getAttributes() {
    return attributes;
  }

  protected void setAttributes(ArrayList<String> attributes) {
    this.attributes = attributes;
  }

  public boolean isDebug() {
    return debug;
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  public int getDividendo() {
    return dividendo;
  }

  public void setDividendo(int dividendo) {
    this.dividendo = dividendo;
  }

  public int getDivisor() {
    return divisor;
  }

  public void setDivisor(int divisor) {
    this.divisor = divisor;
  }

  public int getNumAtributos() {
    return numAtributos;
  }

  public void setNumAtributos(int numAtributos) {
    this.numAtributos = numAtributos;
  }
  
}
