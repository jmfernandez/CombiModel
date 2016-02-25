package es.csic.cnb;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.csic.cnb.checker.ChemDigitChecker;
import es.csic.cnb.checker.ChemEsteroisomerChecker;
import es.csic.cnb.checker.ChemIsoChecker;
import es.csic.cnb.checker.ChemSugarChecker;
import es.csic.cnb.data.NetCompound;
import es.csic.cnb.data.WSCompound;
import es.csic.cnb.db.DbManager;
import es.csic.cnb.db.KeggDbManager;
import es.csic.cnb.db.SeedDbManager;
import es.csic.cnb.util.DiffMatchPatch;
import es.csic.cnb.util.Util;
import es.csic.cnb.util.Util.Status;
import es.csic.cnb.ws.ChebiException;
import es.csic.cnb.ws.ChebiWebService;

public class WsMapper {
  //private final static Logger LOGGER = Logger.getLogger(WSMapper.class.getName());
  private final static DecimalFormat df = new DecimalFormat("#.##");

  private Matcher mtAlphaNum = Pattern.compile("[^A-Za-z0-9]").matcher("");

  private NetCompound comp;

  private List<WSCompound> wsChebi;
  private List<WSCompound> wsChebiGral;
  private List<WSCompound> wsChebiFm;
  private List<WSCompound> wsChebiId;

  private SeedDbManager seedDb = DbManager.INSTANCE.getSeedDbMgr();
  private KeggDbManager keggDb = DbManager.INSTANCE.getKeggDbMgr();

  private DiffMatchPatch dmp;

  private ChebiWebService webserviceChebi;

  public WsMapper(ChebiWebService webserviceChebi, NetCompound comp) {
    this.webserviceChebi = webserviceChebi;
    this.comp = comp;

    wsChebi = new LinkedList<WSCompound>();
    wsChebiGral = new LinkedList<WSCompound>();
    wsChebiFm = new LinkedList<WSCompound>();
    wsChebiId = new LinkedList<WSCompound>();

    dmp = new DiffMatchPatch();

    DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
    dfs.setDecimalSeparator('.');
    df.setDecimalFormatSymbols(dfs);
  }

  public void addAllChebiFmComp(List<WSCompound> wscompList) throws ChebiException {
    List<WSCompound> newList = discardCandidates(wscompList);

    wsChebiFm.addAll(newList);
  }

  public void addAllChebiIdComp(List<WSCompound> wscompList) throws ChebiException {
    List<WSCompound> newList = discardCandidates(wscompList);

    wsChebiId.addAll(newList);
  }

  public void addAllChebiGralComp(List<WSCompound> wscompList) throws ChebiException {
    List<WSCompound> newList = discardCandidates(wscompList);

    wsChebiGral.addAll(newList);
  }

  /**
   * Mapear el compuesto con los resultados de los web services.
   *
   * @return el WSCompound que ancaja con el compuesto o null si no hay resultados
   */
  public List<WSCompound> mapCompounds() {
    List<WSCompound> wscompList = new LinkedList<WSCompound>();

    // Guardar Gral, Id y Fm eliminando los iguales
    Set<String> idList = new HashSet<String>();
    for (WSCompound wscomp : wsChebiFm) {
      wsChebi.add(wscomp);
      idList.add(wscomp.getChebiId());
    }
    for (WSCompound wscomp : wsChebiId) {
      if (idList.contains(wscomp.getChebiId())) {
        wscomp.setRepeatedWs(true);
      }
      else {
        wsChebi.add(wscomp);
        idList.add(wscomp.getChebiId());
      }
    }
    for (WSCompound wscomp : wsChebiGral) {
      if (idList.contains(wscomp.getChebiId())) {
        wscomp.setRepeatedWs(true);
      }
      else {
        wsChebi.add(wscomp);
      }
    }

    // Mapear Chebi y el compuesto
    if (!wsChebi.isEmpty()) {
      final ChemDigitChecker digitChecker = new ChemDigitChecker(comp);
      final ChemEsteroisomerChecker estChecker = new ChemEsteroisomerChecker(comp);
      final ChemIsoChecker isoChecker = new ChemIsoChecker(comp);
      final ChemSugarChecker sugarChecker = new ChemSugarChecker(comp);

      for (WSCompound wscomp : wsChebi) {
        // Casos erroneos propensos a validacion manual
        final int mlv = wscomp.getMinLevenshtein();
        if (mlv > 0) {
          final Set<String> wscompSynList = wscomp.getSynList();

          boolean penalty = true;
          // Checks
          if (digitChecker.check(wscompSynList)) {
            if (estChecker.check(wscompSynList)) {
              if (isoChecker.check(wscompSynList)) {
                if (sugarChecker.check(wscompSynList)) {
                  penalty = false;
                }
              }
            }
          }
          if (penalty) {
            wscomp.setNumMatches(0);
          }
        }
      } // Fin for

      // Ordenar por numero de matches (de mayor a menor)
      Collections.sort(wsChebi, Collections.reverseOrder());
      double max = Collections.max(wsChebi).getNumMatches();
      // Hay matches
      if (max > 0) {
        int lv = Collections.max(wsChebi).getMinLevenshtein();
        // Dejar un margen en Levenshtein para permitir casos dudosos
        lv = lv + Util.LV_MARGIN;

        for (WSCompound wscomp : wsChebi) {
          if (wscomp.getNumMatches() == max && wscomp.getMinLevenshtein() <= lv) {
            // Guardar compuestos validos
            wscompList.add(wscomp);
          }
        }
      }
    } // Fin if

    return wscompList;
  }


  ////////////////////////////////////////////////////////
  //// METODOS PRIVADOS
  ////////////////////////////////////////////////////////

  /**
   * Elimina los candidatos claramente erroneos.
   *
   * @param wscompList
   * @return Lista limpia
   * @throws ChebiException
   */
  private List<WSCompound> discardCandidates(List<WSCompound> wscompList) throws ChebiException {
    int numCandidates = wscompList.size();

    final List<String> chebiIdList = new ArrayList<String>();
    if ((comp.validateCharge() && comp.getCharge() != 0) || !comp.validateCharge()) {
      final List<String> keggIdList = seedDb.getKeggIdList(comp.getCleanSbmlId());
      for (String keggId : keggIdList) {
        for (String seedChebiId : keggDb.getChebiIdList(keggId)) {
          chebiIdList.add("CHEBI:" + seedChebiId);
        }
      }
    }

    for (WSCompound wscomp : wscompList) {
      // Comprobar conjugacion
      for (String seedChebiId : chebiIdList) {
        if (webserviceChebi.isConjugated(wscomp.getChebiId(), seedChebiId)) {
          wscomp.incrementNumMatches(0.5);
          break;
        }
      }

      if (comp.getSbmlName() != null) {
        if (matchName(wscomp)) {
          wscomp.setMatchName(true);
          wscomp.incrementNumMatches(0.5);

          if (!comp.validateCharge()) {
            wscomp.incrementNumMatches(0.25);
          }
        }
        int minLv = matchNameLevenshtein(wscomp);
        wscomp.setMinLevenshtein(minLv);
        // Match
        if (minLv == 0) {
          wscomp.setMatchName(true);
          wscomp.incrementNumMatches(1);

          if (!comp.validateCharge()) {
            wscomp.incrementNumMatches(0.5);
          }
        }
      }

      if (!comp.validateCharge()) {
        // Buscar en la BD con el nuevo comp
        NetCompound tmpComp = new NetCompound(comp.getSpecies());
        tmpComp.update(wscomp);
        DbSearchEngine dbSearch = new DbSearchEngine(tmpComp);
        Status stDb = dbSearch.findChemIdCandidates();
        if (stDb == Status.FOUND) {
          wscomp.incrementNumMatches(0.5);
        }
      }

      if (matchSmiles(wscomp)) {
        wscomp.setMatchSmiles(true);
        wscomp.incrementNumMatches(1);
      }

      if (matchInchi(wscomp)) {
        wscomp.setMatchInchi(true);
        wscomp.incrementNumMatches(1);
      }
      else if (matchInchikey(wscomp)) {
        wscomp.setMatchInchi(true);
        wscomp.incrementNumMatches(1);
      }

      String sbmlFormula = comp.getSbmlFormula();
      if (!wscomp.getFormulaList().isEmpty() && sbmlFormula != null) {
        if (matchFm(wscomp)) {
          wscomp.setMatchFormula(true);
          //double inc = (wscomp.isMatchName()) ? 1 : 0.25;
          wscomp.incrementNumMatches(1);
        }
        else {
          wscomp.setNumMatches(0);
        }
      }

      if (!comp.getKeggIdList().isEmpty()) {
        if (matchKeggId(wscomp)) {
          wscomp.setMatchKeggId(true);
          wscomp.incrementNumMatches(1);
        }
        // Castigar si no coincide ningun keggId
        else if (wscomp.getKeggId() != null) {
          wscomp.setNumMatches(0);
          // TODO log discrepa kegg
        }
      }

      // Premiar si existe carga y coincide
      // Las cargas tienen que ser distintas de cero y tiene que existir 1 o mas matches
      if (comp.validateCharge() && wscomp.getNumMatches() > 0) {
        if (comp.getCharge() == wscomp.getCharge()) {
          // Incrementar si las cargas son cero pero existe fm y syn
          if (comp.getCharge() == 0) {
            if (wscomp.isMatchName() && wscomp.getNumMatches() >= 2) {
              wscomp.incrementNumMatches(0.5);
            }
          }
          // Incrementar si las cargas son distintas de cero
          else {
            //double inc = (wscomp.isMatchName()) ? 1 : 0.25;
            wscomp.incrementNumMatches(1);
          }
        }
        // Descartar casos en los que la carga no coincide
        else {
          wscomp.setNumMatches(0);
          // TODO log
        }
      }

      // Descartar casos con zwitterion
      if (wscomp.getNumMatches() > 0) {
        for (String syn : wscomp.getSynList()) {
          if (syn.contains("zwitterion")) {
            wscomp.setNumMatches(0);
            break;
          }
        }
      }

//      // Penalizar si la masa no coincide
//      if (wscomp.getNumMatches() > 0) {
//        Set<Double> compMassList = comp.getMassList();
//        if (!compMassList.isEmpty() && wscomp.getMass() > 0) {
//          double mass1 = Double.valueOf(df.format(wscomp.getMass()));
//          boolean penalty = true;
//          for (Double mass2 : compMassList) {
//            mass2 = Double.valueOf(df.format(mass2));
//            if (mass1 > 0 && mass2 > 0) {
//              if (mass1 == mass2) {
//                penalty = false;
//              }
//            }
//          }
//          if (penalty) {
//            wscomp.incrementNumMatches(-1);
//          }
//        }
//      }

    } // Fin for

    // Crear la lista definitiva
    List<WSCompound> output = new LinkedList<WSCompound>();

    // Ningun candidato
    if (numCandidates == 0) {
      return output;
    }
    // Un candidato
    else if (numCandidates == 1) {
      WSCompound wscomp = wscompList.get(0);
      if (wscomp.getNumMatches() > 0) {
        // Solo se ha recuperado un compuesto del WS
        wscomp.setUniqueWs(true);
        return wscompList;
      }
      else {
        return output;
      }
    }
    // Mas de un candidato
    else {
      // Ordenar por numero de matches (de mayor a menor)
      Collections.sort(wscompList, Collections.reverseOrder());
      double max = Collections.max(wscompList).getNumMatches();
      // Hay matches
      if (max > 0) {
        for (WSCompound wscomp : wscompList) {
          if (wscomp.getNumMatches() == max) {
            // Guardar compuestos validos
            output.add(wscomp);
          }
        }
      }
    } // Fin if numCandidates

    return output;
  }

  private int matchNameLevenshtein(WSCompound wscomp) {
    int minLevenshtein = 100;

//    // Calcular Levenshtein
//    Set<String> compSynList = comp.getSynList(); // incluye nombre

    // Calcular Levenshtein
    final Set<String> compSynList = new HashSet<String>(comp.getSynList()); // incluye nombre
    // Incluir sinonimos de Seed
    if (comp.validateCharge() && comp.getCharge() != 0) {
      compSynList.addAll(seedDb.getSeedSynList(comp.getCleanSbmlId()));
    }

    String wscompName = wscomp.getName().toLowerCase();
    Set<String> wscompSynList = wscomp.getSynList();

    for (String compSyn : compSynList) {
      compSyn = compSyn.toLowerCase();
      String cleanSyn = mtAlphaNum.reset(compSyn).replaceAll("");

      int lv = dmp.diff_levenshtein(dmp.diff_main(cleanSyn,
              mtAlphaNum.reset(wscompName).replaceAll("")));
      minLevenshtein = (lv < minLevenshtein) ? lv : minLevenshtein;

      if (minLevenshtein == 0)
        return 0;

      String normalSyn = Util.getChemicalName(Util.splitChemicalName(compSyn));

      lv = dmp.diff_levenshtein(dmp.diff_main(normalSyn,
              Util.getChemicalName(Util.splitChemicalName(wscompName))));
      minLevenshtein = (lv < minLevenshtein) ? lv : minLevenshtein;

      if (minLevenshtein == 0)
        return 0;

      for (String wscompSyn : wscompSynList) {
        wscompSyn = wscompSyn.toLowerCase();
        lv = dmp.diff_levenshtein(dmp.diff_main(cleanSyn,
                mtAlphaNum.reset(wscompSyn).replaceAll("")));
        minLevenshtein = (lv < minLevenshtein) ? lv : minLevenshtein;

        if (minLevenshtein == 0)
          return 0;

        lv = dmp.diff_levenshtein(dmp.diff_main(normalSyn,
                Util.getChemicalName(Util.splitChemicalName(wscompSyn))));
        minLevenshtein = (lv < minLevenshtein) ? lv : minLevenshtein;

        if (minLevenshtein == 0)
          return 0;
      }
    }

    return minLevenshtein;
  }

  private boolean matchName(WSCompound wscomp) {
    String compName = comp.getSbmlName().toLowerCase();
    String wscompName = wscomp.getName().toLowerCase();

    if (compName.equals(wscompName)) {
      return true;
    }
    else if (Util.getChemicalName(Util.splitChemicalName(compName)).equals(
            Util.getChemicalName(Util.splitChemicalName(wscompName)))) {
      return true;
    }

    return false;
  }

  private boolean matchInchi(WSCompound wscomp) {
    String inchi = comp.getInchi();
    return (inchi != null && inchi.equals(wscomp.getInchi()));
  }

  private boolean matchInchikey(WSCompound wscomp) {
    String inchi = comp.getInchikey();
    return (inchi != null && inchi.equals(wscomp.getInchiKey()));
  }

  private boolean matchSmiles(WSCompound wscomp) {
    String wscompSm = wscomp.getSmiles();
    if (wscompSm != null) {
      Set<String> smList = comp.getSmilesList();
      for (String compSm : smList) {
        if (compSm.equals(wscompSm)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean matchKeggId(WSCompound wscomp) {
    Set<String> keggList = comp.getKeggIdList();

    String wscompKeggId = wscomp.getKeggId();
    for (String compKeggId : keggList) {
      if (compKeggId.equals(wscompKeggId)) {
        return true;
      }
    }
    return false;
  }

  private boolean matchFm(WSCompound wscomp) {
    String sbmlFormula = comp.getSbmlFormula();
    for (String fm2 : wscomp.getAllFormulas()) {
      if (sbmlFormula.equalsIgnoreCase(fm2)) {
        return true;
      }
    }
    return false;
  }
}
