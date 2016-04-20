package es.csic.cnb;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLError;
import org.sbml.jsbml.SBMLErrorLog;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.SpeciesType;
import org.sbml.jsbml.validator.SBMLValidator;

import chemaxon.license.LicenseProcessingException;
import es.csic.cnb.checker.PkaChecker;
import es.csic.cnb.data.NetCompound;
import es.csic.cnb.util.Util;
import es.csic.cnb.ws.ChebiException;

/**
 * This class parses a SBML file using the jsbml library.
 * During the parsing process a name normalization of the compounds is executed.
 *
 * @author Pablo D. Sánchez (pd.sanchez@cnb.csic.es)
 */
/**
 *
 *
 * @author Pablo D. Sánchez (pd.sanchez@cnb.csic.es)
 */
@SuppressWarnings("deprecation")
public class ModelParsing {
  private final static Logger LOGGER = Logger.getLogger(ModelParsing.class.getName());

  private static final String DATEFORMAT_FOR_FILENAME = "yyyyMMddHHmmssSSS";

  private SBMLReader reader;
  private SBMLWriter wr;
  private SBMLDocument doc;

  private SBMLDocument docManTemplate;

  private CompoundNormalization compNormalization;
  //  private ReactionNormalization reacNormalization;

  private Map<String,List<SpeciesReference>> sprMap;
//  private Map<String,List<ModifierSpeciesReference>> msprMap;

  private int modelIdx = 0;

  private PkaChecker pkaChecker;

  public ModelParsing() {
    reader = new SBMLReader();
    wr = new SBMLWriter();
    compNormalization = new CompoundNormalization();
    //    reacNormalization = new ReactionNormalization();

    try {
      pkaChecker = new PkaChecker();

      //docManTemplate = reader.readSBMLFromStream(this.getClass().getResourceAsStream("/resources/template.dat"));

    } catch (LicenseProcessingException e) {
      e.printStackTrace();
    } 
//    catch (XMLStreamException e) {
//      e.printStackTrace();
//    }
    
    
  }

  public void parseSBML(File file, Map<String,String> modelAtts) {
    modelIdx++;

    if (modelAtts == null) {
      modelAtts = Collections.emptyMap();
    }

    // TRAZA
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.log(Level.INFO, "+++ Reading model {0}: {1}", new Object[]{modelIdx, file});
      //TraceCompound.INSTANCE.setSbmlFile(file, modelIdx);
    }

    try {
      doc = reader.readSBML(file);
      doc.setSBMLDocumentAttributes(modelAtts);

      // Parses an SBML file containing the network description
      Model model = doc.getModel();
      int version = model.getVersion();
      int level = model.getLevel();
      
      // TEMPLATE
      /*
	  String templateStr = String
			.format("﻿<?xml version='1.0' encoding='UTF-8' standalone='no'?>%n"
					//+ "<!-- Created by manual version 1 on 2012-07-09 15:12 with jsbml version 0.8-rc1. -->%n"
					+ "<sbml xmlns=\"http://www.sbml.org/sbml/level2\" xmlns:html=\"http://www.w3.org/1999/xhtml\" level=\"%d\" version=\"%d\">%n"
					+ "  <model name=\"manual\">%n"
					+ "  </model>%n"
					+ "</sbml>", level, version);
			
	  docManTemplate = reader.readSBMLFromString(templateStr);
	  */
	
	docManTemplate = new SBMLDocument(level,version);
	Model docManTemplateModel = docManTemplate.createModel("manual");

      // If needed
      if (level == 2 && (version == 2 || version == 3 || version == 4)) {
        ListOf<SpeciesType> allSpTypes = model.getListOfSpeciesTypes();
        if (!allSpTypes.isEmpty()) {
          // TODO Not implemented yet
          UnsupportedOperationException e = new UnsupportedOperationException();
          LOGGER.log(Level.WARNING, "No implementado", e);
          throw e;
        }
      }

      //modelDescription = {'Source File': filename, 'ID': modelId, 'Name': modelName}


      // COMPARTIMENTOS
      //----------------------------------------------------------------

      // Recorrer compartimentos
      Set<String> cptList = new HashSet<String>();
      for (Compartment cpt : model.getListOfCompartments()) {
        // Guardar id del compartimento
        cptList.add(cpt.getId());
      }

      // Recorrer compuestos para ver que compartimentos presentan
      Set<String> cptInCompList = new HashSet<String>();
      for (Species sp : model.getListOfSpecies()) {
        // Guardar id del compartimento
        cptInCompList.add(sp.getCompartment());
      }

      for (String cptId : cptInCompList) {
        // Si el compartimento que aparece en los compuestos no existe lo creo
        if (!cptList.contains(cptId)) {
          model.addCompartment(new Compartment(cptId));

          LOGGER.log(Level.WARNING, "Se crea compartimento {0}", cptId);
        }
      }

      for (Compartment cpt : model.getListOfCompartments()) {
        if (!docManTemplateModel.containsCompartment(cpt.getId())) {
          Compartment cptClone = cpt.clone();
          cptClone.unsetMetaId();
          docManTemplateModel.addCompartment(cptClone);
        }
      }


      // REACCIONES Y COMPUESTOS
      //----------------------------------------------------------------

      // Corregir biomasa
      // Hago esto lo primero para que el hash sprMap pueda guardar los cambios introducidos por checkBiomass()
      checkBiomass();

      // Hash con las SpeciesReference asociadas al identificador del compuesto (species)
      sprMap = new HashMap<String,List<SpeciesReference>>();
      
//      // Hash con las modifierSpeciesReference
//      msprMap = new HashMap<String,List<ModifierSpeciesReference>>();

      // Recorrer reacciones
      List<Species> spList = new ArrayList<Species>();

      for (Reaction r : model.getListOfReactions()) {
        spList.clear();
        boolean isExchangeReaction = false;

        // Recuperar compuestos reactantes
        for (SpeciesReference spr : r.getListOfReactants()) {
          Species sp = model.getSpecies(spr.getSpecies()); // Compuesto
          if (sp == null) {
            LOGGER.log(Level.WARNING, "No existe id {0} para la reaccion {1}", new Object[]{spr.getSpecies(), r.getId()});
            continue;
          }
          // Identificar reaccion de intercambio
          if (Util.isExtracellular(sp.getCompartment())) {
            isExchangeReaction = true;
          }
          spList.add(sp);
          List<SpeciesReference> list = sprMap.get(spr.getSpecies());
          if (list == null) {
            list = new ArrayList<SpeciesReference>();
          }
          list.add(spr);
          sprMap.put(spr.getSpecies(), list);
        }
        // Recuperar productos
        for (SpeciesReference spr : r.getListOfProducts()) {
          Species sp = model.getSpecies(spr.getSpecies()); // Compuesto
          if (sp == null) {
            LOGGER.log(Level.WARNING, "No existe id {0} para la reaccion {1}", new Object[]{spr.getSpecies(), r.getId()});
            continue;
          }
          // Identificar reaccion de intercambio
          if (Util.isExtracellular(sp.getCompartment())) {
            isExchangeReaction = true;
          }
          spList.add(sp);
          List<SpeciesReference> list = sprMap.get(spr.getSpecies());
          if (list == null) {
            list = new ArrayList<SpeciesReference>();
          }
          list.add(spr);
          sprMap.put(spr.getSpecies(), list);
        }

        // Anadir al xml una referencia que indica que es reaccion de intercambio
        if (isExchangeReaction) {
          if (!Util.isExchangeReaction(r)) {
            // Anadir al xml una referencia que indica que es reaccion de intercambio
            r.addCVTerm(Util.getExchangeCVTerm());
          }
        }
        
//        // Modificacion 2016
//        // Recuperar especies de los modifierSpeciesReference
//        for (ModifierSpeciesReference mspr : r.getListOfModifiers()) {
//        	Species sp = model.getSpecies(mspr.getSpecies()); // Compuesto
//            if (sp == null) {
//              LOGGER.log(Level.WARNING, "No existe id {0} para la reaccion {1}", new Object[]{mspr.getSpecies(), r.getId()});
//              continue;
//            }
//            
//        }

        // Recorrer los compuestos de la reaccion
        for (Species sp : spList) {
          // Lista con los ids de los compuestos de las reacciones de intercambio
          if (isExchangeReaction) {
            if (!Util.isExchangeCompound(sp)) {
              // Anadir al xml una referencia que indica que es compuesto de intercambio
              sp.addCVTerm(Util.getExchangeCVTerm());
            }
          }
        } // Fin for compuestos de la reaccion
      } // Fin for lista de reacciones

    } catch (XMLStreamException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  /**
   * Normaliza los compuestos del SBML.
   *
   * @param changeModel - Si true se actualiza el modelo con los valores normalizados.
   * @return List<String> con las equivalencias entre los id de los compuestos del
   * modelo original y los id normalizados.
   * @throws ChebiException
   */
  public List<String> normalizeSBML(boolean changeModel) throws ChebiException {
    Model docModel = doc.getModel();

    double ph = Double.valueOf(doc.getSBMLDocumentAttributes().get("ph"));
    boolean checkPh = (ph != Util.DEFAULT_PH);
    Map<String, NetCompound> pkaChangeList = new HashMap<String, NetCompound>();
    if (checkPh) {
      LOGGER.log(Level.INFO, "### PH {0} ---> {1}", new Object[]{ph, Util.DEFAULT_PH});
    }

    Set<String> newIdList = new HashSet<String>();
    Set<NetCompound> removeIdList = new HashSet<NetCompound>();

    // Equivalencias
    List<String> equivList = new ArrayList<String>();

    // Normalizar los compuestos
    for (Species sp : docModel.getListOfSpecies()) {
      LOGGER.log(Level.INFO, ">>> Comp: {0} - {1}", new Object[]{sp.getId(),sp.getName()});

      // Crear compuesto y normalizarlo
      NetCompound comp = new NetCompound(sp);
      compNormalization.normalize(comp);

      if (comp.isPendingCurationXrefMap() || comp.isPendingCurationDbMap()) {
        Model templateModel = docManTemplate.getModel();
        if (!templateModel.containsSpecies(sp.getId())) {
          Species spClone = sp.clone();
          spClone.unsetMetaId();
          
          if (comp.isExchangeCompound()) {
            if (!Util.isExchangeCompound(sp)) {
              // Anadir al xml una referencia que indica que es compuesto de intercambio
              sp.addCVTerm(Util.getExchangeCVTerm());
            }
            //templateModel.addSpecies(spClone);
          }
          //else {
          //  templateModel.addSpecies(sp.clone());
          //}
          templateModel.addSpecies(sp.clone());
        }
      }

      if (changeModel && comp.getChemId() > 0) {
        String newId = comp.getChemIdStr();
        if (newId != null) {
          if (newIdList.contains(newId)) {
            removeIdList.add(comp);
          }
          else {
            StringBuilder equiv = new StringBuilder();
            equiv.append(newId).append(" = ").append(sp.getId()).append(" - ").append(sp.getName());
            equivList.add(equiv.toString());

            newIdList.add(newId);
            sp.setId(newId);
            LOGGER.log(Level.INFO, "{0} = {1}", new Object[]{comp.getSbmlId(), newId});
          }

          List<SpeciesReference> sprList = sprMap.get(comp.getSbmlId());
          if (sprList != null) {
            for (SpeciesReference spr : sprList) {
              spr.setSpecies(comp.getChemIdStr());
            }
          }
        }
      }

      // Evaluar de nuevo si es necesario por ph
      if (comp.getStructure() != null && checkPh) {
        NetCompound newComp = pkaChecker.check(comp, ph);
        if (newComp != null) {
          pkaChangeList.put(sp.getName(), newComp);
        }
      }

    } // End for comp

    if (changeModel) {
      for (NetCompound comp : removeIdList) {
        docModel.removeSpecies(comp.getSpecies());
        LOGGER.log(Level.INFO, "Elimino {0}", new Object[]{comp.getSbmlId()});
      }
    }


    //    // TODO
    //    if (false) {
    //      // Normalizar las reacciones
    //      for (Reaction r : doc.getModel().getListOfReactions()) {
    //        LOGGER.log(Level.INFO, ">>> Reac: {0}", r.getId());
    //
    //        NetReaction reac = new NetReaction(r);
    //        reac = reacNormalization.normalize(reac);
    //      }
    //    }


    //    // TRAZA
    //    if (LOGGER.isLoggable(Level.INFO)) {
    //      TraceCompound.INSTANCE.write();
    //    }




    // CORREGIR COMP Y REAC INTERCAMBIO
    checkExchangeCompounds();
    checkExchangeReactions();

    // CORREGIR POR PH
    if (checkPh) {
      fixPhCompounds(pkaChangeList);
    }


    return equivList;
  }

  /**
   * Escribe xml (sbml) con los casos a curar manualmente.
   *
   * @return lista con los compuestos a curar manualmente.
   */
  public List<Species> writeManualCurationSBML() {
    final String filename = new SimpleDateFormat(DATEFORMAT_FOR_FILENAME).format(new Date());
    try {
      SBMLWriter.write(docManTemplate, new File(Util.USERDIR_CURATIONFILES, filename + ".xml"), "CombiModel", "1");

    } catch (SBMLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } catch (XMLStreamException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }

    return docManTemplate.getModel().getListOfSpecies();
  }

  /**
   * Metodo que crea un SBML normalizado a partir del modelo cargado.
   * Se crea un nuevo fichero xml con los cambios realizados.
   *
   * @param file - fichero de salida
   */
  public void writeNormalSBML(File file) {
    // Eliminar ref
    for (Species sp : doc.getModel().getListOfSpecies()) {
      Util.removeExchangeResource(sp);
    }

    try {
      wr.write(doc, file);

    } catch (SBMLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } catch (XMLStreamException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  public SBMLDocument getSBMLDocument() {
    return doc;
  }

  /**
   * TODO
   * @param filepath
   * @return
   */
  public String validateSBML(String filepath) {
    /*
      u --> Disable checking the consistency of measurement units associated with quantities (SBML L2V3 rules 105nn)
      i --> Disable checking the correctness and consistency of identifiers used for model entities (SBML L2V3 rules 103nn)
      m --> Disable checking the syntax of MathML mathematical expressions (SBML L2V3 rules 102nn)
      s --> Disable checking the validity of SBO identifiers (if any) used in the model (SBML L2V3 rules 107nn)
      o --> Disable static analysis of whether the model is overdetermined
      p --> Disable additional checks for recommended good modeling practices
      g --> Disable all other general SBML consistency checks (SBML L2v3 rules 2nnnn)
     */

    HashMap<String,String> parameters = new HashMap<String,String>();
    parameters.put("offcheck", "u,m,s");

    SBMLErrorLog errLog = SBMLValidator.checkConsistency(filepath, parameters);
    if (errLog!=null) {
      System.out.println("NUM: "+errLog.getNumErrors());
      for (SBMLError e : errLog.getValidationErrors()) {
        System.out.println("- " + e.getCategory() +" : "+e.getSeverity() + ": "+e.getMessage());
      }
    }
    return "";
  }

  private void checkBiomass() throws XMLStreamException {
    final Matcher matcher = Pattern.compile("biomass", Pattern.CASE_INSENSITIVE).matcher("");

    boolean existCpdBiomassC = false;
    boolean existCpdBiomassE = false;
    boolean existCpdBiomassB = false;

    Species cpdBiomassC = null;
    Species cpdBiomassE = null;
    Species cpdBiomassB = null;

    // Identificar los compuestos de biomasa
    Model docModel = doc.getModel();
    for (Species sp : docModel.getListOfSpecies()) {
      // Llenar vectores de comp de intercambio a corregir
      if (matcher.reset(sp.getName()).find() || matcher.reset(sp.getId()).find()) {
        if (sp.getBoundaryCondition()) {
          existCpdBiomassB = true;
          cpdBiomassB = sp;
        }
        else if (Util.isCytosolic(sp.getCompartment())) {
          existCpdBiomassC = true;
          cpdBiomassC = sp;
        }
        else if (Util.isExtracellular(sp.getCompartment())) {
          existCpdBiomassE = true;
          cpdBiomassE = sp;
        }
      }
    }

    boolean existRccBiomassC = false; // c - c
    boolean existRccBiomassE = false; // c - e
    boolean existRccBiomassB = false; // c - b o e - b

    List<Reaction> rccBiomassCList = new ArrayList<Reaction>();
    Reaction rccBiomassC = null;
    Reaction rccBiomassE = null;
    Reaction rccBiomassB = null;

    // Identificar las reac de biomasa
    for (Reaction r : docModel.getListOfReactions()) {
      // Llenar vectores de comp de intercambio a corregir
      if (matcher.reset(r.getName()).find() || matcher.reset(r.getId()).find()) {
        // Obtener reactantes y productos
        ListOf<SpeciesReference> rsrList = r.getListOfReactants();
        ListOf<SpeciesReference> psrList = r.getListOfProducts();
	
	KineticLaw klr = r.getKineticLaw();
        if (rsrList.size() == 1 && psrList.size() == 1) {
          Species spP = psrList.getFirst().getSpeciesInstance();

          // Reac e -> b o c -> b
          if (spP.getBoundaryCondition()) {
            rccBiomassB = r;
            existRccBiomassB = true;
		
		if(klr!=null) {
			klr.getLocalParameter("LOWER_BOUND").setValue(0.0);
			klr.getLocalParameter("UPPER_BOUND").setValue(1000);
		}
          }
          // Reac c -> e
          else if (Util.isExtracellular(spP.getCompartment())) {
            rccBiomassE = r;
            existRccBiomassE = true;

		if(klr!=null) {
			klr.getLocalParameter("LOWER_BOUND").setValue(0.0);
			klr.getLocalParameter("UPPER_BOUND").setValue(1000);
		}
          }
          // Reac citoplasmatica
          else {
            rccBiomassC = r;
            rccBiomassCList.add(r);//
            existRccBiomassC = true;
          }
        }
        // Reac citoplasmatica
        else {
          if (klr!=null && klr.getListOfLocalParameters().get("OBJECTIVE_COEFFICIENT").getValue() == 1) {
            rccBiomassC = r;
          }
          rccBiomassCList.add(r);

          //rccBiomassC = r;
          existRccBiomassC = true;
        }
      }
    }

//    // TRAZA
//    System.out.println("CP: C-"+existCpdBiomassC+ " E-"+existCpdBiomassE+" B-"+existCpdBiomassB);
//    System.out.println("RX: C-"+existRccBiomassC+ " E-"+existRccBiomassE+" B-"+existRccBiomassB);

    // DE MOMENTO SOLO SE CORRIGEN LOS CASOS RAROS QUE APARECEN EN LOS MODELOS REVISADOS
    // OTROS CASOS HABRIA QUE ANADIRLOS A CONTINUACION

    // Aparece formula en notas
    boolean useNotes = false;
    String notes = null;

    // Crear plantilla que presente esa formula vacia
    Species spTp = docModel.getSpecies(0);
    Matcher mtFormula = Pattern.compile("(FORMULA:\\s?)([A-Za-z0-9]+)").matcher("");
    mtFormula.reset(spTp.getNotesString());
    if (mtFormula.find()) {
      notes = mtFormula.group(1);
      useNotes = true;
    }

    // Compartimentos
    String cytosol = "c";
    String extracell = "e";
    for (Compartment compartment : docModel.getListOfCompartments()) {
      if (Util.isCytosolic(compartment.getId())) {
        cytosol = compartment.getId();
      }
      else if (Util.isExtracellular(compartment.getId())) {
        extracell = compartment.getId();
      }
    }

    // Existe reaccion de biomasa en c, pero no existe comp biomasa en c
    if (!existCpdBiomassC && existRccBiomassC) {
      // Crear compuesto de biomasa c
      cpdBiomassC = docModel.createSpecies("biomass_c");
      cpdBiomassC.setName(Util.BIOMASS);
      cpdBiomassC.setCompartment(cytosol);
      cpdBiomassC.setBoundaryCondition(false);
      if (useNotes) {
        cpdBiomassC.setNotes(notes);
      }

      // Anadir comp creado a la reac de biomasa c
      for (Reaction r : rccBiomassCList) {
        SpeciesReference sr = r.createProduct(cpdBiomassC);
        sr.setStoichiometry(1);
      }
      //SpeciesReference sr = rccBiomassC.createProduct(cpdBiomassC);
      //sr.setStoichiometry(1);

      // Crear compuesto de biomasa e y reac de biomasa c - e (si no existen)
      if (!existCpdBiomassE) {
        cpdBiomassE = docModel.createSpecies("biomass_e");
        cpdBiomassE.setName(Util.BIOMASS);
        cpdBiomassE.setCompartment(extracell);
        cpdBiomassE.setBoundaryCondition(false);
        if (useNotes) {
          cpdBiomassE.setNotes(notes);
        }

        if (!existRccBiomassE && rccBiomassC != null ) {
          rccBiomassE = rccBiomassC.clone();
          
          // Modificacion 01/2016 (el metaid de idb coincide -clon- y salta excepcion => unset metaid del clon)
          rccBiomassE.unsetMetaId();
          
          rccBiomassE.setId("R_EX_Biomass_auto");
          rccBiomassE.setName("EX_Biomass_auto");

          rccBiomassE.unsetNotes();

          rccBiomassE.unsetListOfReactants();
          rccBiomassE.setListOfReactants(new ListOf<SpeciesReference>());
          SpeciesReference srr = rccBiomassE.createReactant(cpdBiomassC);
          srr.setStoichiometry(1);

          rccBiomassE.unsetListOfProducts();
          rccBiomassE.setListOfProducts(new ListOf<SpeciesReference>());
          SpeciesReference srp = rccBiomassE.createProduct(cpdBiomassE);
          srp.setStoichiometry(1);

		KineticLaw rcckl = rccBiomassE.getKineticLaw();
		if(rcckl != null) {
			rcckl.getListOfLocalParameters().get("LOWER_BOUND").setValue(0.0);
			rcckl.getListOfLocalParameters().get("UPPER_BOUND").setValue(1000);
			rcckl.getListOfLocalParameters().get("FLUX_VALUE").setValue(0.0);
			// Mantener el OBJECTIVE_COEFFICIENT de la reaccion original en la nueva reac (deberia ser 1)
			//rcckl.getListOfLocalParameters().get("OBJECTIVE_COEFFICIENT").setValue(1); // A veces es cero
		}

          docModel.addReaction(rccBiomassE);

          // Cambiar a cero el OBJECTIVE_COEFFICIENT de la reaccion original
		if(rcckl != null) {
			rcckl.getListOfLocalParameters().get("OBJECTIVE_COEFFICIENT").setValue(0.0);
		}
        }
      }

      // Crear compuesto de biomasa b y reac de biomasa e - b (si no existen)
      if (!existCpdBiomassB) {
        cpdBiomassB = docModel.createSpecies("biomass_b");
        cpdBiomassB.setName(Util.BIOMASS);
        cpdBiomassB.setCompartment(extracell);
        cpdBiomassB.setBoundaryCondition(true);
        if (useNotes) {
          cpdBiomassB.setNotes(notes);
        }

        if (!existRccBiomassB && rccBiomassC != null) {
          rccBiomassB = rccBiomassC.clone();
          
          // Modificacion 01/2016 (el metaid de idb coincide -clon- y salta excepcion => unset metaid del clon)
          rccBiomassB.unsetMetaId();
          
          rccBiomassB.setId("R_EX_TotBiomass");
          rccBiomassB.setName("EX_TotBiomass");

          rccBiomassB.unsetNotes();

          rccBiomassB.unsetListOfReactants();
          rccBiomassB.setListOfReactants(new ListOf<SpeciesReference>());
          SpeciesReference srr = rccBiomassB.createReactant(cpdBiomassE);
          srr.setStoichiometry(1);

          rccBiomassB.unsetListOfProducts();
          rccBiomassB.setListOfProducts(new ListOf<SpeciesReference>());
          SpeciesReference srp = rccBiomassB.createProduct(cpdBiomassB);
          srp.setStoichiometry(1);

		KineticLaw rcckl = rccBiomassB.getKineticLaw();
		if(rcckl!=null) {
			rcckl.getListOfLocalParameters().get("LOWER_BOUND").setValue(0.0);
			rcckl.getListOfLocalParameters().get("UPPER_BOUND").setValue(1000);
			rcckl.getListOfLocalParameters().get("OBJECTIVE_COEFFICIENT").setValue(0.0);
			rcckl.getListOfLocalParameters().get("FLUX_VALUE").setValue(0.0);
		}

          docModel.addReaction(rccBiomassB);
        }
      }
    }

    // Existe el comp de biomasa b, pero no existe reac de biomasa hacia b
    if (!existRccBiomassB && existCpdBiomassB && existRccBiomassC && existCpdBiomassC) {
      // Crear comp e
      if (!existCpdBiomassE) {
        cpdBiomassE = docModel.createSpecies("biomass_e");
        cpdBiomassE.setName(Util.BIOMASS);
        cpdBiomassE.setCompartment(extracell);
        cpdBiomassE.setBoundaryCondition(false);
        if (useNotes) {
          cpdBiomassE.setNotes(notes);
        }
      }
      // Crear reac c - e
      if (!existRccBiomassE && rccBiomassC != null) {
        rccBiomassE = rccBiomassC.clone();
        
        // Modificacion 01/2016 (el metaid de idb coincide -clon- y salta excepcion => unset metaid del clon)
        rccBiomassE.unsetMetaId();
        
        rccBiomassE.setId("R_EX_Biomass_auto");
        rccBiomassE.setName("EX_Biomass_auto");

        rccBiomassE.unsetNotes();

        rccBiomassE.unsetListOfReactants();
        rccBiomassE.setListOfReactants(new ListOf<SpeciesReference>());
        SpeciesReference srr = rccBiomassE.createReactant(cpdBiomassC);
        srr.setStoichiometry(1);

        rccBiomassE.unsetListOfProducts();
        rccBiomassE.setListOfProducts(new ListOf<SpeciesReference>());
        SpeciesReference srp = rccBiomassE.createProduct(cpdBiomassE);
        srp.setStoichiometry(1);

	KineticLaw rcckl = rccBiomassE.getKineticLaw();
	if(rcckl != null) {
		rcckl.getListOfLocalParameters().get("LOWER_BOUND").setValue(0.0);
		rcckl.getListOfLocalParameters().get("UPPER_BOUND").setValue(1000);
		rcckl.getListOfLocalParameters().get("FLUX_VALUE").setValue(0.0);
		// Mantener el OBJECTIVE_COEFFICIENT de la reaccion original en la nueva reac (deberia ser 1)
		//rcckl.getListOfLocalParameters().get("OBJECTIVE_COEFFICIENT").setValue(1); // A veces es cero
	}

        docModel.addReaction(rccBiomassE);

        // Cambiar a cero el OBJECTIVE_COEFFICIENT de la reaccion original
        if(rcckl != null) {
		rcckl.getListOfLocalParameters().get("OBJECTIVE_COEFFICIENT").setValue(0.0);
	}
      }

      // Crear reac de biomasa e - b
      rccBiomassB = rccBiomassE.clone();
      
      // Modificacion 01/2016 (el metaid de idb coincide -clon- y salta excepcion => unset metaid del clon)
      rccBiomassB.unsetMetaId();
      
      rccBiomassB.setId("R_EX_TotBiomass");
      rccBiomassB.setName("EX_TotBiomass");

      rccBiomassB.unsetNotes();

      rccBiomassB.unsetListOfReactants();
      rccBiomassB.setListOfReactants(new ListOf<SpeciesReference>());
      SpeciesReference srr = rccBiomassB.createReactant(cpdBiomassE);
      srr.setStoichiometry(1);

      rccBiomassB.unsetListOfProducts();
      rccBiomassB.setListOfProducts(new ListOf<SpeciesReference>());
      SpeciesReference srp = rccBiomassB.createProduct(cpdBiomassB);
      srp.setStoichiometry(1);

	KineticLaw rcckl = rccBiomassB.getKineticLaw();
	if(rcckl != null) {
		rcckl.getListOfLocalParameters().get("LOWER_BOUND").setValue(0.0);
		rcckl.getListOfLocalParameters().get("UPPER_BOUND").setValue(1000);
		rcckl.getListOfLocalParameters().get("OBJECTIVE_COEFFICIENT").setValue(0.0);
		rcckl.getListOfLocalParameters().get("FLUX_VALUE").setValue(0.0);
	}

      docModel.addReaction(rccBiomassB);
    }


    // Existe el comp de biomasa b, pero no existe reac de biomasa hacia e
    if (!existRccBiomassE && existCpdBiomassB && existRccBiomassB && existRccBiomassC && existCpdBiomassC) {
      // Crear comp e
      if (!existCpdBiomassE) {
        cpdBiomassE = docModel.createSpecies("biomass_e");
        cpdBiomassE.setName(Util.BIOMASS);
        cpdBiomassE.setCompartment(extracell);
        cpdBiomassE.setBoundaryCondition(false);
        if (useNotes) {
          cpdBiomassE.setNotes(notes);
        }
      }

      // Crear la reac c - e
      if(rccBiomassC != null) {
	      rccBiomassE = rccBiomassC.clone();
	      
	      // Modificacion 01/2016 (el metaid de idb coincide -clon- y salta excepcion => unset metaid del clon)
	      rccBiomassE.unsetMetaId();
	      
	      rccBiomassE.setId("R_EX_Biomass_auto");
	      rccBiomassE.setName("EX_Biomass_auto");

	      rccBiomassE.unsetNotes();

	      rccBiomassE.unsetListOfReactants();
	      rccBiomassE.setListOfReactants(new ListOf<SpeciesReference>());
	      SpeciesReference srr = rccBiomassE.createReactant(cpdBiomassC);
	      srr.setStoichiometry(1);

	      rccBiomassE.unsetListOfProducts();
	      rccBiomassE.setListOfProducts(new ListOf<SpeciesReference>());
	      SpeciesReference srp = rccBiomassE.createProduct(cpdBiomassE);
	      srp.setStoichiometry(1);

		KineticLaw rcckl = rccBiomassE.getKineticLaw();
		if(rcckl != null) {
			rcckl.getListOfLocalParameters().get("LOWER_BOUND").setValue(0.0);
			rcckl.getListOfLocalParameters().get("UPPER_BOUND").setValue(1000);
			rcckl.getListOfLocalParameters().get("FLUX_VALUE").setValue(0.0);
			// Mantener el OBJECTIVE_COEFFICIENT de la reaccion original en la nueva reac (deberia ser 1)
			//rcckl.getListOfLocalParameters().get("OBJECTIVE_COEFFICIENT").setValue(1); // A veces es cero
		}

	      docModel.addReaction(rccBiomassE);

		if(rcckl != null) {
			// Cambiar a cero el OBJECTIVE_COEFFICIENT de la reaccion original
			rcckl.getListOfLocalParameters().get("OBJECTIVE_COEFFICIENT").setValue(0.0);
		}
	}

      // Modificar la reac c - b
      rccBiomassB.removeReactant(cpdBiomassC.getId()); // eliminar c
      rccBiomassB.createReactant(cpdBiomassE); // anadir e
    }
  }

  private void checkExchangeCompounds() {
    // Vectores para corregir los compuestos de intercambio
    List<String> cpEList = new ArrayList<String>();
    List<String> cpBList = new ArrayList<String>();

    Model docModel = doc.getModel();
    for (Species sp : docModel.getListOfSpecies()) {
      // Llenar vectores de comp de intercambio a corregir
      if (sp.getId().endsWith("_e")) {
        cpEList.add(sp.getId());
      }
      else if (sp.getId().endsWith("_b")) {
        cpBList.add(sp.getId());
      }
    }

    // Compartimentos
    String extracell = "e";
    String cytosolic = "c";
    for (Compartment compartment : docModel.getListOfCompartments()) {
      if (Util.isExtracellular(compartment.getId())) {
        extracell = compartment.getId();
      }
      else if (Util.isCytosolic(compartment.getId())) {
    	cytosolic = compartment.getId();
      }
    }

    // Recorrer comp _e para ver si existen los correspondientes comp _b
    for (String id : cpEList) {
      // Cambiar _e por _b en el identificador de comp
      String idb = id.replaceFirst("_e$", "_b");

      // Si no existe se crea
      if (!docModel.containsSpecies(idb)) {
    	LOGGER.log(Level.FINEST, "No existe compuesto _b " + idb); // TRAZA

        // Comp original
        Species spc = docModel.getSpecies(id);

        // OJO: a veces usan compartimento citosol en _e o en _b
        if (!Util.isExtracellular(spc.getCompartment())) {
          spc.setCompartment(extracell);
        }

        // Nuevo comp
        Species spb = spc.clone();
        spb.setId(idb);
        spb.setBoundaryCondition(true);
        
        // Modificacion 01/2016 (el metaid de idb coincide -clon- y salta excepcion => unset metaid del clon)
        spb.unsetMetaId();
        try {
        	//LOGGER.log(Level.FINEST, "SP-->"+spb+" ["+idb+"]: "+spb.getMetaId()); // TRAZA
        	docModel.addSpecies(spb);
        }
        catch(IllegalArgumentException e) {
        	LOGGER.log(Level.SEVERE, e.getMessage());
        }
      }
    }

    // Recorrer comp _b para ver si existen los correspondientes comp _e
    // y comp_c (modificacion 2016)
    for (String id : cpBList) {
      // Cambiar _b por _e en el identificador de comp
      String ide = id.replaceFirst("_b$", "_e");

      // Si no existe se crea
      if (!docModel.containsSpecies(ide)) {
    	LOGGER.log(Level.FINEST, "No existe compuesto _e " + ide); // TRAZA

        // Comp original
        Species spc = docModel.getSpecies(id);

        // OJO: a veces usan compartimento citosol en _e o en _b
        if (!Util.isExtracellular(spc.getCompartment())) {
          spc.setCompartment(extracell);
        }

        // Nuevo comp
        Species spe = spc.clone();
        spe.setId(ide);
        spe.setBoundaryCondition(false);

        // Modificacion 01/2016 (el metaid de idb coincide -clon- y salta excepcion => unset metaid del clon)
        spe.unsetMetaId();
        docModel.addSpecies(spe);
      }
      
      // Modificacion 2016
      // Cambiar _b por _c en el identificador de comp
      String idc = id.replaceFirst("_b$", "_c");

      // Si no existe se crea
      if (!docModel.containsSpecies(idc)) {
    	LOGGER.log(Level.FINEST, "No existe compuesto _c " + idc); // TRAZA

        // Comp original
        Species spc = docModel.getSpecies(id);

        if (Util.isExtracellular(spc.getCompartment())) {
          spc.setCompartment(cytosolic);
        }

        // Nuevo comp
        Species spe = spc.clone();
        spe.setId(idc);
        spe.setBoundaryCondition(false);

        // Modificacion 01/2016 (el metaid de idb coincide -clon- y salta excepcion => unset metaid del clon)
        spe.unsetMetaId();
        docModel.addSpecies(spe);
      }
    }
  }

  private void checkExchangeReactions() {
    // Lista de reacciones a anadir
    List<Reaction> newReacList = new ArrayList<Reaction>();

    Model docModel = doc.getModel();
    for (Reaction r : docModel.getListOfReactions()) {
      //String id = r.getId();
      //if (id.contains("EX_")) {
        boolean ok = false; // Correcto
        boolean caso1 = false; // Reactante = _c
        boolean caso2 = false; // No existe producto
        // Resto de casos no contemplados

        //System.out.println(id);

        // Obtener reactantes y productos
        ListOf<SpeciesReference> rsrList = r.getListOfReactants();
        ListOf<SpeciesReference> psrList = r.getListOfProducts();

        if (rsrList.size() == 1 && psrList.size() == 1) {
          String cpIdR = rsrList.getFirst().getSpecies();
          String cpIdP = psrList.getFirst().getSpecies();

          //System.out.println("  -"+cpIdR);
          //System.out.println("  -"+cpIdP);

          if (cpIdR.endsWith("_c") && cpIdP.endsWith("_e")) {
            ok = true;
          }
          else if (cpIdR.endsWith("_e") && cpIdP.endsWith("_b")) {
            ok = true;
          }
          else if (cpIdR.endsWith("_c") && cpIdP.endsWith("_b")) {
            caso1 = true;
          }
        }
        else if (psrList.size() == 0) {
          caso2 = true;
        }

        // Evaluar casos
        if (!ok) {
          // Reactante = _c
          if (caso1) {
            //System.out.println("CREAR NUEVA REACCION: "+r);

            // Crear nueva reaccion
            Reaction rclon = r.clone();
            
            // Modificacion 01/2016 (el metaid de idb coincide -clon- y salta excepcion => unset metaid del clon)
            rclon.unsetMetaId();
            
            // Cambiar id
            String newId = rclon.getId() + "_copy";
            String newName = rclon.getName() + "_copy";
            rclon.setId(newId);
            rclon.setName(newName);
            // Anadir al modelo
            // Lo guardo para anadirlo posteriormente ya que si se anade aqui modifico
            // el numero de elementos de la iteracion y da error
            newReacList.add(rclon);

            // 1 Dejar la reaccion original como _c -> _e
            // Cambiar producto de _b a _e
            String cpSp = r.getProduct(0).getSpecies().replaceFirst("_b$", "_e");
            r.getProduct(0).setSpecies(cpSp);

            // 2 Dejar la reaccion clonada como _e -> _b
            // Cambiar reactante de _c a _e
            String crSp = rclon.getReactant(0).getSpecies().replaceFirst("_c$", "_e");
            rclon.getReactant(0).setSpecies(crSp);
          }

          // No existe producto
          else if (caso2) {
            //System.out.println("CREAR PRODUCTO: "+r);

            // Crear producto
            SpeciesReference psr = rsrList.getFirst().clone();
            
            // Modificacion 01/2016 (el metaid de idb coincide -clon- y salta excepcion => unset metaid del clon)
            psr.unsetMetaId();
            
            // Cambiar specie
            String cpIdP = psr.getSpecies().replaceFirst("_e$", "_b");
            psr.setSpecies(cpIdP);
            // Anadir a la reaccion
            psrList.add(psr);
          }

          // Caso no contemplado (no se corrige)
          //else {
          //  System.out.println("!!! Caso no contemplado: "+r);
          //}
        }
      //}
    } // end reactions

    // Anadir las nuevas reacciones
    for (Reaction r : newReacList) {
      docModel.addReaction(r);
    }


    // Casos en los que no existen reacciones, pero si los compuestos _e
    // Lista de compuestos
    List<String> cpEList = new ArrayList<String>();
    for (Species sp : docModel.getListOfSpecies()) {
      // Llenar vectores de comp de intercambio a corregir
      if (sp.getId().endsWith("_e")) {
        cpEList.add(sp.getId());
      }
    }

    // Contrastar los compuestos con _e con la lista de reacciones que contienen comp _e
    for (String cpId : cpEList) {
      boolean found = false;

      // Recorrer reacciones
      rx:
        for (Reaction r : docModel.getListOfReactions()) {
          // Obtener reactantes y productos
          ListOf<SpeciesReference> rsrList = r.getListOfReactants();
          for (SpeciesReference sr : rsrList) {
            if (sr.getSpecies().equals(cpId)) {
              found = true;
              continue rx;
            }
          }
          ListOf<SpeciesReference> psrList = r.getListOfProducts();
          for (SpeciesReference sr : psrList) {
            if (sr.getSpecies().equals(cpId)) {
              found = true;
              continue rx;
            }
          }
        }

      if (!found) {
        //System.out.println("NO EXISTE REACCION _e PARA "+cpId);

        Reaction rtemplate = null;
        // Tomar una reaccion modelo
        for (Reaction r : docModel.getListOfReactions()) {
          String id = r.getId();
          if (id.contains("EX_")) {
            rtemplate = r;
            break;
          }
        }

        // Crear reaccion c->e
        Reaction r1 = Util.cloneReaction(rtemplate);
        
        r1.setId("rxn_new_" + cpId);
        r1.setName("rxn_new_" + cpId);
        
        String crSp1 = cpId.replaceFirst("_\\w$", "_c");
        r1.getReactant(0).setSpecies(crSp1);
        
        String cpSp1 = cpId.replaceFirst("_\\w$", "_e");
        r1.getProduct(0).setSpecies(cpSp1);
        docModel.addReaction(r1);

        // Crear reaccion e->b
        Reaction r2 = Util.cloneReaction(rtemplate);
        //Reaction r2 = rtemplate.clone();
        
        r2.setId("EX_new_" + cpId.replaceFirst("_e$", "_b"));
        r2.setName("EX_new_" + cpId.replaceFirst("_e$", "_b"));
        String crSp2 = cpId.replaceFirst("_\\w$", "_e");
        r2.getReactant(0).setSpecies(crSp2);
        String cpSp2 = cpId.replaceFirst("_\\w$", "_b");
        r2.getProduct(0).setSpecies(cpSp2);
        docModel.addReaction(r2);
      }
    }
  }

  /**
   * Modifica los compuestos y las reacciones especificados en el hash
   *
   * @param pkaChangeList - hash String-WSCompound
   * @throws ChebiException
   */
  public void fixPhCompounds(Map<String,NetCompound> pkaChangeList) throws ChebiException {
    Map<String, String> tochangeList = new HashMap<String, String>();

    Model docModel = doc.getModel();
    for (Species sp : docModel.getListOfSpecies()) {
      if (pkaChangeList.containsKey(sp.getName())) {
        NetCompound newComp = pkaChangeList.get(sp.getName());
        compNormalization.normalize(newComp); // Guarda en la BD

        String oldId = sp.getId();
        String newId = newComp.getChemIdStr();
        String compartmentAbb = Util.getCompartmentAbb(sp);

        newId = newId.replaceFirst("_[a-z]$", "_" + compartmentAbb);

        sp.setId(newId);
        sp.setName(newComp.getSbmlName() + "_" + newComp.getSbmlFormula());
        sp.setCharge(newComp.getCharge());

        tochangeList.put(oldId, newId);

        LOGGER.log(Level.INFO, "PH compound change {0} -> {1}", new Object[]{oldId, newId});
      }
    }

    for (Reaction r : docModel.getListOfReactions()) {
      if (Util.isExchangeReaction(r)) {
        Util.removeExchangeResource(r);
      }

      // Obtener reactantes y productos
      ListOf<SpeciesReference> rsrList = r.getListOfReactants();
      for (SpeciesReference sr : rsrList) {
        if (tochangeList.containsKey(sr.getSpecies())) {
          sr.setSpecies(tochangeList.get(sr.getSpecies()));
        }
      }

      ListOf<SpeciesReference> psrList = r.getListOfProducts();
      for (SpeciesReference sr : psrList) {
        if (tochangeList.containsKey(sr.getSpecies())) {
          sr.setSpecies(tochangeList.get(sr.getSpecies()));
        }
      }
    }

    String ph = doc.getSBMLDocumentAttributes().get("ph") + " --> " + Util.DEFAULT_PH;
    doc.getSBMLDocumentAttributes().put("ph", ph);
  }
}
