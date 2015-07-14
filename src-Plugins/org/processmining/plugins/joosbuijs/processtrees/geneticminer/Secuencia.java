package org.processmining.plugins.joosbuijs.processtrees.geneticminer;

import java.util.Arrays;

public class Secuencia {
  private String[] nodo;
  private int valor;
  public String[] getNodo() {
    return nodo;
  }
  public Secuencia(String linea, String num) {
    this.valor = Integer.parseInt(num);
    String[] subnodos = linea.split("}");
    String[] resultado = new String[subnodos.length];
    for (int i=0;i<subnodos.length;i++){
      resultado[i] = subnodos[i].substring(1, subnodos[i].length());
    }
    this.nodo = resultado;

  }
  public void setNodo(String[] nodo) {
    this.nodo = nodo;
  }
  public int getValor() {
    return valor;
  }
  public void setValor(int valor) {
    this.valor = valor;
  }
  public String toString() {
    return "Secuencia [nodo=" + Arrays.toString(nodo) + ", valor=" + valor
        + "]";
  }
  

}
