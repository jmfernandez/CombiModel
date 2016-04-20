package es.csic.cnb;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.csic.cnb.checker.ChemDigitChecker;
import es.csic.cnb.checker.ChemEsteroisomerChecker;
import es.csic.cnb.checker.ChemIsoChecker;
import es.csic.cnb.checker.ChemSugarChecker;
import es.csic.cnb.data.NetCompound;
import es.csic.cnb.db.CompoundDbManager;
import es.csic.cnb.db.DbManager;
import es.csic.cnb.util.DiffMatchPatch;
import es.csic.cnb.util.Util;
import es.csic.cnb.util.Util.Status;

public class DbSearchEngine {
  private final static Logger LOGGER = Logger.getLogger(DbSearchEngine.class.getName());

  private static final int CONT_TOTAL = 0;
  private static final int CONT_CHEBI = 1;
  private static final int CONT_KEGG  = 2;
  private static final int CONT_FM    = 3;
  private static final int CONT_OTHER = 4;

  private Matcher mtAlphaNum = Pattern.compile("[^A-Za-z0-9]").matcher("");

  private NetCompound comp;

  private List<Integer> chemIdList;
  private Map<Integer,int[]> chemIdMap;
  private Map<Integer,Integer> candidatesMinLv;

  private CompoundDbManager db;

  private DiffMatchPatch dmp;

  public DbSearchEngine(NetCompound comp) {
    this.comp = comp;
    chemIdList = new ArrayList<Integer>();
    chemIdMap = new HashMap<Integer,int[]>();
    candidatesMinLv = new HashMap<Integer,Integer>();

    dmp = new DiffMatchPatch();

    db = DbManager.INSTANCE.getCompoundDbMgr();
  }

  public Status findChemIdCandidates() {

    Set<String> seedChebiIdList = new HashSet<String>();
    Set<String> seedKeggIdList = new HashSet<String>();
    if ((comp.validateCharge() && comp.getCharge() != 0) || !comp.validateCharge()) {
      final List<String> keggIdList = DbManager.INSTANCE.getSeedDbMgr().getKeggIdList(comp.getCleanSbmlId());
      for (String keggId : keggIdList) {
        seedKeggIdList.add(keggId);
        for (String seedChebiId : DbManager.INSTANCE.getKeggDbMgr().getChebiIdList(keggId)) {
          seedChebiIdList.add("CHEBI:" + seedChebiId);
        }
      }
    }


    // La siguiente busqueda deberia dar un unico resultado
    if (comp.getSbmlId() != null) {
      List<Integer> chemIdList = db.getChemIdForSbmlId(comp.getCleanSbmlId());

      updateChemIdMap(chemIdList, CONT_OTHER);
    }

    // Las siguientes busquedas pueden dar varios resultados
    // Buscar por formula (NO SE BUSCA POR FM_OTHER)
    if (comp.getSbmlFormula() != null) {
      List<Integer> chemIdList = db.getChemIdListForSbmlFormula(comp.getSbmlFormula());

      updateChemIdMap(chemIdList, CONT_FM);
    }

    // Buscar por xref
    seedChebiIdList.addAll(comp.getChebiIdList());
    for (String chebiId : seedChebiIdList) {
      List<Integer> chemIdList = db.getChemIdListForChebi(chebiId);

      updateChemIdMap(chemIdList, CONT_CHEBI);
    }
    seedKeggIdList.addAll(comp.getKeggIdList());
    for (String keggId : seedKeggIdList) {
      List<Integer> chemIdList = db.getChemIdListForKegg(keggId);

      updateChemIdMap(chemIdList, CONT_KEGG);
    }

    // Buscar por syn
    if (comp.getSbmlName() != null) {
      Set<Integer> chemIdList = new HashSet<Integer>();
      chemIdList.addAll(db.getChemIdListBySyn(comp.getSbmlName()));

      String normalName = Util.getChemicalName(Util.splitChemicalName(comp.getSbmlName())).toLowerCase();
      chemIdList.addAll(db.getChemIdListByNormalSyn(normalName));

      chemIdList.addAll(chemIdMap.keySet());

      for (int chemId : chemIdList) {
        int minLv = matchNameLevenshtein(chemId, comp);

        if (candidatesMinLv.containsKey(chemId)) {
          int candidateML = candidatesMinLv.get(chemId);
          // El Levenshtein recuperado es menor
          if (minLv < candidateML) {
            candidatesMinLv.put(chemId, minLv);
          }
        }
        else {
          candidatesMinLv.put(chemId, minLv);
        }

        // Match
        if (minLv == 0) {
          int[] matches = chemIdMap.get(chemId);
          if (matches == null) {
            matches = new int[5];
          }
          matches[CONT_TOTAL] += 2;
          matches[CONT_OTHER] += 2;

          // Premiar biomasa
          if (comp.getSbmlName().equalsIgnoreCase(Util.BIOMASS)) {
            matches[CONT_TOTAL] += 1;
            matches[CONT_OTHER] += 1;
          }

          if (!comp.validateCharge()) {
            matches[CONT_TOTAL] += 1;
            matches[CONT_OTHER] += 1;
          }

          chemIdMap.put(chemId, matches);
        }
      }
    }

    // Buscar por inchi o smile
    for (String sm : comp.getSmilesList()) {
      List<Integer> chemIdList = db.getChemIdListForStructures(sm);

      updateChemIdMap(chemIdList, CONT_OTHER);
    }
    if (comp.getInchi() != null) {
      List<Integer> chemIdList = db.getChemIdListForStructures(comp.getInchi());

      updateChemIdMap(chemIdList, CONT_OTHER);
    }
    if (comp.getInchikey() != null) {
      List<Integer> chemIdList = db.getChemIdListForStructures(comp.getInchikey());

      updateChemIdMap(chemIdList, CONT_OTHER);
    }

    // Penalizaciones: chequeos para ver si todo es correcto
    checkCharge();
    checkChebi();
    checkKegg();
    checkFm();

    // Eliminar casos erroneos (iso-anteiso, numeros diferentes...)
    checkErrors();

    // Obtener candidatos validos
    return checkCandidates();
  }

  /**
   * @return the chemIdList
   */
  public List<Integer> getChemIdCandidateList() {
    return chemIdList;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int chemId : chemIdMap.keySet()) {
      sb.append("{");
      sb.append(chemId).append(": ");
      sb.append(db.getOfn(chemId)).append(" > ");
      sb.append(chemIdMap.get(chemId)[CONT_TOTAL]);
      sb.append(" ").append(Arrays.toString(chemIdMap.get(chemId)));
      sb.append("}");
      //sb.append(Util.NEWLINE);
    }

    return sb.toString();
  }

  ////////////////////////////////////////////////////////
  //// METODOS CORRESPONDIENTES AL MATCH
  ////////////////////////////////////////////////////////

  private Status checkCandidates() {
    // No se ha encontrado id
    Status st = Status.NOTFOUND;

    // Se ha encontrado id
    if (!chemIdMap.isEmpty()) {
      // numMatches - lista chemids
      Map<Integer,List<Integer>> matchesMap = new HashMap<Integer,List<Integer>>();
      // Verificar datos y quedarnos con el que mas se ajusta
      int bestMatch = 0;
      for (int id : chemIdMap.keySet()) {
        int match = chemIdMap.get(id)[CONT_TOTAL];
        if (match >= bestMatch) {
          List<Integer> tmpList = matchesMap.get(match);
          if (tmpList == null) {
            tmpList = new ArrayList<Integer>();
          }
          tmpList.add(id);
          matchesMap.put(match, tmpList);

          bestMatch = match;
        }
      }

      // Existe match con valores superiores a dos: aceptados
      if (bestMatch > 2) {
        chemIdList = matchesMap.get(bestMatch);
        st = (chemIdList.size() == 1) ? Status.FOUND : Status.DUDE;
      }
      // Si es uno y su Levenshtein es menor que 5 requerira correccion manual
      else if (bestMatch == 2) {
        // Creo lista con varios elementos para permitir correccion manual
        for (int chemId : matchesMap.get(bestMatch)) {
          if (candidatesMinLv.get(chemId) != null && candidatesMinLv.get(chemId) <= Util.LV_MARGIN) {
            chemIdList.add(chemId);
            st = Status.DUDE;
          }
        }
      }
      // El resto de casos se eliminan
      else {
        st = Status.NOTFOUND;
      }
    }

    return st;
  }

  /**
   * Evaluar la carga.
   * Si no coincide se pone el numero de matches a cero.
   * @throws SQLException
   */
  private void checkCharge() {
    if (comp.validateCharge()) {
      int compChg = comp.getCharge();

      for (int chemId : chemIdMap.keySet()) {
        int dbChg = db.getCharge(chemId);

        // Las cargas son iguales
        if (compChg == dbChg) {
          // Si son diferentes de cero se incrementa el match
          if (compChg != 0) {
            updateChemIdMap(chemId, CONT_OTHER);
          }
        }
        // Las cargas son diferentes: penalizar
        else {
          chemIdMap.put(chemId, new int[]{0,0,0,0});

          // No coincide
          if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST,
                    "No coincide comp. carga ({0}): {1} >>> DB[{2}: {3}] con carga ({4})",
                    new Object[]{compChg, comp, String.valueOf(chemId), db.getOfn(chemId), dbChg});
          }
        }
      } // Fin for
    }
  }

  /**
   * Evaluar las formulas.
   * Si alguna formula no coincide se pone el numero de matches a cero.
   * @throws SQLException
   */
  private void checkFm() {
    String sbmlFormula = comp.getSbmlFormula();
    if (sbmlFormula != null) {
      for (int chemId : chemIdMap.keySet()) {
        int total = chemIdMap.get(chemId)[CONT_TOTAL];
        if (total == 0) continue;

        int fm = chemIdMap.get(chemId)[CONT_FM];
        if (fm == 0) {
          Set<String> fmDbList = db.getSbmlFormulaList(chemId);
          if (!fmDbList.isEmpty()) {
            boolean match = false;
            for (String dbSbmlFormula : fmDbList) {
              if (sbmlFormula.equals(dbSbmlFormula)) {
                match = true;
                break;
              }
            }

            if (!match) {
              // Penalizar si la formula no coincide
              chemIdMap.put(chemId, new int[]{0,0,0,0});

              // No coincide
              if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST,
                        "No coincide comp. formula (sbml: {0}): {1} >>> DB[{2}: {3}] " +
                                "con formula (sbml: {4} - neutra: {5})",
                                new Object[]{sbmlFormula, comp, String.valueOf(chemId), db.getOfn(chemId),
                        db.getSbmlFormulaList(chemId), db.getNeutralFormulaList(chemId)});
              }
            }
          }
        }
      } // Fin for
    }
  }

  /**
   * Evaluar enlaces de Chebi.
   * Si algun enlace no coincide se pone el numero de matches a cero.
   * @throws SQLException
   */
  private void checkChebi() {
    Set<String> compChebiIdList = comp.getChebiIdList();
    if (!compChebiIdList.isEmpty()) {
      for (int chemId : chemIdMap.keySet()) {
        int total = chemIdMap.get(chemId)[CONT_TOTAL];
        if (total == 0) continue;

        int chebi = chemIdMap.get(chemId)[CONT_CHEBI];
        if (chebi == 0) {
          Set<String> dbChebiIdList = db.getChebiIdList(chemId);
          if (!dbChebiIdList.isEmpty()) {
            boolean match = false;
            for (String chebiId : compChebiIdList) {
              for (String dbChebiId : dbChebiIdList) {
                if (chebiId.equals(dbChebiId)) {
                  match = true;
                  break;
                }
              }
            }
            if (!match) {
              // Penalizar si chebi no coincide
              chemIdMap.put(chemId, new int[]{0,0,0,0});

              if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST,
                        "No coincide comp. {0}: {1} >>> DB[{2}: {3}] con {4}",
                        new Object[]{compChebiIdList, comp, String.valueOf(chemId), db.getOfn(chemId), dbChebiIdList});
              }
            }
          }
        }
      } // Fin for
    }
  }

  /**
   * Evaluar enlaces de Kegg.
   * Si algun enlace no coincide se pone el numero de matches a cero.
   * @throws SQLException
   */
  private void checkKegg() {
    Set<String> compKeggIdList = comp.getKeggIdList();
    if (!compKeggIdList.isEmpty()) {
      for (int chemId : chemIdMap.keySet()) {
        int total = chemIdMap.get(chemId)[CONT_TOTAL];
        if (total == 0) continue;

        int kegg = chemIdMap.get(chemId)[CONT_KEGG];
        if (kegg == 0) {
          Set<String> dbKeggIdList = db.getKeggIdList(chemId);
          if (!dbKeggIdList.isEmpty()) {
            boolean match = false;
            for (String keggId : compKeggIdList) {
              for (String dbKeggId : dbKeggIdList) {
                if (keggId.equals(dbKeggId)) {
                  match = true;
                  break;
                }
              }
            }

            if (!match) {
              // Penalizar si kegg no coincide
              chemIdMap.put(chemId, new int[]{0,0,0,0});

              if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST,
                        "No coincide comp. KEGG {0}: {1} >>> DB[{2}: {3}] con KEGG {4}",
                        new Object[]{compKeggIdList, comp, String.valueOf(chemId), db.getOfn(chemId), dbKeggIdList});
              }
            }
          }
        }
      } // Fin for
    }
  }

  private void checkErrors() {
    final ChemDigitChecker digitChecker = new ChemDigitChecker(comp);
    final ChemEsteroisomerChecker estChecker = new ChemEsteroisomerChecker(comp);
    final ChemIsoChecker isoChecker = new ChemIsoChecker(comp);
    final ChemSugarChecker sugarChecker = new ChemSugarChecker(comp);

    // Eliminar casos erroneos
    for (int chemId : chemIdMap.keySet()) {
      int total = chemIdMap.get(chemId)[CONT_TOTAL];
      if (total == 0) continue;
      if (candidatesMinLv.isEmpty()) continue; // Cambio 2016 para evitar nullPointer por candidatesMinLv: {}
      
      // Casos erroneos propensos a validacion manual
      final int mlv = candidatesMinLv.get(chemId);
      if (mlv > 0 && mlv <= Util.LV_MARGIN) {
        final Set<String> synDbList = db.getSynList(chemId);

        boolean penalty = true;
        // Checks
        if (digitChecker.check(synDbList)) {
          if (estChecker.check(synDbList)) {
            if (isoChecker.check(synDbList)) {
              if (sugarChecker.check(synDbList)) {
                penalty = false;
              }
            }
          }
        }
        if (penalty) {
          chemIdMap.put(chemId, new int[]{0,0,0,0});
        }
      }
    }
  }

  private void updateChemIdMap(List<Integer> chemIdList, int source) {
    for (int chemId : chemIdList) {
      this.updateChemIdMap(chemId, source);
    }
  }

  private void updateChemIdMap(int chemId, int source) {
    int[] matches = chemIdMap.get(chemId);
    if (matches == null) {
      matches = new int[5];
    }
    matches[CONT_TOTAL] += 1;
    matches[source] += 1;
    chemIdMap.put(chemId, matches);
  }

  private int matchNameLevenshtein(int chemId, NetCompound comp) {
    int minLevenshtein = 100;

    // Calcular Levenshtein
    Set<String> compSynList = comp.getSynList(); // incluye nombre

    if (!compSynList.isEmpty()) {
      Set<String> synDbList = db.getSynList(chemId);

      for (String compSyn : compSynList) {
        compSyn = compSyn.toLowerCase();
        String cleanSyn = mtAlphaNum.reset(compSyn).replaceAll("");
        String normalSyn = Util.getChemicalName(Util.splitChemicalName(compSyn));

        for (String dbSyn : synDbList) {
          dbSyn = dbSyn.toLowerCase();
          int lv = dmp.diff_levenshtein(dmp.diff_main(cleanSyn,
                  mtAlphaNum.reset(dbSyn).replaceAll("")));
          minLevenshtein = (lv < minLevenshtein) ? lv : minLevenshtein;

          if (minLevenshtein == 0)
            return 0;

          lv = dmp.diff_levenshtein(dmp.diff_main(normalSyn,
                  Util.getChemicalName(Util.splitChemicalName(dbSyn)).toLowerCase()));
          minLevenshtein = (lv < minLevenshtein) ? lv : minLevenshtein;

          if (minLevenshtein == 0)
            return 0;
        }
      }
    }

    return minLevenshtein;
  }
}
