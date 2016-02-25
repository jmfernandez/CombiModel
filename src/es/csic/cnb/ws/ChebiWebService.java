package es.csic.cnb.ws;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.ebi.chebi.webapps.chebiWS.client.ChebiWebServiceClient;
import uk.ac.ebi.chebi.webapps.chebiWS.model.ChebiWebServiceFault_Exception;
import uk.ac.ebi.chebi.webapps.chebiWS.model.DataItem;
import uk.ac.ebi.chebi.webapps.chebiWS.model.Entity;
import uk.ac.ebi.chebi.webapps.chebiWS.model.LiteEntity;
import uk.ac.ebi.chebi.webapps.chebiWS.model.LiteEntityList;
import uk.ac.ebi.chebi.webapps.chebiWS.model.RelationshipType;
import uk.ac.ebi.chebi.webapps.chebiWS.model.SearchCategory;
import uk.ac.ebi.chebi.webapps.chebiWS.model.StarsCategory;
import uk.ac.ebi.chebi.webapps.chebiWS.model.StructureDataItem;
import es.csic.cnb.data.NetCompound;
import es.csic.cnb.data.WSCompound;
import es.csic.cnb.data.WSCompound.WebService;
import es.csic.cnb.db.DbManager;
import es.csic.cnb.db.KeggDbManager;
import es.csic.cnb.util.Util;
import es.csic.cnb.ws.ChebiException.Field;

public class ChebiWebService {
  private final static Logger LOGGER = Logger.getLogger(ChebiWebService.class.getName());

  private static final int MAX = 15;
  private static final int SLEEP = 3 * 60 * 1000;
  private static final String STATUS_CHECKED = "CHECKED";

  private Matcher mtCharge = Pattern.compile("\\(\\d+[-+]\\)$").matcher("");

  private Matcher mtChebi = Pattern.compile(Util.SOURCE_CHEBI, Pattern.CASE_INSENSITIVE).matcher("");
  private Matcher mtKegg = Pattern.compile(Util.SOURCE_KEGG+".compound", Pattern.CASE_INSENSITIVE).matcher("");

  private ChebiWebServiceClient client;

  public ChebiWebService() throws ChebiException {
    try {
      client = new ChebiWebServiceClient(); // WebServiceException
    }
    catch (Exception e) {
      throw new ChebiException(e);
    }
  }

  /**
   * Busqueda en el Web Service por formula.
   *
   * @param String formula
   * @return Set<WSCompound>
   * @throws ChebiException
   */
  public List<WSCompound> searchByFm(NetCompound comp) throws ChebiException {
    String sbmlFormula = comp.getSbmlFormula();
    return this.searchByFm(sbmlFormula);
  }

  /**
   * Busqueda en el Web Service por formula.
   *
   * @param comp
   * @return Set<WSCompound>
   * @throws ChebiException
   */
  public List<WSCompound> searchByFm(String sbmlFormula) throws ChebiException {
    List<WSCompound> data = new LinkedList<WSCompound>();

    // Formula
    if (sbmlFormula != null) {
      try {
        LiteEntityList list = client.getLiteEntity(sbmlFormula, SearchCategory.FORMULA, MAX, StarsCategory.THREE_ONLY);
        for (LiteEntity lite : list.getListElement()) {
          Entity entity = client.getCompleteEntity(lite.getChebiId());
          if (entity.getStatus().equals(STATUS_CHECKED)) {
            WSCompound wscomp = createWSComp(entity);
            wscomp.setScore(lite.getSearchScore());

            data.add(wscomp);
          }
        }
      } catch(Exception e) {
        if (e instanceof ChebiWebServiceFault_Exception) {
          if (e.getMessage().equals("ERROR (Entity status): " +
                  "the entity in question is deleted, obsolete, or not yet released.")) {
            LOGGER.log(Level.WARNING, "Ignorado error Chebi: {0}", ((ChebiWebServiceFault_Exception)e).getFaultInfo().getFaultInfo());
          }
          else {
            throw new ChebiException(Field.FM, sbmlFormula, e);
          }
        }
        else if (e.getMessage().equals("Unsupported Content-Type: " +
                "text/html Supported ones are: [text/xml]")) {

          LOGGER.log(Level.WARNING, "RELANZAR sbmlFormula={0} ### Se ignora error: {1}",
                  new Object[]{sbmlFormula, e.getMessage()});

          // Esperar y lanzar de nuevo
          try {
            Thread.sleep(SLEEP);
          } catch (InterruptedException e1) {
            throw new ChebiException(Field.FM, sbmlFormula, e1);
          }
          finally {
            searchByFm(sbmlFormula);
          }
        }
      }
    }

    return data;
  }

  /**
   * Busqueda en el Web Service por nombre.
   *
   * @param comp
   * @return Set<WSCompound>
   * @throws ChebiException
   */
  public List<WSCompound> searchByName(NetCompound comp) throws ChebiException {
    List<WSCompound> data = new LinkedList<WSCompound>();

    // Nombre
    if (comp.getSbmlName() != null) {
      try {
        LiteEntityList list = client.getLiteEntity(comp.getSbmlName(), SearchCategory.ALL_NAMES, MAX, StarsCategory.THREE_ONLY);
        for (LiteEntity lite : list.getListElement()) {
          Entity entity = client.getCompleteEntity(lite.getChebiId());
          if (entity.getStatus().equals(STATUS_CHECKED)) {
            WSCompound wscomp = createWSComp(entity);
            wscomp.setScore(lite.getSearchScore());

            data.add(wscomp);
          }
        }
      } catch(Exception e) {
        if (e instanceof ChebiWebServiceFault_Exception) {
          if (e.getMessage().equals("ERROR (Entity status): " +
                  "the entity in question is deleted, obsolete, or not yet released.")) {
            LOGGER.log(Level.WARNING, "Ignorado error Chebi: {0}", ((ChebiWebServiceFault_Exception)e).getFaultInfo().getFaultInfo());
          }
          else {
            throw new ChebiException(Field.NAME, comp.getSbmlName(), e);
          }
        }
        else {
          if (e.getMessage().equals("Unsupported Content-Type: " +
                  "text/html Supported ones are: [text/xml]")) {

            LOGGER.log(Level.WARNING, "RELANZAR SbmlName={0} ### Se ignora error: {1}",
                    new Object[]{comp.getSbmlName(), e.getMessage()});

            // Esperar y lanzar de nuevo
            try {
              Thread.sleep(SLEEP);
            } catch (InterruptedException e1) {
              throw new ChebiException(Field.NAME, comp.getSbmlName(), e1);
            }
            finally {
              searchByName(comp);
            }
          }
        }
      }
    }

    return data;
  }

  /**
   * Busca en Chebi por el Chebi ID.
   *
   * @param comp
   * @return Lista de WSCompound
   * @throws ChebiException
   */
  public List<WSCompound> searchById(NetCompound comp) throws ChebiException {
    List<WSCompound> data = new LinkedList<WSCompound>();

    for (String chebiId : comp.getChebiIdList()) {
      Entity entity = null;
      try {
        entity = client.getCompleteEntity(chebiId);
      } catch(Exception e) {
        if (e instanceof ChebiWebServiceFault_Exception) {
          if (e.getMessage().equals("ERROR (Entity status): " +
                  "the entity in question is deleted, obsolete, or not yet released.")) {
            LOGGER.log(Level.WARNING, "Ignorado error Chebi: {0}", ((ChebiWebServiceFault_Exception)e).getFaultInfo().getFaultInfo());
          }
          else {
            throw new ChebiException(Field.ID, chebiId, e);
          }
        }
        else if (e.getMessage().equals("Unsupported Content-Type: " +
                "text/html Supported ones are: [text/xml]")) {

          LOGGER.log(Level.WARNING, "RELANZAR chebiId={0} ### Se ignora error: {1}",
                  new Object[]{chebiId, e.getMessage()});

          // Esperar y lanzar de nuevo
          try {
            Thread.sleep(SLEEP);
          } catch (InterruptedException e1) {
            throw new ChebiException(Field.ID, chebiId, e1);
          }
          finally {
            searchById(chebiId);
          }
        }
      }

      if (entity != null) {
        data.add(createWSComp(entity));
      }
    }

    return data;
  }

  /**
   * Busca en Chebi por el Chebi ID.
   *
   * @param chebiId
   * @return WSCompound or null si no existe Chebi ID o no se recupera nada
   * @throws ChebiException
   */
  public WSCompound searchById(String chebiId) throws ChebiException {
    WSCompound wscomp = null;

    if (chebiId != null) {
      Entity entity = null;
      try {
        entity = client.getCompleteEntity(chebiId);
      } catch(Exception e) {
        if (e instanceof ChebiWebServiceFault_Exception) {
          if (e.getMessage().equals("ERROR (Entity status): " +
                  "the entity in question is deleted, obsolete, or not yet released.")) {
            LOGGER.log(Level.WARNING, "Ignorado error Chebi: {0}", ((ChebiWebServiceFault_Exception)e).getFaultInfo().getFaultInfo());
          }
          else {
            throw new ChebiException(Field.ID, chebiId, e);
          }
        }
        else if (e.getMessage().equals("Unsupported Content-Type: " +
                "text/html Supported ones are: [text/xml]")) {

          LOGGER.log(Level.WARNING, "RELANZAR chebiId={0} ### Se ignora error: {1}", new Object[]{chebiId, e.getMessage()});

          // Esperar y lanzar de nuevo
          try {
            Thread.sleep(SLEEP);
          } catch (InterruptedException e1) {
            throw new ChebiException(Field.ID, chebiId, e1);
          }
          finally {
            searchById(chebiId);
          }
        }
      }
      wscomp = (entity == null) ? null : createWSComp(entity);
    }

    return wscomp;
  }

  /**
   * Metodo para determinar si un chebi es acido/base respecto a otro.
   * @param candidateChebiId
   * @param seedChebiId
   * @throws ChebiException
   * @return true si son conjugados.
   */
  public boolean isConjugated(String candidateChebiId, String seedChebiId) throws ChebiException {
    boolean ok = false;

    try {
      final LiteEntityList listAcid = client.getAllOntologyChildrenInPath(
              candidateChebiId, RelationshipType.IS_CONJUGATE_ACID_OF, true);
      for (LiteEntity liteAcid : listAcid.getListElement()) {
        String id = liteAcid.getChebiId();
        if (id.equalsIgnoreCase(seedChebiId)) {
          ok = true;
        }
      }

      if (!ok) {
        final LiteEntityList listBase = client.getAllOntologyChildrenInPath(
                candidateChebiId, RelationshipType.IS_CONJUGATE_BASE_OF, true);
        for (LiteEntity liteBase : listBase.getListElement()) {
          String id = liteBase.getChebiId();
          if (id.equalsIgnoreCase(seedChebiId)) {
            ok = true;
          }
        }
      }
    } catch(Exception e) {
      if (e instanceof ChebiWebServiceFault_Exception) {
        if (e.getMessage().equals("ERROR (Entity status): " +
                "the entity in question is deleted, obsolete, or not yet released.")) {
          LOGGER.log(Level.WARNING, "Ignorado error Chebi: {0}", ((ChebiWebServiceFault_Exception)e).getFaultInfo().getFaultInfo());
        }
        else {
          throw new ChebiException(Field.ID, candidateChebiId, e);
        }
      }
      else if (e.getMessage().equals("Unsupported Content-Type: " +
              "text/html Supported ones are: [text/xml]")) {

        LOGGER.log(Level.WARNING, "RELANZAR chebiId={0} ### Se ignora error: {1}",
                new Object[]{candidateChebiId, e.getMessage()});

        // Esperar y lanzar de nuevo
        try {
          Thread.sleep(SLEEP);
        } catch (InterruptedException e1) {
          throw new ChebiException(Field.ID, candidateChebiId, e1);
        }
        finally {
          isConjugated(candidateChebiId, seedChebiId);
        }
      }
    }

    return ok;
  }

  /**
   * Metodo interno que permite crear objetos WSCompound a partir de los datos
   * recuperados por el Web Service.
   *
   * @param entity
   * @return WSCompound
   * @throws SQLException
   */
  private WSCompound createWSComp(Entity entity) {
    WSCompound wscomp = new WSCompound(WebService.CHEBI, entity.getChebiId());
    wscomp.setName(entity.getChebiAsciiName());
    wscomp.setInchi(entity.getInchi());
    wscomp.setInchiKey(entity.getInchiKey());
    wscomp.setSmiles(entity.getSmiles());
    wscomp.addXref(Util.SOURCE_CHEBI, entity.getChebiId());
    wscomp.setMapping(Util.MAPPING_AUTO);

    // Structure
    for (StructureDataItem st : entity.getChemicalStructures()) {
      if (st.isDefaultStructure()) {
        wscomp.setStructure(st.getStructure());
      }
    }

    // Mass
    double mass = (entity.getMass() == null) ? 0.0 : Double.parseDouble(entity.getMass());
    wscomp.setMass(mass);

    // Carga
    int charge = (entity.getCharge() == null) ? 0 : Integer.parseInt(entity.getCharge().replaceAll("\\+", ""));
    wscomp.setCharge(charge);
    // Si existe carga en el nombre se crea sinonimo sin ella
    wscomp.addSyn(mtCharge.reset(entity.getChebiAsciiName()).replaceAll(""));

    // Sinonimos
    for (DataItem dataItem : entity.getSynonyms()) {
      wscomp.addSyn(dataItem.getData());
    }
    // iupac
    for (DataItem dataItem : entity.getIupacNames()) {
      wscomp.addSyn(dataItem.getData());
    }
    // Formula
    for (DataItem dataItem : entity.getFormulae()) {
      if (dataItem != null) {
        String fm = dataItem.getData().replaceAll("\\s", ""); // A veces aparecen blancos
        fm = Util.getFormulaCorrected(fm);
        wscomp.addFormula(Util.SOURCE_FORMULA_CHEBI, fm);
      }
    }
    // xrefs
    for (DataItem dataItem : entity.getDatabaseLinks()) {
      String source = dataItem.getType();
      // Normalizar nombres de kegg y chebi
      mtChebi.reset(source);
      if (mtChebi.find()) {
        source = Util.SOURCE_CHEBI;
      }
      mtKegg.reset(source);
      if (mtKegg.find()) {
        source = Util.SOURCE_KEGG;

        // Recuperar datos de kegg db
        String keggId = dataItem.getData();
        KeggDbManager keggDb = DbManager.INSTANCE.getKeggDbMgr();
        List<String> synList = keggDb.getSynList(keggId);
        for (String syn : synList) {
          wscomp.addSyn(syn);
        }
        String formula = keggDb.getFormula(keggId);
        if (formula != null) {
          formula = formula.replaceAll("\\s", ""); // A veces aparecen blancos
          formula = Util.getFormulaCorrected(formula);
          wscomp.addFormula(Util.SOURCE_FORMULA_OTHER, formula);
        }
      }
      wscomp.addXref(source, dataItem.getData());
    }
    return wscomp;
  }
}
