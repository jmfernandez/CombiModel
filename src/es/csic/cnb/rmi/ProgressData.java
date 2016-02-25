package es.csic.cnb.rmi;

import java.io.Serializable;

public class ProgressData implements Serializable {
  private static final long serialVersionUID = 1854143435969781731L;

  private int total;
  private int percentaje;
  private String inf;

  public ProgressData() {

  }

  /**
   * @return the inf
   */
  public String getInf() {
    return inf;
  }
  /**
   * @param inf the inf to set
   */
  public void setInf(String inf) {
    this.inf = inf;
  }
  /**
   * @return the total
   */
  public int getTotal() {
    return total;
  }
  /**
   * @param total the total to set
   */
  public void setTotal(int total) {
    this.total = total;
  }

  /**
   * @return the percentaje
   */
  public int getPercentaje() {
    return percentaje;
  }

  /**
   * @param percentaje the percentaje to set
   */
  public void setPercentaje(int percentaje) {
    this.percentaje = percentaje;
  }
}
