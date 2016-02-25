import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.csic.cnb.data.NetCompound;
import es.csic.cnb.data.WSCompound;
import es.csic.cnb.db.CompoundDbManager;
import es.csic.cnb.db.DbManager;
import es.csic.cnb.util.Util;
import es.csic.cnb.util.Util.Status;

public class CurationFileSearchEngine {
  private final static Logger LOGGER = Logger.getLogger(CurationFileSearchEngine.class.getName());

  private NetCompound comp;

  private int chemId = -1;
  private WSCompound wscomp = null;

  private CompoundDbManager db;
  private CurationFileManager cfMgr;

  public CurationFileSearchEngine(NetCompound comp) {
    this.comp = comp;

    db = DbManager.INSTANCE.getCompoundDbMgr();
    cfMgr = new CurationFileManager();
  }

  /**
   * En compuestos pendientes de validacion manual (PARA DB), compruebo el fichero
   * de correcciones manuales (historico)
   *
   * @return Status
   * @throws SQLException
   */
  public Status findEntryForDb() {
    // Status
    Status st = Status.NOTFOUND;

    String refSbmlId = cfMgr.getDbManualCurationRefSbmlId(comp.getCleanSbmlId());

    if (refSbmlId != null) {
      if (refSbmlId.equals(Util.ENTRY_NONE)) {
        st = Status.FOUND_ENTRYNONE;

        LOGGER.log(Level.FINE, "FOUND_ENTRYNONE :: Existe validacion manual previa [DB] {0}", refSbmlId);
      }
      else {
        List<Integer> list = db.getChemIdForSbmlId(refSbmlId);
        if (list.size() == 1) {
          st = Status.FOUND;
          chemId = list.get(0);

          LOGGER.log(Level.FINE, "FOUND :: Existe validacion manual previa [DB] {0}", chemId);
        }
      }
    }
    return st;
  }

  public int getChemId() {
    return chemId;
  }

  /**
   * Si el compuesto esta pendiente de validacion manual (PARA WEB SERVICES),
   * compruebo el fichero de correcciones manuales (historico)
   *
   * @param wscompList
   * @return Status
   */
  public Status findEntryForXref(List<WSCompound> wscompList) {
    // Status
    Status st = Status.NOTFOUND;

    String chebiId = cfMgr.getXrefManualCurationChebiId(comp.getCleanSbmlId());

    if (chebiId != null) {
      if (chebiId.equals(Util.ENTRY_NONE)) {
        st = Status.FOUND_ENTRYNONE;

        LOGGER.log(Level.FINE, "FOUND_ENTRYNONE :: Existe validacion manual previa [CHEBI] {0}", chebiId);
      }
      else {
        for (WSCompound wsc : wscompList) {
          if (wsc.getChebiId().equals(chebiId)) {
            st = Status.FOUND;
            wscomp = wsc;

            LOGGER.log(Level.FINE, "FOUND :: Existe validacion manual previa [CHEBI] {0}", chebiId);

            break;
          }
        }
      }
    }

    return st;
  }

  public WSCompound getWSCompound() {
    return wscomp;
  }
}
