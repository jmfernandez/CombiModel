package es.csic.cnb.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import es.csic.cnb.util.Util;

public class WSCompound implements Comparable<WSCompound>, Serializable {
  private static final long serialVersionUID = 8120482882261042207L;

  public enum WebService { CHEBI, CHEMSPIDER, PUBCHEM, KEGG, DB, UNKNOWN };

  private WebService ws = WebService.UNKNOWN;

  private String id;
  private String name;
  private String smiles;
  private String inchi;
  private String inchiKey;
  private String structure;
  private int charge;
  private double mass;
  private int mapping;

  private Set<String> synList;
  private Map<String,Set<String>> formulaList;
  private Map<String,String> xrefList;

  // Match con el compuesto
  private boolean matchName = false;
  private boolean matchSyn = false;
  private boolean matchFormula = false;
  private boolean matchInchi = false;
  private boolean matchSmiles = false;
  private boolean matchChebiId = false;
  private boolean matchKeggId = false;

  private double numMatches;
  private int levenshtein;
  private int minLevenshtein;
  private double score;
  private boolean uniqueWs = false; // Solo se recupera este comp en el WS
  private boolean repeatedWs = false; // Aparece en varias busquedas diferentes en el WS

  public WSCompound() {}

  public WSCompound(WebService ws, String id) {
    this.ws = ws;
    this.id = id;

    this.synList = new HashSet<String>();
    this.formulaList = new HashMap<String,Set<String>>();
    this.xrefList = new HashMap<String,String>();
  }

  /**
   * @return the ws
   */
  public WebService getWs() {
    return ws;
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = (name == null) ? null : name.trim();
  }

  /**
   * @return the formula hash (source - formula)
   */
  public Map<String, Set<String>> getFormulaList() {
    return formulaList;
  }

  public Set<String> getAllFormulas() {
    Set<String> list = new HashSet<String>();
    for (Set<String> l : formulaList.values()) {
      list.addAll(l);
    }
    return list;
  }

  /**
   * @param formula the formula to add
   */
  public void addFormula(String source, String formula) {
    if (formula != null) {
      Set<String> list = formulaList.get(source);
      if (list == null) {
        list = new HashSet<String>();
      }
      list.add(formula.trim());
      this.formulaList.put(source, list);
    }
  }

  /**
   * @return the smiles
   */
  public String getSmiles() {
    return smiles;
  }

  /**
   * @param smiles the smiles to set
   */
  public void setSmiles(String smiles) {
    this.smiles = (smiles == null) ? null : smiles.trim();
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
    this.inchi = (inchi == null) ? null : inchi.trim();
  }

  /**
   * @return the inchiKey
   */
  public String getInchiKey() {
    return inchiKey;
  }

  /**
   * @param inchiKey the inchiKey to set
   */
  public void setInchiKey(String inchiKey) {
    this.inchiKey = (inchiKey == null) ? null : inchiKey.trim();
  }

  /**
   * @return the structure
   */
  public String getStructure() {
    return structure;
  }

  /**
   * @param structure the structure to set
   */
  public void setStructure(String structure) {
    this.structure = structure;
  }

  /**
   * @return the synList
   */
  public Set<String> getSynList() {
    return synList;
  }

  /**
   * @param syn the synonym to add
   */
  public void addSyn(String syn) {
    if (syn != null)
      this.synList.add(syn.trim());
  }

  /**
   * @return the xrefList (source - xref)
   */
  public Map<String, String> getXrefList() {
    return xrefList;
  }

  /**
   * @return the keggId
   */
  public String getKeggId() {
    return xrefList.get(Util.SOURCE_KEGG);
  }

  /**
   * @return the chebiId
   */
  public String getChebiId() {
    return xrefList.get(Util.SOURCE_CHEBI);
  }

  /**
   * @param source
   * @param xref
   */
  public void addXref(String source, String xref) {
    this.xrefList.put(source, xref);
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
   * @return the matchName
   */
  public boolean isMatchName() {
    return matchName;
  }

  /**
   * @param matchName the matchName to set
   */
  public void setMatchName(boolean matchName) {
    this.matchName = matchName;
  }

  /**
   * @return the matchSyn
   */
  public boolean isMatchSyn() {
    return matchSyn;
  }

  /**
   * @param matchSyn the matchSyn to set
   */
  public void setMatchSyn(boolean matchSyn) {
    this.matchSyn = matchSyn;
  }

  /**
   * @return the matchFormula
   */
  public boolean isMatchFormula() {
    return matchFormula;
  }

  /**
   * @param matchFormula the matchFormula to set
   */
  public void setMatchFormula(boolean matchFormula) {
    this.matchFormula = matchFormula;
  }

  /**
   * @return the matchInchi
   */
  public boolean isMatchInchi() {
    return matchInchi;
  }

  /**
   * @param matchInchi the matchInchi to set
   */
  public void setMatchInchi(boolean matchInchi) {
    this.matchInchi = matchInchi;
  }

  /**
   * @return the matchSmiles
   */
  public boolean isMatchSmiles() {
    return matchSmiles;
  }

  /**
   * @param matchSmiles the matchSmiles to set
   */
  public void setMatchSmiles(boolean matchSmiles) {
    this.matchSmiles = matchSmiles;
  }

  /**
   * @return the matchChebiId
   */
  public boolean isMatchChebiId() {
    return matchChebiId;
  }

  /**
   * @param matchChebiId the matchChebiId to set
   */
  public void setMatchChebiId(boolean matchChebiId) {
    this.matchChebiId = matchChebiId;
  }

  /**
   * @return the matchKeggId
   */
  public boolean isMatchKeggId() {
    return matchKeggId;
  }

  /**
   * @param matchKeggId the matchKeggId to set
   */
  public void setMatchKeggId(boolean matchKeggId) {
    this.matchKeggId = matchKeggId;
  }

  /**
   * @return the numMatches
   */
  public double getNumMatches() {
    return numMatches;
  }

  /**
   * @param num
   */
  public void setNumMatches(double num) {
    this.numMatches = num;
  }

  /**
   * @param numMatches the numMatches to increment
   */
  public void incrementNumMatches(double increment) {
    this.numMatches += increment;
    if (this.numMatches < 0) {
      this.numMatches = 0;
    }
  }

  /**
   * @return the levenshtein
   */
  public int getLevenshtein() {
    return levenshtein;
  }

  /**
   * @param levenshtein the levenshtein to set
   */
  public void setLevenshtein(int levenshtein) {
    this.levenshtein = levenshtein;
  }

  /**
   * @return the min levenshtein
   */
  public int getMinLevenshtein() {
    return minLevenshtein;
  }

  /**
   * @param levenshtein the min levenshtein to set
   */
  public void setMinLevenshtein(int minLevenshtein) {
    this.minLevenshtein = minLevenshtein;
  }

  /**
   * @return the score
   */
  public double getScore() {
    return score;
  }

  /**
   * @param score the score to set
   */
  public void setScore(double score) {
    this.score = score;
  }

  /**
   * @return the mass
   */
  public double getMass() {
    return mass;
  }

  /**
   * @param mass the mass to set
   */
  public void setMass(double mass) {
    this.mass = mass;
  }

  /**
   * @return the mapping
   */
  public int getMapping() {
    return mapping;
  }

  /**
   * @param mapping the mapping to set
   */
  public void setMapping(int mapping) {
    this.mapping = mapping;
  }

  /**
   * @return the uniqueWs
   */
  public boolean isUniqueWs() {
    return uniqueWs;
  }

  /**
   * @param uniqueWs the uniqueWs to set
   */
  public void setUniqueWs(boolean uniqueWs) {
    this.uniqueWs = uniqueWs;
  }

  /**
   * @return the repeatedWs
   */
  public boolean isRepeatedWs() {
    return repeatedWs;
  }

  /**
   * @param repeatedWs the repeatedWs to set
   */
  public void setRepeatedWs(boolean repeatedWs) {
    this.repeatedWs = repeatedWs;
  }

  @Override
  public int compareTo(WSCompound wscomp) {
    double nm = wscomp.getNumMatches();
    if(this.numMatches > nm)
      return 1;
    else if (this.numMatches < nm)
      return -1;
    else if (this.minLevenshtein < wscomp.getMinLevenshtein())
      return 1;
    else if (this.minLevenshtein > wscomp.getMinLevenshtein())
      return -1;
    else
      return 0;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(id).append(": ").append(name);
    sb.append(" (").append(charge).append(")");
    sb.append(" [M:").append(numMatches).append(" - ML:").append(minLevenshtein).append("]");
    return sb.toString();
  }
}
