package es.csic.cnb.rmi;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.csic.cnb.data.Fm;
import es.csic.cnb.data.WSCompound;
import es.csic.cnb.util.Util;

public class RMIData implements Serializable {
  private static final long serialVersionUID = 6708203059823159485L;

  public static final int IDX_NONE = -1;
  public static final int IDX_NULL = -2;

  private String sbmlId;
  private String sbmlCleanId;
  private String sbmlName;

  private String sbmlFormula;
  private String neutralFormula;
  private int charge = 0;

  private String ofn;     // Nombre oficial
  private String inchi;
  private String inchikey;
  private Set<String> smilesList;
  private Set<String> synList;
  private Map<String,Set<Fm>> fmList; // Formulas
  private Map<String,Set<String>> xrefList;

  private boolean exchangeCompound = false;

  private boolean pendingCurationXrefMap = false;
  private boolean pendingCurationDbMap = false;

  private List<WSCompound> wscompList;

  private int dorsal;

  public RMIData() {
    wscompList = new LinkedList<WSCompound>();
  }

  /**
   * @return the sbmlId
   */
  public String getSbmlId() {
    return sbmlId;
  }

  /**
   * @param sbmlId the sbmlId to set
   */
  public void setSbmlId(String sbmlId) {
    this.sbmlId = sbmlId;
  }

  /**
   * @return the sbmlCleanId
   */
  public String getSbmlCleanId() {
    return sbmlCleanId;
  }

  /**
   * @param sbmlCleanId the sbmlCleanId to set
   */
  public void setSbmlCleanId(String sbmlCleanId) {
    this.sbmlCleanId = sbmlCleanId;
  }

  /**
   * @return the sbmlName
   */
  public String getSbmlName() {
    return sbmlName;
  }

  /**
   * @param sbmlName the sbmlName to set
   */
  public void setSbmlName(String sbmlName) {
    this.sbmlName = sbmlName;
  }

  /**
   * @return the sbmlFormula
   */
  public String getSbmlFormula() {
    return sbmlFormula;
  }

  /**
   * @param sbmlFormula the sbmlFormula to set
   */
  public void setSbmlFormula(String sbmlFormula) {
    this.sbmlFormula = sbmlFormula;
  }

  /**
   * @return the neutralFormula
   */
  public String getNeutralFormula() {
    return neutralFormula;
  }

  /**
   * @param neutralFormula the neutralFormula to set
   */
  public void setNeutralFormula(String neutralFormula) {
    this.neutralFormula = neutralFormula;
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

  /**
   * @return the ofn
   */
  public String getOfn() {
    return ofn;
  }

  /**
   * @param ofn the ofn to set
   */
  public void setOfn(String ofn) {
    this.ofn = ofn;
  }

  /**
   * @return the inchi
   */
  public String getInchi() {
    return inchi;
  }

  /**
   * @param inchi the inchi to set
   */
  public void setInchi(String inchi) {
    this.inchi = inchi;
  }

  /**
   * @return the inchikey
   */
  public String getInchikey() {
    return inchikey;
  }

  /**
   * @param inchikey the inchikey to set
   */
  public void setInchikey(String inchikey) {
    this.inchikey = inchikey;
  }

  /**
   * @return the smilesList
   */
  public Set<String> getSmilesList() {
    return smilesList;
  }

  /**
   * @param smilesList the smilesList to set
   */
  public void setSmiles(Set<String> smilesList) {
    this.smilesList = smilesList;
  }

  /**
   * @return the synList
   */
  public Set<String> getSynList() {
    return synList;
  }

  /**
   * @param synList the synList to set
   */
  public void setSynList(Set<String> synList) {
    this.synList = synList;
  }

  /**
   * @return the fmList
   */
  public Map<String, Set<Fm>> getFmList() {
    return fmList;
  }

  /**
   * @param fmList the fmList to set
   */
  public void setFmList(Map<String, Set<Fm>> fmList) {
    this.fmList = fmList;
  }

  /**
   * @return the xrefList
   */
  public Map<String, Set<String>> getXrefList() {
    return xrefList;
  }

  /**
   * @param xrefList the xrefList to set
   */
  public void setXrefList(Map<String, Set<String>> xrefList) {
    this.xrefList = xrefList;
  }

  /**
   * @return the chebiId list
   */
  public Set<String> getChebiIdList() {
    return xrefList.get(Util.SOURCE_CHEBI);
  }

  /**
   * @return the keggId list
   */
  public Set<String> getKeggIdList() {
    return xrefList.get(Util.SOURCE_KEGG);
  }

  /**
   * @return the exchangeCompound
   */
  public boolean isExchangeCompound() {
    return exchangeCompound;
  }

  /**
   * @param exchangeCompound the exchangeCompound to set
   */
  public void setExchangeCompound(boolean exchangeCompound) {
    this.exchangeCompound = exchangeCompound;
  }

  /**
   * @return the isPendingCurationXrefMap
   */
  public boolean isPendingCurationXrefMap() {
    return pendingCurationXrefMap;
  }

  /**
   * @param pending
   */
  public void setPendingCurationXrefMap(boolean pending) {
    this.pendingCurationXrefMap = pending;
  }

  /**
   * @return the isPendingCurationDbMap
   */
  public boolean isPendingCurationDbMap() {
    return pendingCurationDbMap;
  }

  /**
   * @param pending
   */
  public void setPendingCurationDbMap(boolean pending) {
    this.pendingCurationDbMap = pending;
  }

  /**
   * @return the wscompList
   */
  public List<WSCompound> getWsCompList() {
    return wscompList;
  }

  /**
   * @param wscompList the wscompList to set
   */
  public void setWsCompList(List<WSCompound> wscompList) {
    this.wscompList = wscompList;
  }

  /**
   * @param wscomp
   */
  public void addWsComp(WSCompound wscomp) {
    wscompList.add(wscomp);
  }

  /**
   * @return the dorsal
   */
  public int getDorsal() {
    return dorsal;
  }

  /**
   * @param dorsal the dorsal to set
   */
  public void setDorsal(int dorsal) {
    this.dorsal = dorsal;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (exchangeCompound) {
      sb.append("*");
    }
    sb.append(sbmlId);
//    sb.append(sbmlId).append(":").append(sbmlName);
//    sb.append(" [").append(charge).append("]");
//    sb.append(" --> ").append(wscompList);
    return sb.toString();
  }
}
