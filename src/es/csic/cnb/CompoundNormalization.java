package es.csic.cnb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.csic.cnb.data.NetCompound;
import es.csic.cnb.data.WSCompound;
import es.csic.cnb.db.CompoundDbManager;
import es.csic.cnb.db.DbManager;
import es.csic.cnb.util.Util.Status;
import es.csic.cnb.ws.ChebiException;

/**
 * Acceso a base de datos para ver si el compuesto aparece alli.
 * Si no es asi se recurre a web services que aportan informacion que
 * se guarda en la base de datos.
 * Por tanto todo compuesto que no aparece en la base de datos pasa a traves
 * de los web services antes de almacenarse en la base de datos.
 * Si los web services no recuperan informacion se hace un tratamiento manual.
 *
 * @author Pablo D. Sánchez
 *
 */
public class CompoundNormalization {
  private final static Logger LOGGER = Logger.getLogger(CompoundNormalization.class.getName());

  // Ids procesados
  private static Map<String,Integer> idList = new HashMap<String,Integer>();
  private static Map<Integer,Status> idListStatus = new HashMap<Integer,Status>();

  private CompoundDbManager db;

  public CompoundNormalization() {
    db = DbManager.INSTANCE.getCompoundDbMgr();

    idList.clear();
    idListStatus.clear();
  }

  public void normalize(NetCompound comp) throws ChebiException {
    String cleanSbmlId = comp.getCleanSbmlId();

    // No mapear los ids ya procesados
    if (!comp.isManualCuration() && idList.containsKey(cleanSbmlId)) {
      int chemId = idList.get(cleanSbmlId);
      // Se guarda el id
      comp.setChemId(chemId);

      // Comprobar que esta como exchange
      if (comp.isExchangeCompound()) {
        if (chemId != 0) {
          db.exchangeReaction(chemId);
        }
      }

      if (LOGGER.isLoggable(Level.INFO)) {
        Status status = idListStatus.get(chemId);
        if (status == Status.FOUND) {
          LOGGER.log(Level.INFO, "MAPPING FOUND");
        }
        else if (status == Status.NOTFOUND) {
          LOGGER.log(Level.INFO, "MAPPING NOTFOUND");
        }
        else if (status == Status.DUDE) {
          LOGGER.log(Level.INFO, "MAPPING DUDE");
        }
      }
      else if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.log(Level.FINE, ":: Evaluado {0} [chemId: {1}]", new Object[]{comp, chemId});
      }
    }
    // Mapear
    else {
      List<Object> clist = new ArrayList<Object>();
      Status stDb = null;
      Status stWs = null;

      // 1. Buscar en base de datos
      // --------------------------------
      DbSearchEngine dbSearch1 = new DbSearchEngine(comp);
      Status stDb1 = dbSearch1.findChemIdCandidates();

      if (LOGGER.isLoggable(Level.INFO)) {
        stDb = stDb1;
        clist.addAll(dbSearch1.getChemIdCandidateList());
      }
      else if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.log(Level.FINE, "1.DB {0} - candidates: {1}",
                new Object[]{stDb1, dbSearch1.getChemIdCandidateList()});

        if (LOGGER.isLoggable(Level.FINEST) && stDb1 != Status.NOTFOUND) {
          LOGGER.log(Level.FINEST, "1.DB candidates detail: {0}", dbSearch1);
        }
      }

      // 1.1. Busqueda 1 en BD encontrado (solo deberia haber un candidato)
      if (stDb1 == Status.FOUND) {
        List<Integer> chemIdList = dbSearch1.getChemIdCandidateList();

        assert (chemIdList.size() == 1) :
        "Error: multiples candidatos (solo debe haber uno) >>> " + chemIdList;

        int chemId = chemIdList.get(0);
        // Se guarda el id
        comp.setChemId(chemId);
        // Actualizar base de datos
        db.updateDb(chemId, comp);
      }

      // 1.2 Busqueda 1 en BD no encontrado
      else if (stDb1 == Status.NOTFOUND) {
        // 2. Buscar en web services cuando no se encuentra en la base de datos
        // --------------------------------
        WsSearchEngine wsSearch2 = new WsSearchEngine(comp);
        Status stWs2 = wsSearch2.findCandidates();

        if (LOGGER.isLoggable(Level.INFO)) {
          stWs = stWs2;
          clist.addAll(wsSearch2.getCandidateList());
        }
        else if (LOGGER.isLoggable(Level.FINE)) {
          LOGGER.log(Level.FINE, " 2.WS {0} - candidates: {1}",
                  new Object[]{stWs2, wsSearch2.getCandidateList()});
        }

        // 2.1 Busqueda 2 en WS encontrado (puede haber varios candidatos)
        if (stWs2 == Status.FOUND) {
          // Actualizar compuesto con datos de los candidatos
          // Si viene de ChebiId puede haber varios candidatos
          List<WSCompound> wscompList = wsSearch2.getCandidateList();
          for (WSCompound wscomp : wscompList) {
            comp.update(wscomp);
          }

          // 3. Buscar de nuevo en la BD
          // --------------------------------
          DbSearchEngine dbSearch3 = new DbSearchEngine(comp);
          Status stDb3 = dbSearch3.findChemIdCandidates();

          if (LOGGER.isLoggable(Level.INFO)) {
            stDb = stDb3;
            clist.addAll(dbSearch3.getChemIdCandidateList());
          }
          else if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "  3.DB {0} - candidates: {1}",
                    new Object[]{stDb3, dbSearch3.getChemIdCandidateList()});

            if (LOGGER.isLoggable(Level.FINEST) && stDb3 != Status.NOTFOUND) {
              LOGGER.log(Level.FINEST, "  3.DB candidates detail: {0}", dbSearch3);
            }
          }

          // 3.1 Busqueda 3 en DB encontrado (solo un candidato)
          if (stDb3 == Status.FOUND) {
            List<Integer> chemIdList = dbSearch3.getChemIdCandidateList();

            assert (chemIdList.size() == 1) :
            "Error: multiples candidatos (solo debe haber uno) >>> " + chemIdList;

            int chemId = chemIdList.get(0);
            // Actualizar la base de datos
            db.updateDb(chemId, comp);
          }

          // 3.2 Busqueda 3 en DB no encontrado
          else if (stDb3 == Status.NOTFOUND) {
            // Insertar el compuesto en la base de datos
            db.insertInDb(comp);
          }

          // 3.3 Busqueda 3 en DB con resultados pendientes de curacion manual
          else if (stDb3 == Status.DUDE) {
            // Pendiente de curacion manual (DB)
            comp.setPendingCurationDbMap(true);
          }
        }

        // 2.3 Busqueda 2 en WS no encontrado
        else if (stWs2 == Status.NOTFOUND) {
          // Insertar el compuesto en la base de datos
          db.insertInDb(comp);
        }

        // 2.3 Busqueda 2 en WS con resultados pendientes de curacion manual
        else if (stWs2 == Status.DUDE) {
          // Pendiente de curacion manual (WS)
          comp.setPendingCurationXrefMap(true);
        }
      }

      // Busqueda 1 en BD con resultados pendientes de curacion manual
      else if (stDb1 == Status.DUDE) {
        comp.setPendingCurationDbMap(true);
      }

      // Actualizar chemIDList
      idList.put(cleanSbmlId, comp.getChemId());
      idListStatus.put(comp.getChemId(), stWs);

      if (LOGGER.isLoggable(Level.INFO)) {
        if (stDb == Status.NOTFOUND && stWs == Status.NOTFOUND) {
          LOGGER.log(Level.INFO, "MAPPING NOTFOUND");
        }
        else if (stWs == Status.NOTFOUND || stWs == null) {
          LOGGER.log(Level.INFO, "MAPPING {0} {1}", new Object[]{stDb, clist});
        }
        else if (stDb == Status.NOTFOUND) {
          LOGGER.log(Level.INFO, "MAPPING {0} {1}", new Object[]{stWs, clist});
        }
        else {
          LOGGER.log(Level.INFO, "MAPPING DB {0} / WS {1} - candidates: {2}", new Object[]{stDb, stWs, clist});
        }
      }
    } // Fin else
  }
}
