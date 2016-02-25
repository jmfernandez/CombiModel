package es.csic.cnb.data;

import java.io.Serializable;

public class Fm implements Serializable {
  private static final long serialVersionUID = 1222533228899960016L;

  private String fm;
  private int charge;

  public Fm() {}

  public Fm(String fm, int charge) {
    this.fm = fm;
    this.charge = charge;
  }

  /**
   * @return the fm
   */
  public String getFm() {
    return fm;
  }

  /**
   * @param fm the fm to set
   */
  public void setFm(String fm) {
    this.fm = fm;
  }

  /**
   * @return the charge
   */
  public int getCharge() {
    return charge;
  }

  /**
   * @param charge the charge to set
   */
  public void setCharge(int charge) {
    this.charge = charge;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((fm == null) ? 0 : fm.hashCode());
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Fm other = (Fm) obj;
    if (fm == null) {
      if (other.fm != null)
        return false;
    } else if (!fm.equals(other.fm))
      return false;
    return true;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(fm);
    sb.append(" [").append(charge).append("]");

    return sb.toString();
  }
}
