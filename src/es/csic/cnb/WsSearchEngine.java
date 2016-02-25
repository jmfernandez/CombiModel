package es.csic.cnb;

import java.util.LinkedList;
import java.util.List;

import es.csic.cnb.data.NetCompound;
import es.csic.cnb.data.WSCompound;
import es.csic.cnb.db.DbManager;
import es.csic.cnb.db.KeggDbManager;
import es.csic.cnb.util.Util;
import es.csic.cnb.util.Util.Status;
import es.csic.cnb.ws.ChebiException;
import es.csic.cnb.ws.ChebiWebService;

public class WsSearchEngine {
  //private final static Logger LOGGER = Logger.getLogger(WsSearchEngine.class.getName());

  private List<WSCompound> wscompList;

  private NetCompound comp;

  private ChebiWebService wsChebi;

  public WsSearchEngine(NetCompound comp) throws ChebiException {
    this.comp = comp;
    wscompList = new LinkedList<WSCompound>();

    wsChebi = new ChebiWebService();
  }

  public Status findCandidates() throws ChebiException {
    // Status
    Status st = Status.NOTFOUND;

    // WSMapper busca nuevos candidatos (por nombre, formula, etc) y
    // realiza el mapeado de resultados
    WsMapper wsmapper = new WsMapper(wsChebi, comp);


    // Ver si existe referencia a Kegg
    List<WSCompound> chebiFromKeggList = new LinkedList<WSCompound>();

    List<String> keggIdList = DbManager.INSTANCE.getSeedDbMgr().getKeggIdList(comp.getCleanSbmlId());
    if (!keggIdList.isEmpty()) {
      KeggDbManager keggDb = DbManager.INSTANCE.getKeggDbMgr();
      for (String keggId : keggIdList) {
        // Chebi
        for (String chebiId : keggDb.getChebiIdList(keggId)) {
          chebiId = "CHEBI:" + chebiId;

          WSCompound wscomp = wsChebi.searchById(chebiId);
          // Guardar en la lista
          if (wscomp != null)
            chebiFromKeggList.add(wscomp);
        }
      }
    }


    // Validar chebiIds si la carga es cero
    if (!chebiFromKeggList.isEmpty() && comp.validateCharge() && comp.getCharge() == 0) {
      wsmapper.addAllChebiIdComp(chebiFromKeggList);

      // Comprobar las cargas
      for (WSCompound wscomp : wsmapper.mapCompounds()) {
        if (comp.getCharge() == wscomp.getCharge()) {
          wscompList.add(wscomp);
        }
      }
    }

    // Si existe chebiIds (comprobar cargas)
    if (comp.validateCharge() && !comp.getChebiIdList().isEmpty()) {
      wsmapper.addAllChebiIdComp(wsChebi.searchById(comp));

      // Comprobar las cargas
      for (WSCompound wscomp : wsmapper.mapCompounds()) {
        if (comp.getCharge() == wscomp.getCharge()) {
          wscompList.add(wscomp);
        }
      }
    }

    // Si existe chebiIds
    if (!wscompList.isEmpty()) {
      st = Status.FOUND;
    }

    // No existen chebiIds
    else {
      // Ver si existe referencia a Kegg para recuperar ChebiIds candidatos
      // Solo se hace si la carga es diferente de cero o no hay carga
      if (!chebiFromKeggList.isEmpty()) {
        if ((comp.validateCharge() && comp.getCharge() != 0) || !comp.validateCharge()) {
          wsmapper.addAllChebiIdComp(chebiFromKeggList);
        }
      }

      // Formula
      if (comp.getSbmlFormula() != null) {
        wsmapper.addAllChebiFmComp(wsChebi.searchByFm(comp));
      }
      // Nombres y estructuras
      wsmapper.addAllChebiGralComp(wsChebi.searchByName(comp));

      // Resultado de los web services
      wscompList = wsmapper.mapCompounds();

      int size = wscompList.size();
      if (size == 1) {
        if (wscompList.get(0).getNumMatches() <= 1) { // XXX 2
          st = Status.DUDE;
        }
        else {
          st = Status.FOUND;
        }
      }
      else if (size > 1) {
        st = Status.DUDE;
      }
    }

    return st;
  }

  public List<WSCompound> getCandidateList() {
    return wscompList;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(comp).append(" > ").append(wscompList);
    sb.append(Util.NEWLINE);

    return sb.toString();
  }
}
