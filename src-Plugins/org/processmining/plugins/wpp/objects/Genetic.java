package org.processmining.plugins.wpp.objects;

import java.util.ArrayList;
import java.util.List;

import org.processmining.framework.annotations.AuthoredType;
import org.processmining.framework.annotations.Icon;

import weka.core.Attribute;
import weka.core.Instances;

/**
 * @author Mauro This class have the frequent sequential patterns
 */
@AuthoredType(typeName = "Genetic Alg", affiliation = "Universidad de la Laguna", author = "Daniel Nicolás Fernández del Castilllo Salazar", email = "danibone@icloud.com")
@Icon(icon = "./resources/resourcetype_gsp_30x35.png")
public class Genetic {
  private List<Double> medias;
  private List<Double> division;
  private List<String> atributos;
  private List<String> ciudades;
  private int numAtributos;
  private boolean debug;
  private int dividendo;
  private int divisor;

  public Genetic(Instances instances, boolean d, int i, int f, int a, ArrayList<String> atritutos) {
    setDebug(d);
    setAtributos(atritutos);
    setDividendo(i);
    setDivisor(f);
    setNumAtributos(a);
    runAlgorith(instances);
  }

  public Genetic(Instances instances) {
    setDebug(false);
    setDividendo(0);
    setDivisor(0);
    runAlgorith(instances);
  }

  protected void runAlgorith(Instances instances) {
    double temp = 0;
    medias =  new ArrayList<Double>();
    division =  new ArrayList<Double>();
    ciudades =  new ArrayList<String>();
    Attribute att = instances.attribute(0);
    for (int i = 0; i < att.numValues(); i++) {
      ciudades.add(att.value(i));
      for (int j = 0; j < numAtributos; j++) {
        if (i==0) {
          medias.add(instances.get(i).value(j));
        }
        else {
          medias.set(j, medias.get(j) + instances.get(i).value(j));
        }
      }
        division.add(instances.get(i).value(dividendo)/instances.get(i).value(divisor));
    }
    for (int i = 0; i < numAtributos; i++) {
      medias.set(i, medias.get(i)/att.numValues());
    }
    
  }

  protected boolean isDebug() {
    return debug;
  }

  protected void setDebug(boolean debug) {
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

  public List<Double> getMedias() {
    return medias;
  }

  public void setMedias(List<Double> medias) {
    this.medias = medias;
  }

  public List<Double> getDivision() {
    return division;
  }

  public void setDivision(List<Double> division) {
    this.division = division;
  }

  public List<String> getAtributos() {
    return atributos;
  }

  public void setAtributos(List<String> atributos) {
    this.atributos = atributos;
  }

  public List<String> getCiudades() {
    return ciudades;
  }

  public void setCiudades(List<String> ciudades) {
    this.ciudades = ciudades;
  }

}
