package org.processmining.plugins.wpp.objects;

import org.processmining.framework.annotations.AuthoredType;

@AuthoredType(typeName = "Medias y relaci�n de valores", affiliation = "Universidad de la Laguna", author = "Daniel Nicol�s Fern�ndez del Castilllo Salazar", email = "danibone@icloud.com")
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
