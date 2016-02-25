package es.csic.cnb.curation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import es.csic.cnb.CompoundNormalization;
import es.csic.cnb.DbSearchEngine;
import es.csic.cnb.WsSearchEngine;
import es.csic.cnb.data.NetCompound;
import es.csic.cnb.data.WSCompound;
import es.csic.cnb.data.WSCompound.WebService;
import es.csic.cnb.db.CompoundDbManager;
import es.csic.cnb.db.DbManager;
import es.csic.cnb.log.LogMgr;
import es.csic.cnb.rmi.ProgressData;
import es.csic.cnb.util.Util;
import es.csic.cnb.util.Util.Status;
import es.csic.cnb.ws.ChebiException;

public class ManualCurationManager {
  private final static Logger LOGGER = Logger.getLogger(ManualCurationManager.class.getName());

  private static final String TAB = "\t";

  private DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyy HH:mm:ss");

  private File compCurationFile = new File(Util.USERDIR_CURATIONFILES, Util.COMPOUNDS_CURATIONFILE);

  private SBMLReader reader;
  private SBMLWriter wr;

  private Map<String, NetCompound> mapComp;
  private LinkedBlockingDeque<NetCompound> queue;

  private ProgressData progressData;

  private int curAuto;
  private Set<String> curMan;

  private boolean stopLoad = false;
  private boolean isRunning = false;

  private SBMLDocument docManTemplate = null;

  private XStream xstream;

  public ManualCurationManager() {
    reader = new SBMLReader();
    wr = new SBMLWriter();

    mapComp = new HashMap<String, NetCompound>();
    queue = new LinkedBlockingDeque<NetCompound>();

    progressData = new ProgressData();

    curMan = new HashSet<String>();

    xstream = new XStream(new StaxDriver());

    // Arrancar logs
    LogMgr log = new LogMgr();
    log.configureLog();

    DbManager.INSTANCE.connect();

//    // BORRAR XXX
//      loadManCurationFile();
  }


  public void loadManualCurationData() throws InterruptedException {
    stopLoad = false;
    isRunning = true;

    progressData.setInf("Loading compounds...");
    progressData.setTotal(0);
    progressData.setPercentaje(0);

    queue.clear();
    mapComp.clear();

    curAuto = 0;
    curMan.clear();

    try {
      if (compCurationFile.exists()) {
        docManTemplate = reader.readSBML(compCurationFile);
      }
      else {
        docManTemplate = reader.readSBMLFromStream(
              this.getClass().getResourceAsStream("/resources/template.dat"));
      }
    } catch (XMLStreamException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Recorrer xmls en "/curation/files/"
    File dir = new File(Util.USERDIR_CURATIONFILES);
    for (File file : dir.listFiles()) {
      if (file.equals(compCurationFile)) continue;

      // Para cada xml
      if (file.getName().endsWith(".xml")) {
        progressData.setInf("Loading " + file.getName());
        progressData.setTotal(0);
        progressData.setPercentaje(0);

        SBMLDocument docManCuration = null;
        try {
          docManCuration = reader.readSBML(file);
        } catch (XMLStreamException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }

        // Volcar a template
        Model templateModel = docManTemplate.getModel();
        for (Compartment cpt : docManCuration.getModel().getListOfCompartments()) {
          if (!templateModel.containsCompartment(cpt.getId()))
            templateModel.addCompartment(cpt);
        }
        for (Species sp : docManCuration.getModel().getListOfSpecies()) {
          if (templateModel.containsSpecies(sp.getId())) {
            Species spTemplate = templateModel.getSpecies(sp.getId());
            if (Util.isExchangeCompound(sp) && !Util.isExchangeCompound(spTemplate)) {
              // Anadir al xml una referencia que indica que es compuesto de intercambio
              spTemplate.addCVTerm(Util.getExchangeCVTerm());
            }
          }
          else if (!Util.isCuratedCompound(sp)) {
            templateModel.addSpecies(sp);
          }
        }
        // Borrar fichero
        file.delete();
      }
    } // Fin for

    try {
      SBMLWriter.write(docManTemplate,
              compCurationFile, "manual", "1");

    } catch (SBMLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } catch (XMLStreamException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }


    int dorsal = 1;
    // Curar manualmente los compuestos
    int num = docManTemplate.getModel().getNumSpecies();
    int cont = 0;
    for (Species sp : docManTemplate.getModel().getListOfSpecies()) {
      if (!Util.isCuratedCompound(sp)) {
        cont++;
        progressData.setInf("Checking " + sp.getId());
        progressData.setTotal(cont);
        progressData.setPercentaje(cont*100/num);
        NetCompound comp = new NetCompound(sp);
        comp.setDorsal(dorsal);
        if (!mapComp.containsKey(comp.getCleanSbmlId())) {
          mapComp.put(comp.getCleanSbmlId(), comp);
          queue.put(comp);
          dorsal++;
        }
      }

      if (stopLoad) {
        break;
      }
    } // Fin for

    isRunning = false;
  }

  /**
   * Recupear el siguiente compuesto pendiente de validacion manual.
   *
   * @return BeanCompound o null si no hay mas compuestos.
   * @throws InterruptedException
   * @throws ChebiException
   */
  public BeanCompound getNextCompound() throws InterruptedException, ChebiException {
    BeanCompound bean = null;

    boolean next = true;
    do {
      NetCompound currentCompound = queue.poll(850, TimeUnit.MILLISECONDS);

      if (currentCompound == null)
        break;

      LOGGER.log(Level.INFO, ">>> Comp: {0}", currentCompound);

      currentCompound.setManualCuration(true);

      CompoundNormalization cn = new CompoundNormalization();
      cn.normalize(currentCompound);

      // Si hay que curar la bd, primero se mira si existe xref
      if (currentCompound.isPendingCurationDbMap()) {
        WsSearchEngine wsSearch = new WsSearchEngine(currentCompound);
        Status stWs = wsSearch.findCandidates();

        if (LOGGER.isLoggable(Level.FINE)) {
          LOGGER.log(Level.FINE, "DB-WS Curation {0} - candidates: {1}",
                  new Object[]{stWs, wsSearch.getCandidateList()});
        }

        if (stWs == Status.FOUND) {
          for (WSCompound wscomp : wsSearch.getCandidateList()) {
            currentCompound.update(wscomp);

            cn.normalize(currentCompound);
          }
        }
        else if (stWs == Status.DUDE) {
          currentCompound.setPendingCurationXrefMap(true);
        }
      }


      // Si sigue pendiente de curar
      if (currentCompound.isPendingCurationDbMap() || currentCompound.isPendingCurationXrefMap()) {
        // Flag para detener bucle
        next = false;

        // Crear el beanCompound
        bean = getBeanCompound(currentCompound);

//        // BORRAR XXX
//          final String xref = getXrefManualCurationChebiId(currentCompound.getCleanSbmlId());
//          final String dbref = getDbManualCurationRefSbmlId(currentCompound.getCleanSbmlId());
//          final String pxr = (currentCompound.isPendingCurationXrefMap()) ? "--+ " : "--- ";
//          final String pdb = (currentCompound.isPendingCurationDbMap()) ? "--+ " : "--- ";
//          System.out.println();
//          System.out.println("---------------------------------------------");
//          System.out.println("--- COMP:  " + currentCompound);
//          System.out.println(pxr + "XREF:  " + xref);
//          System.out.println(pdb + "DBREF: " + dbref);
//          System.out.println("---");
//          // Ver si existe referencia a Kegg (solo si la carga es cero)
//          if (currentCompound.validateCharge()) {
//            List<String> keggIdList = DbManager.INSTANCE.getSeedDbMgr().getKeggIdList(currentCompound.getCleanSbmlId());
//            KeggDbManager keggDb = DbManager.INSTANCE.getKeggDbMgr();
//            for (String keggId : keggIdList) {
//              System.out.println("--- KEGG:  " + keggId);
//              // Chebi
//              for (String chebiId : keggDb.getChebiIdList(keggId)) {
//                System.out.println("--- CHEBI: " + chebiId);
//              }
//            }
//          }
//          System.out.println("---------------------------------------------");
//          System.out.println();

      }
      // Se ha curado automaticamente
      else {
        curAuto++;

        // Una vez curada y guardada en la bd, pongo marca en la specie para eliminar
        currentCompound.getSpecies().addCVTerm(Util.getCurateCVTerm());

        // Limpiar XML
        cleanAndSaveCurationModel(currentCompound);

        LOGGER.log(Level.INFO, "Automatic curation");
      }

    } while (next);

    return bean;
  }

  public void saveBothCandidates(String cleanId, WSCompound wsCandidate, WSCompound dbCandidate) {
    NetCompound comp = mapComp.get(cleanId);

    // Guardar en la BD
    comp.update(wsCandidate);
    DbManager.INSTANCE.getCompoundDbMgr().updateDb(Integer.parseInt(dbCandidate.getId()), comp);

    // Update curationfile
    writeManualSelection(comp, dbCandidate);

    curMan.add(cleanId);

    // Una vez curada y guardada en la bd, pongo marca en la specie para eliminar
    comp.getSpecies().addCVTerm(Util.getCurateCVTerm());

    // Limpiar XML
    cleanAndSaveCurationModel(comp);
  }

  public void saveWsCandidate(String cleanId, WSCompound wsCandidate) {
    NetCompound comp = mapComp.get(cleanId);

    // Guardar en la BD
    comp.update(wsCandidate);
    DbManager.INSTANCE.getCompoundDbMgr().insertInDb(comp);

    // Update curationfile
    writeManualSelection(comp, null);

    curMan.add(cleanId);

    // Una vez curada y guardada en la bd, pongo marca en la specie para eliminar
    comp.getSpecies().addCVTerm(Util.getCurateCVTerm());

    // Limpiar XML
    cleanAndSaveCurationModel(comp);
  }

  public void saveDbCandidate(String cleanId, WSCompound dbCandidate) {
    NetCompound comp = mapComp.get(cleanId);

    // Guardar en la BD
    DbManager.INSTANCE.getCompoundDbMgr().updateDb(Integer.parseInt(dbCandidate.getId()), comp);

    // Update curationfile
    writeManualSelection(comp, dbCandidate);

    curMan.add(cleanId);

    // Una vez curada y guardada en la bd, pongo marca en la specie para eliminar
    comp.getSpecies().addCVTerm(Util.getCurateCVTerm());

    // Limpiar XML
    cleanAndSaveCurationModel(comp);
  }

  public void saveNoneCandidate(String cleanId) {
    NetCompound comp = mapComp.get(cleanId);

    // Guardar en la BD
    DbManager.INSTANCE.getCompoundDbMgr().insertInDb(comp);

    // Update curationfile
    writeManualSelection(comp, null);

    curMan.add(cleanId);

    // Una vez curada y guardada en la bd, pongo marca en la specie para eliminar
    comp.getSpecies().addCVTerm(Util.getCurateCVTerm());

    // Limpiar XML
    cleanAndSaveCurationModel(comp);
  }

  public void skipCompound(String cleanId) {
    NetCompound comp = mapComp.get(cleanId);

    // Skip - Volver a poner el compuesto en la cola
    queue.offer(comp);
  }

  public void exit(String cleanId) {
    NetCompound comp = mapComp.get(cleanId);

    // Skip - Volver a poner el compuesto en la cola
    queue.offerFirst(comp);

    try {
      wr.write(docManTemplate, compCurationFile);
    } catch (SBMLException e) {
      e.printStackTrace();
    } catch (XMLStreamException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public ProgressData getProgressData() {
    return progressData;
  }

  public int getTotal() {
    return queue.size();
  }

  public int getTotalManual() {
    return curMan.size();
  }

  public int getTotalAuto() {
    return curAuto;
  }

  public void close() {
    stopLoad = true;

    // Pausar el hilo hasta que se detienen otros procesos (el de carga principalmente)
    while(isRunning) {
      Thread.yield();
    }
  }

  //////////////////////////////////////////////////////////////////
  ///// METODOS PRIVADOS
  //////////////////////////////////////////////////////////////////

  private BeanCompound getBeanCompound(NetCompound comp) throws ChebiException {
    BeanCompound bean = new BeanCompound();
    bean.setOfn(comp.getOfn());
    bean.setSbmlId(comp.getSbmlId());
    bean.setSbmlCleanId(comp.getCleanSbmlId());
    bean.setSbmlName(comp.getSbmlName());
    bean.setSbmlFormula(comp.getSbmlFormula());
    bean.setFmList(comp.getFmList());
    bean.setInchi(comp.getInchi());
    bean.setInchikey(comp.getInchikey());
    bean.setSmiles(comp.getSmilesList());
    bean.setSynList(comp.getSynList());
    bean.setXrefList(comp.getXrefList());
    bean.setCharge(comp.getCharge());
    bean.setValidateCharge(comp.validateCharge());
    bean.setExchangeCompound(comp.isExchangeCompound());
    bean.setDorsal(comp.getDorsal());

    // Anadir la lista de candidatos
    List<BeanCandidate> candidateList = getCandidateList(comp);
    bean.setBeanCandidateList(candidateList);

    return bean;
  }

  private List<BeanCandidate> getCandidateList(NetCompound comp) throws ChebiException {
    List<BeanCandidate> beanCandidateList = new LinkedList<BeanCandidate>();

    // Pendiente de curar WS
    if (comp.isPendingCurationXrefMap()) {
      WsSearchEngine wsSearch = new WsSearchEngine(comp);
      Status stWs = wsSearch.findCandidates();

      if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.log(Level.FINE, "WS Curation {0} - candidates: {1}",
                new Object[]{stWs, wsSearch.getCandidateList()});
      }

      // Busqueda en WS con resultados pendientes de curacion manual
      List<WSCompound> wsList = wsSearch.getCandidateList();
      for (WSCompound wsCandidate : wsList) {
        NetCompound tmpComp = new NetCompound(comp.getSpecies());
        tmpComp.update(wsCandidate);

        // Buscar en la BD con el nuevo comp
        DbSearchEngine dbSearch = new DbSearchEngine(tmpComp);
        Status stDb = dbSearch.findChemIdCandidates();

        BeanCandidate bean = createCandidateBean(wsCandidate, stWs, dbSearch.getChemIdCandidateList(), stDb);
        beanCandidateList.add(bean);
      } // Fin for
    }

    // Pendiente de curar DB
    else if (comp.isPendingCurationDbMap()) {
      // Buscar en la BD
      DbSearchEngine dbSearch = new DbSearchEngine(comp);
      Status stDb = dbSearch.findChemIdCandidates();

      if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.log(Level.FINE, "DB Curation {0} - candidates: {1}",
                new Object[]{stDb, dbSearch.getChemIdCandidateList()});
      }

      BeanCandidate bean = createCandidateBean(null, null, dbSearch.getChemIdCandidateList(), stDb);
      beanCandidateList.add(bean);
    }

    else {
      LOGGER.log(Level.SEVERE, "No pendiente de curacion: REVISAR");
    }

    return beanCandidateList;
  }

  private BeanCandidate createCandidateBean(WSCompound wsCandidate, Status stWs, List<Integer> candidateList, Status stDb) {
    CompoundDbManager db = DbManager.INSTANCE.getCompoundDbMgr();

    // WS candidate
    BeanCandidate bean = new BeanCandidate();
    bean.setWsCandidate(wsCandidate);
    if (stWs == Status.DUDE) {
      bean.setWsCurated(false);
    }
    if (stDb == Status.DUDE) {
      bean.setDbCurated(false);
    }

    // DB candidates
    List<WSCompound> dbCandidateList = new LinkedList<WSCompound>();
    for (int chemId : candidateList) {
      assert (chemId > 0) : "Error en el chemID: " + chemId;

      WSCompound wscomp = new WSCompound(WebService.DB, String.valueOf(chemId));
      wscomp.setName(db.getOfn(chemId));
      wscomp.setCharge(db.getCharge(chemId));

      // Sinonimos
      for (String syn : db.getSynList(chemId)) {
        wscomp.addSyn(syn);
      }

      // Formula
      for (String fm : db.getSbmlFormulaList(chemId)) {
        wscomp.addFormula(Util.SOURCE_FORMULA_SBML, fm);
      }
      for (String fm : db.getNeutralFormulaList(chemId)) {
        wscomp.addFormula(Util.SOURCE_FORMULA_NEUTRAL, fm);
      }

      // xrefs
      for (String id : db.getXrefList(chemId, Util.SOURCE_SBML)) {
        wscomp.addXref(Util.SOURCE_SBML, id);
      }
      for (String id : db.getChebiIdList(chemId)) {
        wscomp.addXref(Util.SOURCE_CHEBI, id);
      }
      for (String id : db.getKeggIdList(chemId)) {
        wscomp.addXref(Util.SOURCE_KEGG, id);
      }

      for (String s : db.getStructureList(chemId)) {
        if (s.startsWith("InChI")) {
          wscomp.setInchi(s);
        }
        else if (s.startsWith("InChIKey")) {
          wscomp.setInchiKey(s);
        }
        else {
          wscomp.setSmiles(s);
        }
      }

      dbCandidateList.add(wscomp);
    } // Fin for

    bean.setDbCandidateList(dbCandidateList);

    return bean;
  }

  private void cleanAndSaveCurationModel(NetCompound comp) {
    Species sp = comp.getSpecies();

    Model model = docManTemplate.getModel();
    model.removeSpecies(sp.getId());

    try {
      wr.write(docManTemplate, compCurationFile);
    } catch (SBMLException e) {
      e.printStackTrace();
    } catch (XMLStreamException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

//  private void cleanAndSaveCurationModel(NetCompound comp) {
//    Species sp = comp.getSpecies();
//
//    Model model = sp.getModel();
//    model.removeSpecies(sp.getId());
//
//    // Actualizar fichero si existen expecies
//    if (model.getNumSpecies() > 0) {
//      SBMLDocument doc = model.getParent();
//      try {
//        wr.write(doc, compCurationFile);
//      } catch (SBMLException e) {
//        e.printStackTrace();
//      } catch (XMLStreamException e) {
//        e.printStackTrace();
//      } catch (IOException e) {
//        e.printStackTrace();
//      }
//    }
//  }


  public void writeManualSelection(NetCompound comp, WSCompound dbCandidate) {
    // Actualizar el compuesto con la info de la BD
    if (dbCandidate != null) {
      comp.addSyn(dbCandidate.getName());
      for (String syn : dbCandidate.getSynList()) {
        comp.addSyn(syn);
      }

      Map<String,Set<String>> formulaList = dbCandidate.getFormulaList();
      int charge = dbCandidate.getCharge();
      for (String source : formulaList.keySet()) {
        for (String fm : formulaList.get(source)) {
          comp.addFm(source, fm, charge);
        }
      }

      Map<String,String> xrefList = dbCandidate.getXrefList();
      for (String source : xrefList.keySet()) {
        comp.addXref(source, xrefList.get(source));
      }
      comp.addXref(Util.SOURCE_CHEBI, dbCandidate.getChebiId());
      comp.addXref(Util.SOURCE_KEGG, dbCandidate.getKeggId());
      comp.addXref(Util.SOURCE_STRUCTURE, dbCandidate.getInchi());
      comp.addXref(Util.SOURCE_STRUCTURE, dbCandidate.getInchiKey());
      comp.addXref(Util.SOURCE_STRUCTURE, dbCandidate.getSmiles());
    }

    // Actualizar structuras
    comp.addXref(Util.SOURCE_STRUCTURE, comp.getInchi());
    comp.addXref(Util.SOURCE_STRUCTURE, comp.getInchikey());
    for (String sm : comp.getSmilesList()) {
      comp.addXref(Util.SOURCE_STRUCTURE, sm);
    }

    // Actualizar formulas
    comp.addFm(Util.SOURCE_FORMULA_SBML, comp.getSbmlFormula(), comp.getCharge());
    comp.addFm(Util.SOURCE_FORMULA_NEUTRAL, comp.getNeutralFormula(), comp.getCharge());

    // Escribir fichero
    // Se usa xstream para las Collections
    BufferedWriter out = null;
    try {
      out = new BufferedWriter(new FileWriter(new File(Util.USERDIR_MANFILE), true));

      // sbmlId TAB date TAB sbmlName TAB ofn TAB carga TAB mapping TAB exchange TAB
      // xml_syn TAB xml_xref TAB xml_fm TAB EOR
      out.append(comp.getCleanSbmlId()).append(TAB);
      out.append(dateFormat.format(new Date())).append(TAB);
      out.append(comp.getSbmlName() == null ? "" : comp.getSbmlName()).append(TAB);
      out.append(comp.getOfn() == null ? "" : comp.getOfn()).append(TAB);
      out.append(String.valueOf(comp.getCharge())).append(TAB);
      out.append(String.valueOf(comp.getMapping())).append(TAB);
      out.append(String.valueOf(comp.isExchangeCompound())).append(TAB);

      // Sinonimos
      out.append(xstream.toXML(comp.getSynList())).append(TAB);

      // Xrefs (incluye xrefs, inchi, smile)
      out.append(xstream.toXML(comp.getXrefList())).append(TAB);

      // Formulas
      out.append(xstream.toXML(comp.getFmList())).append(TAB);

      out.append("EOR");
      out.newLine();

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


//  ////////////////////////////////////////////////////////
//  //////////// BORRAR XXX
//  ////////////////////////////////////////////////////////
//  Map<String,String> chebiManualCuration;
//  Map<String,String> dbManualCuration;
//  private void loadManCurationFile() {
//    chebiManualCuration = new LinkedHashMap<String,String>();
//    dbManualCuration = new LinkedHashMap<String,String>();
//
//    BufferedReader br = null;
//    try {
//      String sCurrentLine;
//
//      br = new BufferedReader(new FileReader("/home/pdsanchez/temp/cmodel/man.txt"));
//      while ((sCurrentLine = br.readLine()) != null) {
//        if (sCurrentLine.startsWith("#") || sCurrentLine.trim().isEmpty())
//          continue;
//
//        String[] reg = sCurrentLine.split(Util.TAB);
//        if (reg.length == 4) {
//          chebiManualCuration.put(reg[0], reg[3]);
//        }
//        else {
//          // db
//          if (Boolean.parseBoolean(reg[5])) {
//            dbManualCuration.put(reg[0], reg[4]);
//          }
//          // chebi
//          else {
//            chebiManualCuration.put(reg[0], reg[3]);
//          }
//        }
//      }
//    } catch (IOException e) {
//      LOGGER.log(Level.SEVERE, e.getMessage(), e);
//    }
//    finally {
//      try {
//        if (br != null)
//          br.close();
//      } catch (IOException ex) {
//        ex.printStackTrace();
//      }
//    }
//  }
//  public String getXrefManualCurationChebiId(String sbmlId) {
//    return chebiManualCuration.get(sbmlId);
//  }
//  public String getDbManualCurationRefSbmlId(String sbmlId) {
//    return dbManualCuration.get(sbmlId);
//  }
}
