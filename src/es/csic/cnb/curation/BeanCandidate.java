package es.csic.cnb.curation;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import es.csic.cnb.data.WSCompound;

public class BeanCandidate implements Serializable {
  private static final long serialVersionUID = 3530065275562944884L;

  private WSCompound wsCandidate;
  private List<WSCompound> dbCandidateList;

  // True para especificar que no necesita curacion (ya esta curado)
  private boolean wsCurated = true;
  private boolean dbCurated = true;

  public BeanCandidate() {
    dbCandidateList = new LinkedList<WSCompound>();
  }

  public void setWsCandidate(WSCompound wsCandidate) {
    this.wsCandidate = wsCandidate;
  }

  public WSCompound getWsCandidate() {
    return wsCandidate;
  }

  public void setCandidates(WSCompound wsCandidate, List<WSCompound> dbCandidateList) {
    this.wsCandidate = wsCandidate;
    this.dbCandidateList = dbCandidateList;
  }

  /**
   * @return the dbCandidateList
   */
  public List<WSCompound> getDbCandidateList() {
    return dbCandidateList;
  }

  /**
   * @param dbCandidateList the candidateList to set
   */
  public void setDbCandidateList(List<WSCompound> dbCandidateList) {
    this.dbCandidateList = dbCandidateList;
  }

  public void addDbCandidate(WSCompound wscomp) {
    dbCandidateList.add(wscomp);
  }

  /**
   * @return true cuando el WS esta curado y no necesita curacion manual.
   */
  public boolean isWsCurated() {
    return wsCurated;
  }

  /**
   * @param curated the curated to set
   */
  public void setWsCurated(boolean curated) {
    this.wsCurated = curated;
  }

  /**
   * @return true cuando la DB esta curada y no necesita curacion manual.
   */
  public boolean isDbCurated() {
    return dbCurated;
  }

  /**
   * @param curated the curated to set
   */
  public void setDbCurated(boolean curated) {
    this.dbCurated = curated;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(wsCurated).append(": ").append(wsCandidate).append("-{");
    sb.append(dbCurated).append(": ").append(dbCandidateList).append("}");

    return sb.toString();
  }
}
