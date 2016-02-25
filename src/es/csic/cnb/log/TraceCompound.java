package es.csic.cnb.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.csic.cnb.data.NetCompound;
import es.csic.cnb.data.WSCompound;
import es.csic.cnb.db.CompoundDbManager;
import es.csic.cnb.db.DbManager;
import es.csic.cnb.util.Util;

public enum TraceCompound {
  INSTANCE;

  private static final String NEWLINE = System.getProperty("line.separator");

  private static final String OUTPUT = Util.USERDIR + "/log/";
  private static final String OUTPUT_TOTAL = Util.USERDIR + "/log/total.txt";

  private Matcher mtAlphaNum = Pattern.compile("[^A-Za-z0-9]").matcher("");

  private static int totalFiles = 0;
  private static int totalComp = 0;
  private static int totalComp10 = 0;
  private static int totalComp11 = 0;
  private static int totalComp11man = 0;
  private static int totalComp1n = 0;

  private static int totalMapMan = 0;
  private static int totalMapOk = 0;


  private static Set<String> idSet = new HashSet<String>();
  private static Set<String> idMapSet = new HashSet<String>();

  private List<PairComp> pairList10 = new LinkedList<PairComp>();
  private List<PairComp> pairList11 = new LinkedList<PairComp>();
  private List<PairComp> pairList11m = new LinkedList<PairComp>();
  private List<PairComp> pairList1n = new LinkedList<PairComp>();

  private List<String> dbList = new LinkedList<String>();
  private List<String> dbManList = new LinkedList<String>();

  private String sbmlfile;

  private CompoundDbManager db;

  public void setSbmlFile(File file, int idx) {
    this.sbmlfile = file.getName();
    totalFiles++;

    pairList10.clear();
    pairList11.clear();
    pairList11m.clear();
    pairList1n.clear();

    dbList.clear();
    dbManList.clear();

    db = DbManager.INSTANCE.getCompoundDbMgr();
  }

  public void add(NetCompound comp) {
    if (updateParametersAndContinue(comp)) {
      PairComp pair = new PairComp(comp);
      pairList10.add(pair);
    }
  }

  public void add(NetCompound comp, WSCompound wscomp, boolean manual) {
    if (updateParametersAndContinue(comp)) {
      PairComp pair = new PairComp(comp, wscomp);
      if (manual) {
        pairList11m.add(pair);
      }
      else {
        pairList11.add(pair);
      }
    }
  }

  public void add(NetCompound comp, List<WSCompound> list) {
    if (updateParametersAndContinue(comp)) {
      PairComp pair = new PairComp(comp, list);
      pairList1n.add(pair);
    }
  }

  /**
   * Almacena mapeos a la base de datos
   *
   * @param comp
   * @param chemId
   * @throws SQLException
   */
  public void add(NetCompound comp, int chemId) {
    if (!idMapSet.contains(comp.getCleanSbmlId())) {
      idMapSet.add(comp.getCleanSbmlId());

      boolean foundId = false;
      for (String s : DbManager.INSTANCE.getCompoundDbMgr().getXrefList(chemId, Util.SOURCE_SBML)) {
        if (s.equals(comp.getCleanSbmlId())) {
          foundId = true;
          break;
        }
      }

      if (!foundId) {
        StringBuilder sb = new StringBuilder();
        sb.append(":::::::::::::::::::: ddbb Mapping ::::::::::::::::::::").append(NEWLINE);
        sb.append(dbCompToString(chemId, comp)).append(NEWLINE); // dbComp
        sb.append(compToString(comp)).append(NEWLINE); // sbmlComp
        sb.append(NEWLINE);

        dbList.add(sb.toString());
        totalMapOk++;
      }
    }
  }

  /**
   * Almacena mapeos pendientes de validacion manual a la base de datos
   *
   * @param comp
   * @param chemId
   * @throws SQLException
   */
  public void addManCuration(NetCompound comp, List<Integer> chemIdList) {
    if (!idMapSet.contains(comp.getCleanSbmlId())) {
      idMapSet.add(comp.getCleanSbmlId());
      boolean foundId = false;
      for (Integer chemId : chemIdList) {
        if (chemId == null) continue;
        for (String s : DbManager.INSTANCE.getCompoundDbMgr().getXrefList(chemId, Util.SOURCE_SBML)) {
          if (s.equals(comp.getCleanSbmlId())) {
            foundId = true;
            break;
          }
        }
      }

      if (!foundId) {
        StringBuilder sb = new StringBuilder();
        sb.append(":::::::::::::::::::: ddbb Mapping ::::::::::::::::::::").append(NEWLINE);
        sb.append(compToString(comp)).append(NEWLINE); // sbmlComp
        for (Integer chemId : chemIdList) {
          if (chemId == null) continue;
          sb.append(dbCompToString(chemId, comp)).append(NEWLINE); // dbComp
        }
        sb.append(NEWLINE);

        dbManList.add(sb.toString());
        totalMapMan++;
      }
    }
  }


  public void write() {

    // Mapeos a la base de datos
    BufferedWriter out = null;
    try {
      out = new BufferedWriter(new FileWriter(OUTPUT + "dbmapping.txt", true));

      for (String s : dbList) {
        out.append(s);
      }
      out.flush();

    } catch (IOException e) {
      e.printStackTrace();
    }
    finally {
      try {
        if (out != null)
          out.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    try {
      out = new BufferedWriter(new FileWriter(OUTPUT + "dbmapping.manual.txt", true));

      for (String s : dbManList) {
        out.append(s);
      }
      out.flush();

    } catch (IOException e) {
      e.printStackTrace();
    }
    finally {
      try {
        if (out != null)
          out.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }


    // Mapeos
    int t10 = write(pairList10, "1.0.compounds.txt");
    totalComp10 += t10;

    int t11 = write(pairList11, "1.1.compounds.txt");
    totalComp11 += t11;

    int t11m = write(pairList11m, "1.1.manual.compounds.txt");
    totalComp11man += t11m;

    int t1n = write(pairList1n, "1.n.compounds.txt");
    totalComp1n += t1n;

    // Total
    writeTotal();
  }

  ////////////////////////////////////////////////////////
  // Metodos privados
  ////////////////////////////////////////////////////////

  private int write(List<PairComp> pairList, String filename) {
    int cont = 0;

    BufferedWriter out = null;
    try {
      out = new BufferedWriter(new FileWriter(OUTPUT + filename, true));

      for (PairComp pair : pairList) {
        NetCompound comp = pair.getCompound();
        List<WSCompound> list = pair.getWSCompoundList();

        out.append(this.compToString(comp));
        out.append(this.wscompToString(comp, list));

        cont++;
      }
      out.flush();

    } catch (IOException e) {
      e.printStackTrace();
    }
    finally {
      try {
        if (out != null)
          out.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return cont;
  }

  private void writeTotal() {
    BufferedWriter out = null;
    try {
      int totdb = db.getTotalCompounds();
      int totdbnomap = db.getTotalCompoundsNoMapping();
      int totexdb = db.getTotalExchangeCompounds();
      int totexdbnomap = db.getTotalExchangeCompoundsNoMapping();

      out = new BufferedWriter(new FileWriter(OUTPUT_TOTAL));
      out.append("TOTAL FILES: ").append(String.valueOf(totalFiles)).append(NEWLINE);
      out.append("TOTAL COMPOUNDS: ").append(String.valueOf(totalComp)).append(NEWLINE);
      out.append("TOTAL COMPOUNDS IN DB: ").append(String.valueOf(totdb));
      out.append(" [NO MAPPING: ").append(String.valueOf(totdbnomap)).append("]").append(NEWLINE);
      out.append("TOTAL COMPOUNDS EXCHANGE IN DB: ").append(String.valueOf(totexdb));
      out.append(" [NO MAPPING: ").append(String.valueOf(totexdbnomap)).append("]").append(NEWLINE);
      out.append(NEWLINE);


      String p10 = "0%";
      String p11 = "0%";
      String p11man = "0%";
      String p1n = "0%";
      if (totalComp != 0) {
        p10 = String.valueOf(totalComp10 * 100 / totalComp);
        p11 = String.valueOf(totalComp11 * 100 / totalComp);
        p11man = String.valueOf(totalComp11man * 100 / totalComp);
        p1n = String.valueOf(totalComp1n * 100 / totalComp);
      }
      out.append("TOTAL COMPOUNDS 10: ").append(String.valueOf(totalComp10));
      out.append(" [").append(p10).append("%]").append(NEWLINE);
      out.append("TOTAL COMPOUNDS 11: ").append(String.valueOf(totalComp11));
      out.append(" [").append(p11).append("%]").append(NEWLINE);
      out.append("TOTAL COMPOUNDS 11 manual: ").append(String.valueOf(totalComp11man));
      out.append(" [").append(p11man).append("%]").append(NEWLINE);
      out.append("TOTAL COMPOUNDS 1n: ").append(String.valueOf(totalComp1n));
      out.append(" [").append(p1n).append("%]").append(NEWLINE);
      out.append(NEWLINE);
      out.append("TOTAL MAP OK: ").append(String.valueOf(totalMapOk)).append(NEWLINE);
      out.append("TOTAL MAPMAN: ").append(String.valueOf(totalMapMan)).append(NEWLINE);
      out.flush();

    } catch (IOException e) {
      e.printStackTrace();
    }
    finally {
      try {
        if (out != null)
          out.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  protected String dbCompToString(int chemId, NetCompound comp) {
    CompoundDbManager db = DbManager.INSTANCE.getCompoundDbMgr();

    StringBuilder sb = new StringBuilder();
    sb.append(":::::::::: Db Compound: " + chemId).append(NEWLINE);

    String ofn = db.getOfn(chemId);
    if (ofn.equalsIgnoreCase(comp.getSbmlName())) {
      sb.append("*");
    }
    sb.append("OFN: ").append(ofn).append(NEWLINE);

    for (String s : db.getSynList(chemId)) {
      if (s.equalsIgnoreCase(comp.getSbmlName())) {
        sb.append("*");
      }
      sb.append("SYN: ").append(s).append(NEWLINE);
    }
    for (String s : db.getChebiIdList(chemId)) {
      for (String id : comp.getChebiIdList()) {
        if (id.equals(s)) {
          sb.append("*");
          break;
        }
      }
      sb.append("CHEBI ID: ").append(s).append(NEWLINE);
    }
    for (String s : db.getKeggIdList(chemId)) {
      for (String id : comp.getKeggIdList()) {
        if (id.equals(s)) {
          sb.append("*");
          break;
        }
      }
      sb.append("KEGG ID: ").append(s).append(NEWLINE);
    }
    for (String s : db.getSbmlFormulaList(chemId)) {
      if (s.equals(comp.getSbmlFormula())) {
        sb.append("*");
      }
      sb.append("SBML FORMULA: ").append(s).append(NEWLINE);
    }
    for (String s : db.getNeutralFormulaList(chemId)) {
      if (s.equals(comp.getNeutralFormula())) {
        sb.append("*");
      }
      sb.append("NEUTRAL FORMULA: ").append(s).append(NEWLINE);
    }
    for (String s : db.getStructureList(chemId)) {
      sb.append("ST").append(s).append(NEWLINE);
    }
    for (String s : db.getXrefList(chemId, Util.SOURCE_SBML)) {
      sb.append("SBML ID: ").append(s).append(NEWLINE);
    }

    return sb.toString();
  }

  protected String compToString(NetCompound comp) {
    StringBuilder sb = new StringBuilder();
    sb.append(":::::::::: SBML Compound: ").append(sbmlfile).append(NEWLINE);

    if (comp.isExchangeCompound()) {
      sb.append("+++ IMP +++").append(NEWLINE);
    }

    sb.append("SBML ID: ").append(comp.getSbmlId()).append(NEWLINE);
    //sb.append("ML: ").append(comp.getMinLevenshtein()).append(NEWLINE);
    sb.append("SBML NAME: ").append(comp.getSbmlName()).append(NEWLINE);
    for (String s : comp.getSynList()) {
      if (s.equals(comp.getSbmlName())) continue;
      sb.append("SBML SYN: ").append(s).append(NEWLINE);
    }

    for (String s : comp.getChebiIdList()) {
      sb.append("CHEBI ID: ").append(s).append(NEWLINE);
    }
    for (String s : comp.getKeggIdList()) {
      sb.append("KEGG ID: ").append(s).append(NEWLINE);
    }
    if (comp.getSbmlFormula() != null) {
      sb.append("SBML FORMULA: ").append(comp.getSbmlFormula()).append(NEWLINE);
    }
    if (comp.getNeutralFormula() != null) {
      sb.append("NEUTRAL FORMULA: ").append(comp.getNeutralFormula()).append(NEWLINE);
    }
    if (comp.getInchi() != null) {
      sb.append("INCHI: ").append(comp.getInchi()).append(NEWLINE);
    }
    for (String sm : comp.getSmilesList()) {
      sb.append("SMILES: ").append(sm).append(NEWLINE);
    }
    if (comp.validateCharge()) {
      sb.append("CARGA: ").append(comp.getCharge()).append(NEWLINE);
    }
    else {
      sb.append("CARGA: null").append(NEWLINE);
    }

    return sb.toString();
  }

  protected String wscompToString(NetCompound comp, List<WSCompound> list) {
    StringBuilder sb = new StringBuilder();
    sb.append(NEWLINE);

    for (WSCompound wscomp : list) {
      //String ws = (wscomp.getWs() == WebService.CHEBI) ? "CHEBI" : "CHEMSPIDER";
      String ws = wscomp.getWs().toString();
      sb.append(":: ").append(ws).append(" Web Service Compound [M:").append(wscomp.getNumMatches());
      sb.append(" - ML:").append(wscomp.getMinLevenshtein()).append("]: ").append(NEWLINE);
      if (wscomp.isMatchName()) sb.append("*");
      sb.append("NAME: ").append(wscomp.getName()).append(NEWLINE);

      if (wscomp.isMatchName()) {
        String cleanName = (comp.getSbmlName() == null) ? null : mtAlphaNum.reset(comp.getSbmlName()).replaceAll("");
        for (String syn : wscomp.getSynList()) {
          if (cleanName != null) {
            if (cleanName.equalsIgnoreCase(mtAlphaNum.reset(syn).replaceAll(""))) {
              sb.append("*");
            }
          }
          sb.append("SYN: ").append(syn).append(NEWLINE);
        }
      }
      else {
        for (String syn : wscomp.getSynList()) {
          sb.append("SYN: ").append(syn).append(NEWLINE);
        }
      }

      for (String source : wscomp.getXrefList().keySet()) {
        if (source.equalsIgnoreCase(Util.SOURCE_CHEBI) && wscomp.isMatchChebiId()) {
          sb.append("*");
        }
        else if (source.equalsIgnoreCase(Util.SOURCE_KEGG) && wscomp.isMatchKeggId()) {
          sb.append("*");
        }
        sb.append("XREF: ").append(source).append(": ").append(wscomp.getXrefList().get(source)).append(NEWLINE);
      }

      if (wscomp.isMatchInchi()) sb.append("*");
      sb.append("INCHI: ").append(wscomp.getInchi()).append(NEWLINE);
      sb.append("INCHIKEY: ").append(wscomp.getInchiKey()).append(NEWLINE);
      if (wscomp.isMatchSmiles()) sb.append("*");
      sb.append("SMILES: ").append(wscomp.getSmiles()).append(NEWLINE);
      if (wscomp.isMatchFormula()) sb.append("*");
      sb.append("FORMULA: ").append(wscomp.getFormulaList()).append(NEWLINE);
      sb.append("CARGA: ").append(wscomp.getCharge()).append(NEWLINE);
      sb.append(NEWLINE);
    }
    sb.append(NEWLINE);
    sb.append(NEWLINE);

    return sb.toString();
  }

  private boolean updateParametersAndContinue(NetCompound comp) {
    if (!idSet.contains(comp.getCleanSbmlId())) {
      idSet.add(comp.getCleanSbmlId());
      totalComp++;
      return true;
    }
    return false;
  }

  /**
   *
   *
   * @author Pablo D. Sánchez (pd.sanchez@cnb.csic.es)
   */
  private class PairComp {
    private NetCompound comp;
    private List<WSCompound> wscompList;

    public PairComp(NetCompound comp) {
      this.comp = comp;
      this.wscompList = new LinkedList<WSCompound>();
    }

    public PairComp(NetCompound comp, WSCompound wscomp) {
      this.comp = comp;
      this.wscompList = new LinkedList<WSCompound>();
      if (wscomp != null)
        wscompList.add(wscomp);
    }

    public PairComp(NetCompound comp, List<WSCompound> list) {
      this.comp = comp;
      this.wscompList = list;
    }

    public NetCompound getCompound() {
      return comp;
    }

    public List<WSCompound> getWSCompoundList() {
      return wscompList;
    }
  }

}
