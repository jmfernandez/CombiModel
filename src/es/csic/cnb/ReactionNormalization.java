package es.csic.cnb;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.csic.cnb.data.NetReaction;
import es.csic.cnb.db.ReactionDbManager;
import es.csic.cnb.util.Util;

public class ReactionNormalization {
  private final static Logger LOGGER = Logger.getLogger(ReactionNormalization.class.getName());

  private ReactionDbManager db;

  public ReactionNormalization() {
    db = ReactionDbManager.INSTANCE;
  }

  public NetReaction normalize(NetReaction reac) {
    boolean found = false;

    // 1. Buscar en base de datos
    int rxId = this.searchInDb(reac, false);
    // Existe el compuesto en la base datos
    if (rxId != -1) {
      // Se guarda el id
      reac.setRxId(rxId);
      // Actualizar base de datos
      updateDb(rxId, reac);
      // Se marca como encontrado
      found = true;
    }

    // 2. No se ha encontrado en la base de datos
    if (!found) {
      // Insertar en la base de datos
      insertInDb(reac);

      LOGGER.log(Level.FINE, ":: No encontrado rx. en ddbb {0}", reac);
    }

    return reac;
  }


  /**
   * Busqueda en la base de datos.
   *
   * @param reac
   * @return el identificador del compuesto o -1 si no existe
   */
  protected int searchInDb(NetReaction reac, boolean manualValidation) {
    int rxId = -1;

    // Lista para verificar los ids recuperados
    Set<Integer> idList = new HashSet<Integer>();

    if (reac.getSbmlId() != null) {
      idList.addAll(db.getRxIdForSbmlId(reac.getSbmlId()));
    }

    // Buscar por xref
    idList.addAll(db.getRxIdListForEc(reac.getEc()));
    idList.addAll(db.getRxIdListForKegg(reac.getKeggId()));

    // Se ha encontrado id
    if (!idList.isEmpty()) {
      // Verificar que solo hay un id
      if (idList.size() == 1) {
        rxId = idList.iterator().next();

        // Verificar datos
        if (this.checkData(rxId, reac) == 0) {
          rxId = -1;
        }
      }
      // Si hay mas de un id
      else {
        // Verificar datos y quedarnos con el que mas se ajusta
        int bestMatch = 0;
        int bestId = -1;
        for (int id : idList) {
          int match = this.checkData(id, reac);
          if (match > bestMatch) {
            bestMatch = match;
            bestId = id;
          }
        }
        rxId = bestId;
      }

      if (LOGGER.isLoggable(Level.FINEST)) {
        String val = String.valueOf(rxId) + ((rxId > 0) ? " [" + db.getName(rxId) + "]" : "");
        LOGGER.log(Level.FINEST, "Ids rx. candidatos {0} - Seleccionado: {1}", new Object[]{idList, val});
      }
    }

    // Casos que corresponden a una entrada de la bd
    if (rxId != -1) {
      LOGGER.log(Level.FINER,
              "Encontrado rx. candidato {0} en ddbb: {1}",
              new Object[]{String.valueOf(rxId), db.getName(rxId)});

      // Validar manualmente aquellos casos que corresponden a una entrada de la bd
      if (manualValidation) {
        // TODO GUI de validacion - recuperar datos de la bd y comparar con compuesto
        //System.err.println("Validar manualmente: "+chemId);

      }
    }
    // Casos no encontrados
    else if (LOGGER.isLoggable(Level.FINER)) {
      LOGGER.log(Level.FINER, "No encontrado rx. candidato en ddbb");
    }

    return rxId;
  }


  /**
   * Metodo privado que devuelve el numero de coincidencias entre la reaccion
   * y lo recuperado de la base de datos.
   * @param rxId
   * @param reac
   * @return el numero de matches entre la reaccion y lo recuperado de la base de datos
   */
  private int checkData(int rxId, NetReaction reac) {
    int numMatches = 0;

    // Base de datos
    String name = db.getName(rxId);
    String dbEc = db.getEc(rxId);
    String dbKeggId = db.getKeggId(rxId);
    List<String> dbSbmlIdList = db.getSbmlIdList(rxId);

    // Reaccion
    String sbmlId = reac.getSbmlId();
    String sbmlName = reac.getSbmlName();


    if (sbmlName != null && sbmlName.equalsIgnoreCase(name)) {
      numMatches += 1;
    }

    // Comprobar reactantes y productos
    int matchCpd = 0;
    // TODO
    Map<Integer,Double> dbList = new HashMap<Integer,Double>();
    dbList.putAll(db.getReactantList(rxId));
    dbList.putAll(db.getProductList(rxId));

    Map<Integer,Double> sbmlList = new HashMap<Integer,Double>();
    sbmlList.putAll(reac.getReactantList());
    sbmlList.putAll(reac.getProductList());

    for (int chemId : sbmlList.keySet()) {
//      // Uso coef estequiometrico
//      if (dbList.containsKey(chemId) &&
//              sbmlList.get(chemId).equals(dbList.get(chemId))) {
//        matchCpd++;
//      }
      if (dbList.containsKey(chemId)) {
        matchCpd++;
      }
    }

    if (dbList.size() == matchCpd) {
      numMatches += 3;
    }
    else {
      // No match
      numMatches = 0;

      LOGGER.log(Level.FINEST,
              "No coinciden rx. compuestos {0}: {1} >>> DB[{2}: {3}] con {4}",
              new Object[]{sbmlList, reac, String.valueOf(rxId), name, dbList});

      return numMatches;
    }


    // Comprobar xrefs :::::::::::::::::::::::::
    // EC
    String reacEc = reac.getEc();
    if (reacEc != null && dbEc != null) {
      if (reacEc.equals(dbEc)) {
        //numMatches += 1;
      }
      else {
        // No match
        numMatches = 0;

        LOGGER.log(Level.FINEST,
                "No coincide rx. EC {0}: {1} >>> DB[{2}: {3}] con {4}",
                new Object[]{reacEc, reac, String.valueOf(rxId), name, dbEc});

        return numMatches;
      }
    } // Fin EC

    // KEGG
    String reacKeggId = reac.getKeggId();
    if (reacKeggId != null && dbKeggId != null) {
      if (reacKeggId.equals(dbKeggId)) {
        numMatches += 3;
      }
      else {
        // No match
        numMatches = 0;

        LOGGER.log(Level.FINEST,
                "No coincide rx. KEGG {0}: {1} >>> DB[{2}: {3}] con KEGG {4}",
                new Object[]{reacKeggId, reac, String.valueOf(rxId), name, dbKeggId});

        return numMatches;
      }
    } // Fin KEGG

    // ID SBML
    if (sbmlId != null) {
      boolean diffId = false;

      for (String dbSbmlId : dbSbmlIdList) {
        if (sbmlId.equals(dbSbmlId)) {
          numMatches++;
          diffId = false;
          break;
        }
        else {
          diffId = true;
        }
      }

      if (diffId && numMatches <= 1) {
        // No match
        numMatches = 0;

        LOGGER.log(Level.FINEST,
                "No coincide rx. id-sbml ({0}): {1} >>> DB[{2}: {3}] con id-sbml ({4}) Num Mathes <= 1",
                new Object[]{sbmlId, reac, String.valueOf(rxId), name, dbSbmlIdList});

        return numMatches;
      }
    } // Fin ID SBML

    LOGGER.log(Level.FINEST,
            "{0} ==> chemId {1}: {2} [Matches: {3}]",
            new Object[]{reac, String.valueOf(rxId), name, numMatches});

    return numMatches;
  }

  /**
   * Metodo interno que inserta nuevos datos de la reaccion en la base de datos.
   * @param rxId
   * @param reac
   */
  private void updateDb(int rxId, NetReaction reac) {
    if (reac.isExchangeReaction()) {
      db.exchangeReaction(rxId);
    }

    // Xrefs (incluye nombres, inchi, smile)
    Set<String> xrefDbList = db.getXrefList(rxId); // Cache

    if (reac.getSbmlId() != null &&
            !xrefDbList.contains(reac.getSbmlId())) {
      db.insertReacXref(rxId, Util.SOURCE_SBML, reac.getSbmlId());
      xrefDbList.add(reac.getSbmlId());
    }
    if (reac.getEc() != null &&
            !xrefDbList.contains(reac.getEc())) {
      db.insertReacXref(rxId, Util.SOURCE_EC, reac.getEc());
      xrefDbList.add(reac.getEc());
    }
    if (reac.getKeggId() != null &&
            !xrefDbList.contains(reac.getKeggId())) {
      db.insertReacXref(rxId, Util.SOURCE_KEGG, reac.getKeggId());
      xrefDbList.add(reac.getKeggId());
    }

    db.incrementFrequency(rxId);
  }

  /**
   * Metodo interno que inserta los datos de la reaccion en la base de datos.
   * @param reac
   * @return
   */
  private int insertInDb(NetReaction reac) {
    int rxId = db.getNextId();

    reac.setRxId(rxId);

    if (reac.getSbmlName() != null) {
      db.insertReaction(rxId, reac.getSbmlName(), reac.isReversible());
    }
    else {
      db.insertReaction(rxId, reac.getSbmlId(), reac.isReversible());
    }

    if (reac.isExchangeReaction()) {
      db.exchangeReaction(rxId);
    }

    //    // TODO Sinonimos
    //    for (String syn : reac.getSynList()) {
    //      db.insertCompSyn(rxId, syn);
    //    }

    // Xrefs
    Map<String,String> xrefList = reac.getXrefList();
    for (String source : xrefList.keySet()) {
      db.insertReacXref(rxId, source, xrefList.get(source));
    }

    // Compuestos
    for (int chemId : reac.getReactantList().keySet()) {
      db.insertReactant(rxId, chemId, reac.getReactantList().get(chemId));
    }
    for (int chemId : reac.getProductList().keySet()) {
      db.insertProduct(rxId, chemId, reac.getProductList().get(chemId));
    }

    return rxId;
  }
}
