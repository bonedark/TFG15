package org.processmining.plugins.wpp.objects;

import org.processmining.framework.annotations.AuthoredType;

@AuthoredType(typeName = "Medias y relación de valores", affiliation = "Universidad de la Laguna", author = "Daniel Nicolás Fernández del Castilllo Salazar", email = "danibone@icloud.com")
public class Output {

  private Genetic genetic;


  public Output(Genetic genetic) {
    this.genetic = genetic;
  }

  public Genetic getGenetic() {
    return genetic;
  }

  public void setGenetic(Genetic genetic) {
    this.genetic = genetic;
  }

}
