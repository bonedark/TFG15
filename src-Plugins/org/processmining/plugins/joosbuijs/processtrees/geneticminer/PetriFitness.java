package org.processmining.plugins.joosbuijs.processtrees.geneticminer;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;

public class PetriFitness {
  private Petrinet petri;
  private double fitness;
  private double coincidencias;
  public Petrinet getPetri() {
    return petri;
  }
  public void setPetri(Petrinet petri) {
    this.petri = petri;
  }
  public PetriFitness(Petrinet petri, double fitness, double coincidencias) {
    super();
    this.petri = petri;
    this.fitness = fitness;
    this.coincidencias = coincidencias;
  }
  public double getFitness() {
    return fitness;
  }
  public void setFitness(double fitness) {
    this.fitness = fitness;
  }
  public double getCoincidencias() {
    return coincidencias;
  }
  public void setCoincidencias(double coincidencias) {
    this.coincidencias = coincidencias;
  }

}
