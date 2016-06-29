package es.csic.cnb.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;
import org.sbml.jsbml.ext.fbc.FluxObjective;
import org.sbml.jsbml.ext.fbc.ListOfObjectives;
import org.sbml.jsbml.ext.fbc.Objective;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.util.StringTools;
import org.sbml.jsbml.xml.XMLNode;

import uk.ac.ebi.chebi.webapps.chebiWS.client.ChebiWebServiceClient;
import es.csic.cnb.ws.ChebiException;

import es.csic.cnb.util.XmlnsPrefixer;
import java.io.File;
import java.io.IOException;
import javax.xml.transform.TransformerException;

public class Util {
  private static final Logger LOGGER = Logger.getLogger(Util.class.getName());

  public static final String NEWLINE = System.getProperty("line.separator");
  public static final String TAB = "\t";

  public static final String USERDIR = PropertiesMgr.INSTANCE.getCmodelPath();
  public static final String USERDIR_CURATIONFILES = USERDIR + "/db/curation/files/";
  public static final String USERDIR_MANFILE = USERDIR + "/db/curation/man.txt";
  public static final String USERDIR_MODELS  = USERDIR + "/models/";

  public static final String COMPOUNDS_CURATIONFILE = "ccompounds.xml";

  public static final int RMI_PORT = PropertiesMgr.INSTANCE.getRMIPort();
  public static final String RMI_HOST = PropertiesMgr.INSTANCE.getRMIHost();

  public static final int DB_PORT = PropertiesMgr.INSTANCE.getDbPort();
  public static final String DB_HOST = PropertiesMgr.INSTANCE.getDbHost();

  //public static final String DB = "jdbc:h2:file:/";
  //public static final String DB = "jdbc:h2:tcp://localhost/";
  public static final String DB = "jdbc:h2:tcp://" + DB_HOST + ":" + DB_PORT + "/";

	// This local parameters, in the case of FBC, are obtained with specialized methods
	public static final String LOCAL__LOWER_BOUND_PARAM = "LOWER_BOUND";
	public static final String LOCAL__UPPER_BOUND_PARAM = "UPPER_BOUND";
	public static final String LOCAL__FLUX_VALUE_PARAM = "FLUX_VALUE";
	public static final String LOCAL__OBJECTIVE_COEFFICIENT_PARAM = "OBJECTIVE_COEFFICIENT";
	
	public static final String DEFAULT_LOWER_BOUND_PARAM_ID = "cmodel_lb";
	public static final String IMPOSSIBLE_LOWER_BOUND_PARAM_ID = "cmodel_imp_lb";
	public static final String DEFAULT_UPPER_BOUND_PARAM_ID = "cmodel_ub";
	
	public static final String DEFAULT_LOWER_BOUND_PARAM_NAME = "Default lower flux bound (set by cmodel)";
	public static final String IMPOSSIBLE_LOWER_BOUND_PARAM_NAME = "Impossible lower flux bound (set by cmodel)";
	public static final String DEFAULT_UPPER_BOUND_PARAM_NAME = "Default upper flux bound (set by cmodel)";

	public static final double DEFAULT_LOWER_BOUND_VALUE = 0.0;
	public static final double DEFAULT_UPPER_BOUND_VALUE = 1000;
	public static final double IMPOSSIBLE_LOWER_BOUND_VALUE = -DEFAULT_UPPER_BOUND_VALUE;
	public static final double OBJECTIVE_COEFFICIENT_VALUE = 1.0;
	public static final double DISABLED_OBJECTIVE_COEFFICIENT_VALUE = 0.0;
	
  public static final double DEFAULT_PH = 7.2;

  public static final String SOURCE_NONE  = "-";
  public static final String SOURCE_CHEBI = "chebi";
  public static final String SOURCE_KEGG  = "kegg";
  public static final String SOURCE_EC    = "ec";
  public static final String SOURCE_GO    = "go";
  public static final String SOURCE_SBML  = "sbml";
  public static final String SOURCE_STRUCTURE       = "st";
  public static final String SOURCE_ST_SMILES       = "smiles";
  public static final String SOURCE_ST_INCHI        = "inchi";
  public static final String SOURCE_ST_INCHIKEY     = "ikey";
  public static final String SOURCE_FORMULA_SBML    = "fm_sbml";
  public static final String SOURCE_FORMULA_NEUTRAL = "fm_0";
  public static final String SOURCE_FORMULA_OTHER   = "fm_other";
  public static final String SOURCE_FORMULA_CHEBI   = "fm_chebi";
  public static final String SOURCE_FORMULA         = "fm_%";

  public static final String ENTRY_NONE  = "NONE";

  public static final String BIOMASS = "Biomass";

  public static final int MAPPING_NONE = 0;
  public static final int MAPPING_AUTO = 1;
  public static final int MAPPING_MANUSER = 2;
  public static final int MAPPING_MANFILE = 3;

  public static final String COMPARTMENT_EXT = "extracellular";

  public static final int LV_MARGIN = 5;

  public static final int LIMIT_CHARGE = 199;

  private static final String EXCHANGE = "exchange";
  private static final String CURATED  = "curated";
  private static final String PTE_XREF = "ptexref";

  private static final Pattern PTT_SPLIT = Pattern.compile("(?=[A-Z][a-z]?\\d*)");

  private static Matcher mtEmbededDigit = Pattern.compile("[a-z](\\d+)").matcher("");

  private static Matcher mtFmC = Pattern.compile("C(\\d+)").matcher("");
  private static Matcher mtFmRara = Pattern.compile("[RXn]").matcher("");

  private static Matcher mtCompartmentExt = Pattern.compile("(ext|out|^e$|_e$)", Pattern.CASE_INSENSITIVE).matcher("");
  private static Matcher mtCompartmentCyt = Pattern.compile("(cytosol|^c$|_c$|^i$|int)", Pattern.CASE_INSENSITIVE).matcher("");

  private static Matcher mtCompartment = Pattern.compile("_(\\w)$").matcher("");

  public enum Status {FOUND, FOUND_ENTRYNONE, NOTFOUND, DUDE};

  //private static Matcher mtFmError = Pattern.compile("^[R|X]$").matcher("");

  // MODIFICAR MODELO - SPECIES
  // ------------------------------------------------------------

  /**
   * @param sp
   * @return
   */
  public static boolean isExchangeCompound(Species sp) {
    List<String> cvList = sp.filterCVTerms(CVTerm.Qualifier.BQM_UNKNOWN, EXCHANGE);
    return (!cvList.isEmpty());
  }

  /**
   * @return
   */
  public static CVTerm getExchangeCVTerm() {
    return new CVTerm(CVTerm.Qualifier.BQM_UNKNOWN, EXCHANGE);
  }

  /**
   * @param sp
   */
  public static void removeExchangeResource(Species sp) {
    if (sp.getNumCVTerms() == 1 && isExchangeCompound(sp)) {
      sp.getAnnotation().unsetCVTerms();
    }
  }


  public static boolean isCuratedCompound(Species sp) {
    List<String> cvList = sp.filterCVTerms(CVTerm.Qualifier.BQM_UNKNOWN, CURATED);
    return (!cvList.isEmpty());
  }

  public static CVTerm getCurateCVTerm() {
    return new CVTerm(CVTerm.Qualifier.BQM_UNKNOWN, CURATED);
  }


  public static boolean isPteXref(Species sp) {
    List<String> cvList = sp.filterCVTerms(CVTerm.Qualifier.BQM_UNKNOWN, PTE_XREF);
    return (!cvList.isEmpty());
  }

  public static CVTerm getPteXrefCVTerm() {
    return new CVTerm(CVTerm.Qualifier.BQM_UNKNOWN, PTE_XREF);
  }


  // MODIFICAR MODELO - REACTIONS
  // ------------------------------------------------------------

  /**
   * @param rx
   * @return
   */
  public static boolean isExchangeReaction(Reaction rx) {
    List<String> cvList = rx.filterCVTerms(CVTerm.Qualifier.BQM_UNKNOWN, EXCHANGE);
    return (!cvList.isEmpty());
  }

  /**
   * @param sp
   */
  public static void removeExchangeResource(Reaction rx) {
    if (rx.getNumCVTerms() == 1 && isExchangeReaction(rx)) {
      rx.getAnnotation().unsetCVTerms();
    }
  }


  // ------------------------------------------------------------
  // ------------------------------------------------------------

  public static String cleanId(String id) {
    return id.replaceFirst("_[a-z]$", "");
  }

  /**
   * @param compartment
   * @return
   */
  public static boolean isExtracellular(String compartment) {
    // Compartimento extracelular
    mtCompartmentExt.reset(compartment);
    return mtCompartmentExt.find();
  }

  /**
   * @param compartment
   * @return
   */
  public static boolean isCytosolic(String compartment) {
    // Compartimento extracelular
    mtCompartmentCyt.reset(compartment);
    return mtCompartmentCyt.find();
  }



  public static int compareFormulas(String fm1, String fm2) {
    // Si la formula contiene parentesis no hacemos map
    if (fm2.contains("(") || fm1.contains("(")) {
      return 1;
    }
    else if (fm2.matches("^[A-Za-z0-9]+$")) {
      String[] fma1 = PTT_SPLIT.split(fm1);
      String[] fma2 = PTT_SPLIT.split(fm2);

      // Mapear
      int l1 = fma1.length;
      int l2 = fma2.length;

      int totEq = 0;

      // Intercambiar si array2 es mayor que array1
      if (l2 > l1) {
        String[] swap = fma1;
        fma1 = fma2;
        fma2 = swap;

        l1 = fma1.length;
        l2 = fma2.length;
      }

      for (String s1 : fma1) {
        for (String s2 : fma2) {
          if (s1.equals(s2)) {
            totEq++;
            break;
          }
        }
      }

      // Devolver la diferencia
      return (l1 - totEq);
    }
    else {
      return 1;
    }
  }

  /**
   * Metodo basico que devuelve el numero de carbonos de la formula.
   * (Si solo hay un carbono se devuelve cero)
   *
   * @param fm
   * @return nº de carbonos
   */
  public static int getNumCs(String fm) {
    if (mtFmC.reset(fm).find()) {
      return Integer.parseInt(mtFmC.group(1));
    }
    return 0;
  }

  /**
   * Metodo que determina si la formula contiene caracteres extraños.
   *
   * @param fm
   * @return true o false
   */
  public static boolean isRareFm(String fm) {
    return mtFmRara.reset(fm).find();
  }


  /**
   * Metodo que ordena la formula y elimina unos y ceros asociados a los elementos
   * (Ej: H0 -> H, C43H71O6P1 -> C43H71O6P)
   * Tambien se eliminan formulas erroneas (X, R)
   *
   * @param fm formula a corregir
   * @return formula corregida o null si es erronea (X o R)
   */
  public static String getFormulaCorrected(String fm) {
    StringBuilder sb = new StringBuilder();

    String[] array = PTT_SPLIT.split(fm);

    // Ordenar elementos
    Arrays.sort(array);

    int l1 = array.length;
    for (int i=0; i<l1; i++) {
      String tk = array[i];
      if (tk.isEmpty()) continue;

      // Eliminar valor 0 o 1 asociado a un elemento (ej: C43H71O6P1 -> C43H71O6P)
      if (tk.matches("^[A-Za-z]+[01]$")) {
        tk = tk.substring(0, tk.length()-1);
      }

      sb.append(tk);
    }

    return sb.toString();
  }

  /**
   * Metodo que devuelve la formula neutra.
   * Además ordena la formula y elimina unos y ceros asociados a los elementos
   * (Ej: H0 -> H, C43H71O6P1 -> C43H71O6P)
   *
   * @param fm formula
   * @param carga
   * @return la formula neutra y corregida
   */
  public static String getFormulaNeutra(String fm, int carga) {
    String[] array = PTT_SPLIT.split(fm);

    if (carga == 0 || carga > LIMIT_CHARGE) {
      return fm;
    }
    // Corregir H para igualar cargas a cero
    else {
      int l1 = array.length;

      // Existe H
      if (fm.contains("H")) {
        for (int i=0; i<l1; i++) {
          if (array[i].isEmpty()) continue;

          if (array[i].matches("^H[^a-z]?\\d*$")) {
            String val = array[i].substring(1);
            int newVal = (val.isEmpty()) ? 1 : Integer.valueOf(val);
            newVal -= carga;
            array[i] = "H" + newVal;
          }
        }
      }
      // No existe H
      else {
        String[] newArray = Arrays.copyOf(array, l1 + 1);
        newArray[l1] = (Math.abs(carga) == 1) ? "H" : "H" + Math.abs(carga);
        array = newArray;
      }

      // Ordenar elementos
      Arrays.sort(array);

      // Concatenar
      StringBuilder sb = new StringBuilder();
      for (String s : array) {
        // Eliminar valor 0 o 1 asociado a un elemento (ej: C43H71O6P1 -> C43H71O6P)
        if (s.matches("^[A-Za-z]+[01]$")) {
          s = s.substring(0, s.length()-1);
        }
        sb.append(s);
      }

      return sb.toString();
    }
  }

  public static List<String> splitChemicalName(String chem) {
    // Corregir digitos embebidos
    boolean found = mtEmbededDigit.reset(chem).find();
    while (found) {
      int start = mtEmbededDigit.start(1);
      StringBuilder sb = new StringBuilder();
      sb.append(chem.substring(0, start)).append(" ").append(chem.substring(start));
      chem = sb.toString();

      found = mtEmbededDigit.reset(chem).find(start);
    }

    chem = chem.replaceAll("\\s?\\(\\d*[-\\+]\\)$", ""); // quitar cargas al final del string
    chem = chem.replaceAll("[^A-Za-z0-9:=]", " "); //chem.replaceAll("[\\W_]", " ");
    chem = chem.replaceAll("\\s+", " ").trim();

    String tks[] = chem.split(" ");

    List<String> list = new LinkedList<String>();
    StringBuilder sb = new StringBuilder();
    for (String s : tks) {
      if (s.length() == 1) {
        sb.append(s);
      }
      else if (s.length() > 1) {
        sb.append(s);
        // No numeros
        if (!s.matches("\\d+")) {
          list.add(sb.toString());
          sb.setLength(0);
        }
      }
    }
    // Remanente
    if (sb.length() > 0)
      list.add(sb.toString());

    Collections.sort(list);

    return list;
  }

  public static String getChemicalName(List<String> splitChemName) {
    StringBuilder sb = new StringBuilder();
    for (String s : splitChemName) {
      sb.append(s);
    }
    return sb.toString();
  }

  /**
   * <p>Joins the elements of the provided {@code Iterable} into
   * a single String containing the provided elements.</p>
   *
   * <p>No delimiter is added before or after the list.
   * A {@code null} separator is the same as an empty String ("").</p>
   *
   * @param iterable  the {@code Iterable} providing the values to join together, may be null
   * @param separator  the separator character to use, null treated as ""
   * @return the joined String, {@code null} if null iterator input
   */
  public static String join(Iterable<?> iterable, String separator) {
      if (iterable == null) {
          return null;
      }
      Iterator<?> iterator = iterable.iterator();

      // handle null, zero and one elements before building a buffer
      if (iterator == null) {
          return null;
      }
      if (!iterator.hasNext()) {
          return "";
      }
      Object first = iterator.next();
      if (!iterator.hasNext()) {
          return ((first == null) ? "" : first.toString());
      }

      // two or more elements
      StringBuilder buf = new StringBuilder(256); // Java default is 16, probably too small
      if (first != null) {
          buf.append(first);
      }

      while (iterator.hasNext()) {
          if (separator != null) {
              buf.append(separator);
          }
          Object obj = iterator.next();
          if (obj != null) {
              buf.append(obj);
          }
      }
      return buf.toString();
  }

  /**
   * @return true si el web service esta activo
   * @throws ChebiException
   */
  public static boolean checkChebiWebService() throws ChebiException {
    boolean ok = true;
    try {
      new ChebiWebServiceClient();
    }
    catch (Exception e) {
      ok = false;
      throw new ChebiException(e);
    }
    return ok;
  }

  public static String getCompartmentAbb(Species sp) {
    String compartment = sp.getCompartment().trim();

    String compartmentAbb = "c";
    if (sp.getBoundaryCondition()) {
      compartmentAbb = "b";
    }
    else if (Util.isCytosolic(compartment)) {
      compartmentAbb = "c";
    }
    else if (Util.isExtracellular(compartment)) {
      compartmentAbb = "e";
    }
    else {
      mtCompartment.reset(sp.getId().trim());
      if (mtCompartment.find()) {
        compartmentAbb = mtCompartment.group(1);
      }
      else {
        compartmentAbb = "x";
        LOGGER.log(Level.WARNING, "Abrev. compartimento desconocida para {0}", sp.getId().trim());
      }
    }

    return compartmentAbb;
  }

	// This code was needed due a bug in com.fasterxml.woodstox:woodstox-core:5.0.1
	// which was fixed in com.fasterxml.woodstox:woodstox-core:5.0.2
	public static void cleanXMLNode(XMLNode node) {
		node.clearNamespaces();
		int numChildren = node.getChildCount();
		for(int iChild = 0; iChild < numChildren ; iChild++) {
			XMLNode child = node.getChildAt(iChild);
			cleanXMLNode(child);
		}
	}
	
	// This code was needed due a bug in com.fasterxml.woodstox:woodstox-core:5.0.1
	// which was fixed in com.fasterxml.woodstox:woodstox-core:5.0.2
	public static String getCleanNotesString(SBase sp)
		throws XMLStreamException
	{
		// This is needed to avoid getting "Trying to redeclare prefix xml ..." exception
		XMLNode notesNode = sp.getNotes().clone();
		cleanXMLNode(notesNode);
		
		return notesNode.toXMLString();
	}
	
	protected static XmlnsPrefixer fixer = null;
	
	// This code was needed due a bug in com.fasterxml.woodstox:woodstox-core:5.0.1
	// which was fixed in com.fasterxml.woodstox:woodstox-core:5.0.2
	public static SBMLDocument readFixedSBML(File file)
		throws XMLStreamException, IOException
	{
		// Temp file which will contain the patched version of the document
		File output = File.createTempFile("cmodelfixer",".xml");
		try {
			if(fixer == null) {
				fixer = new XmlnsPrefixer();
			}
			// First, let's patch the input XML file
			fixer.doPrefix(file,output);
			
			return SBMLReader.read(output);
		} catch(TransformerException te) {
			throw new IOException("Error while fixing SBML file",te);
		} finally {
			output.delete();
		}
	}
	
	// It clones the reaction, giving it a new metaid, as well as its reactants and products
	public static Reaction cloneReaction(Reaction r) {
		Reaction rclone = r.clone();
		rclone.unsetMetaId();
		
		for(SpeciesReference spr : rclone.getListOfProducts()) {
			spr.unsetMetaId();
		}
		
		for(SpeciesReference sre: rclone.getListOfReactants()) {
			sre.unsetMetaId();
		}
		
		return rclone;
	}
	
	// As these global parameters are added only once, we need to
	// control their lifecycle in the model
	public static Parameter getDefaultLowerBoundParameter(Model docModel) {
		Parameter lbParam = null;
		if(docModel.containsParameter(Util.DEFAULT_LOWER_BOUND_PARAM_ID)) {
			lbParam = docModel.getParameter(Util.DEFAULT_LOWER_BOUND_PARAM_ID);
		} else {
			lbParam = docModel.createParameter(Util.DEFAULT_LOWER_BOUND_PARAM_ID);
			lbParam.setName(Util.DEFAULT_LOWER_BOUND_PARAM_NAME);
			lbParam.setValue(Util.DEFAULT_LOWER_BOUND_VALUE);
			lbParam.setConstant(true);
			lbParam.setUnits(docModel.getExtentUnits());
			docModel.addParameter(lbParam);
		}
		
		return lbParam;
	}
	
	public static Parameter getDefaultUpperBoundParameter(Model docModel) {
		Parameter ubParam = null;
		if(docModel.containsParameter(Util.DEFAULT_UPPER_BOUND_PARAM_ID)) {
			ubParam = docModel.getParameter(Util.DEFAULT_UPPER_BOUND_PARAM_ID);
		} else {
			ubParam = docModel.createParameter(Util.DEFAULT_UPPER_BOUND_PARAM_ID);
			ubParam.setName(Util.DEFAULT_UPPER_BOUND_PARAM_NAME);
			ubParam.setValue(Util.DEFAULT_UPPER_BOUND_VALUE);
			ubParam.setConstant(true);
			ubParam.setUnits(docModel.getExtentUnits());
			docModel.addParameter(ubParam);
		}
		
		return ubParam;
	}
	
	public static Parameter getImpossibleLowerBoundParameter(Model docModel) {
		Parameter lbParam = null;
		if(docModel.containsParameter(Util.IMPOSSIBLE_LOWER_BOUND_PARAM_ID)) {
			lbParam = docModel.getParameter(Util.IMPOSSIBLE_LOWER_BOUND_PARAM_ID);
		} else {
			lbParam = docModel.createParameter(Util.IMPOSSIBLE_LOWER_BOUND_PARAM_ID);
			lbParam.setName(Util.IMPOSSIBLE_LOWER_BOUND_PARAM_NAME);
			lbParam.setValue(Util.IMPOSSIBLE_LOWER_BOUND_VALUE);
			lbParam.setConstant(true);
			lbParam.setUnits(docModel.getExtentUnits());
			docModel.addParameter(lbParam);
		}
		
		return lbParam;
	}
	
	// If the kinetic law does not exist, it is created
	public static KineticLaw getKineticLaw(Reaction r) {
		KineticLaw klr = null;
		if(r.isSetKineticLaw()) {
			klr = r.getKineticLaw();
		} else {
			klr = r.createKineticLaw();
			r.setKineticLaw(klr);
		}
		
		return klr;
	}
	
	public static double getLowerBoundValue(Reaction r, boolean isFBC) {
		double lowerBoundValue;
		if(isFBC) {
			Model model = r.getModel();
			FBCReactionPlugin fbcRP = (FBCReactionPlugin)r.getPlugin(FBCConstants.shortLabel);
			
			Parameter lowerBoundInstance = fbcRP.getLowerFluxBoundInstance();
			lowerBoundValue = lowerBoundInstance.getValue();
		} else {
			KineticLaw kl = Util.getKineticLaw(r);
			lowerBoundValue = kl.getListOfLocalParameters().get(Util.LOCAL__LOWER_BOUND_PARAM).getValue();
		}
		
		return lowerBoundValue;
	}
	
	public static void setDefaultLowerBoundValue(Reaction r, boolean isFBC) {
		if(isFBC) {
			Model model = r.getModel();
			FBCReactionPlugin fbcRP = (FBCReactionPlugin)r.getPlugin(FBCConstants.shortLabel);
			
			fbcRP.setLowerFluxBound(Util.getDefaultLowerBoundParameter(model));
		} else {
			KineticLaw kl = Util.getKineticLaw(r);
			kl.getListOfLocalParameters().get(Util.LOCAL__LOWER_BOUND_PARAM).setValue(Util.DEFAULT_LOWER_BOUND_VALUE);
		}
	}
	
	public static void setImpossibleLowerBoundValue(Reaction r, boolean isFBC) {
		if(isFBC) {
			Model model = r.getModel();
			FBCReactionPlugin fbcRP = (FBCReactionPlugin)r.getPlugin(FBCConstants.shortLabel);
			
			fbcRP.setLowerFluxBound(Util.getImpossibleLowerBoundParameter(model));
		} else {
			KineticLaw kl = Util.getKineticLaw(r);
			kl.getListOfLocalParameters().get(Util.LOCAL__LOWER_BOUND_PARAM).setValue(Util.IMPOSSIBLE_LOWER_BOUND_VALUE);
		}
	}
	
	public static boolean isActiveObjective(Reaction r,boolean isFBC) {
		if(isFBC) {
			Model model = r.getModel();
			FBCModelPlugin fbcModel = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
			if(fbcModel.isSetListOfObjectives() && fbcModel.isSetActiveObjective()) {
				Objective obj = fbcModel.getActiveObjectiveInstance();
				if(obj.isSetListOfFluxObjectives()) {
					ListOf<FluxObjective> lOfFluxObj = obj.getListOfFluxObjectives();
					for(FluxObjective fObj: lOfFluxObj) {
						if(fObj.isSetReaction() && fObj.getReaction().equals(r.getId()) && fObj.isSetCoefficient() && fObj.getCoefficient() == Util.OBJECTIVE_COEFFICIENT_VALUE) {
							return true;
						}
					}
					return false;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} else {
			KineticLaw klr = Util.getKineticLaw(r);
			return klr.getListOfLocalParameters().get(Util.LOCAL__OBJECTIVE_COEFFICIENT_PARAM)!=null && klr.getListOfLocalParameters().get(Util.LOCAL__OBJECTIVE_COEFFICIENT_PARAM).getValue() == Util.OBJECTIVE_COEFFICIENT_VALUE;
		}
	}
	
	public static void setDefaultBoundsToReaction(Model model, Reaction r,boolean isFBC) {
		if(isFBC) {
		// Doing it nicely
			FBCReactionPlugin fbcRP = (FBCReactionPlugin)r.getPlugin(FBCConstants.shortLabel);
			
			fbcRP.setLowerFluxBound(Util.getDefaultLowerBoundParameter(model));
			fbcRP.setUpperFluxBound(Util.getDefaultUpperBoundParameter(model));
		} else {
		// Traditional way before FBC
			KineticLaw kl = Util.getKineticLaw(r);
			kl.getListOfLocalParameters().get(Util.LOCAL__LOWER_BOUND_PARAM).setValue(Util.DEFAULT_LOWER_BOUND_VALUE);
			kl.getListOfLocalParameters().get(Util.LOCAL__UPPER_BOUND_PARAM).setValue(Util.DEFAULT_UPPER_BOUND_VALUE);
		}
	}
	
	public static void addReactionSettingDefaultBounds(Model model,Reaction r, boolean isFBC) {
		Util.setDefaultBoundsToReaction(model,r,isFBC);
		
		if(!isFBC) {
			// This parameter has no equivalence in SBML3 + FBC
			KineticLaw kl = Util.getKineticLaw(r);
			kl.getListOfLocalParameters().get(Util.LOCAL__FLUX_VALUE_PARAM).setValue(0.0);
		}

		model.addReaction(r);
	}
	
	public static void setActiveObjectiveReaction(Model model, Reaction r, Reaction prevR, boolean isFBC) {
		if(r!=null) {
			Util.addReactionSettingDefaultBounds(model,r,isFBC);
		} else if(prevR!=null) {
			Util.addReactionSettingDefaultBounds(model,prevR,isFBC);
		}
		
		// Now, disable the previous reaction's objective coefficient
		if(prevR!=null) {
			// Cambiar a cero el OBJECTIVE_COEFFICIENT de la reaccion original
			if(isFBC) {
				FBCModelPlugin fbcModel = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
				
				if(fbcModel.isSetListOfObjectives()) {
					ListOfObjectives lOfObj = fbcModel.getListOfObjectives();
					for(Objective obj: lOfObj) {
						ListOf<FluxObjective> lOfFluxObj = obj.getListOfFluxObjectives();
						for(FluxObjective fObj: lOfFluxObj) {
							if(fObj.isSetReaction() && fObj.getReaction().equals(prevR.getId())) {
								fObj.setCoefficient(Util.DISABLED_OBJECTIVE_COEFFICIENT_VALUE);
							}
						}
					}
				}
			} else {
				KineticLaw klrPrev = Util.getKineticLaw(prevR);
				if(klrPrev != null) {
					klrPrev.getListOfLocalParameters().get(Util.LOCAL__OBJECTIVE_COEFFICIENT_PARAM).setValue(Util.DISABLED_OBJECTIVE_COEFFICIENT_VALUE);
				}
			}
		}

		// and enable it in the current reaction
		if(r!=null) {
			// Mantener el OBJECTIVE_COEFFICIENT de la reaccion original en la nueva reac (deberia ser 1)
			if(isFBC) {
				FBCModelPlugin fbcModel = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
				 
				Objective obj = fbcModel.createObjective();
				FluxObjective fObj = obj.createFluxObjective();
				
				fObj.setReaction(r);
				fObj.setCoefficient(Util.OBJECTIVE_COEFFICIENT_VALUE);
				
				obj.addFluxObjective(fObj);
				fbcModel.setActiveObjective(obj);
			} else {
				KineticLaw klr = Util.getKineticLaw(r);
				
				if(klr.getListOfLocalParameters().get(Util.LOCAL__OBJECTIVE_COEFFICIENT_PARAM)==null || klr.getListOfLocalParameters().get(Util.LOCAL__OBJECTIVE_COEFFICIENT_PARAM).getValue()==Util.DISABLED_OBJECTIVE_COEFFICIENT_VALUE) {
					klr.getListOfLocalParameters().get(Util.LOCAL__OBJECTIVE_COEFFICIENT_PARAM).setValue(Util.OBJECTIVE_COEFFICIENT_VALUE); // A veces es cero
				}
			}
		}
	}
	
	private static Matcher mtFormula = Pattern.compile("FORMULA:\\s?([A-Za-z0-9]+)").matcher("");
	
	public static String getFormulaFromSpecies(Species s,boolean isFBC) {
		String formula = null;
		if(isFBC) {
			FBCSpeciesPlugin fbcSpecies = (FBCSpeciesPlugin)s.getPlugin(FBCConstants.shortLabel);
			if(fbcSpecies.isSetChemicalFormula()) {
				formula = fbcSpecies.getChemicalFormula();
			}
		} else if(s.isSetNotes()) {
			try {
				mtFormula.reset(s.getNotesString());
				if (mtFormula.find()) {
					formula = mtFormula.group(1);
				}
			} catch (XMLStreamException e1) {
				e1.printStackTrace();
			}
		}
		
		return formula;
	}
	
	public static void setFormulaToSpecies(Species s,String formula,boolean isFBC) {
		if(isFBC) {
			FBCSpeciesPlugin fbcSpecies = (FBCSpeciesPlugin)s.getPlugin(FBCConstants.shortLabel);
			fbcSpecies.setChemicalFormula(formula);
		} else {
			String prevNotes = "";
			if(s.isSetNotes()) {
				try {
					mtFormula.reset(s.getNotesString());
					if (mtFormula.find()) {
						prevNotes = mtFormula.group(1);
					}
				} catch (XMLStreamException e1) {
					e1.printStackTrace();
				}
			}
			try {
				s.setNotes(prevNotes+"FORMULA: "+formula+"\n");
			} catch(XMLStreamException e1) {
				e1.printStackTrace();
			}
		}
	}
}
