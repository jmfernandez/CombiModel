package es.csic.cnb.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Species;

import es.csic.cnb.db.DbManager;
import es.csic.cnb.db.SeedDbManager;
import es.csic.cnb.util.Util;

/**
 *
 * @author Pablo D. Sánchez
 *
 */
public class NetCompound {
  private final static Logger LOGGER = Logger.getLogger(NetCompound.class.getName());

  private Matcher mtChebi = Pattern.compile("chebi:CHEBI%3A(\\d+)$").matcher("");
  private Matcher mtKegg = Pattern.compile("kegg\\.compound:(C\\d+)$").matcher("");

  private Matcher mtCharge = Pattern.compile("CHARGE:\\s?(\\d+)").matcher("");
  private Matcher mtNameFormula = Pattern.compile("^(.+)_((?:[A-Z][a-z]?\\d*)+)$").matcher("");
  private Matcher mtSyn = Pattern.compile("SYN:\\s?(.+)<\\/p>").matcher("");

  private Matcher mtNum = Pattern.compile("[a-z]\\d+").matcher("");

  private Matcher mtP = Pattern.compile("l(?:phosphate|diphosphate)").matcher("");

//  private Matcher mtCompartment = Pattern.compile("_(\\w)$").matcher("");

  private Species species;

  // Identificador del compuesto -ID de la base de datos- (permite normalizar)
  private int chemId;
  private String chemIdStr;

  private String sbmlId;   // Id del compuesto en el modelo
  private String sbmlCleanId;
  private String sbmlName; // Nombre del compuesto en el modelo
  private String compartment;
  private String compartmentAbb;

  private String sbmlFormula;
  private String neutralFormula;
  private int charge  = 0;
  private boolean validateCharge;

  private String ofn;     // Nombre oficial
  private String inchi;
  private String inchikey;
  private Set<String> smilesList;
  private Set<String> synList;
  private Map<String,Set<Fm>> fmList; // Formulas
  private Map<String,Set<String>> xrefList;

  private String structure;

  private int mapping = Util.MAPPING_NONE; // Flag para saber si existe correccion manual
  private boolean exchangeCompound = false;

  private boolean pendingCurationXrefMap = false;
  private boolean pendingCurationDbMap = false;
  private boolean manualCuration = false;

  private int dorsal;
  
  private boolean isFBC;

  @SuppressWarnings("deprecation")
  public NetCompound(Species sp, boolean isFBC) {
	this.isFBC = isFBC;
    this.smilesList = new HashSet<String>();
    this.synList    = new HashSet<String>();
    this.fmList     = new HashMap<String,Set<Fm>>();
    this.xrefList   = new HashMap<String,Set<String>>();
    // Kegg
    xrefList.put(Util.SOURCE_KEGG, new HashSet<String>());
    // Chebi
    xrefList.put(Util.SOURCE_CHEBI, new HashSet<String>());

    this.species = sp;


    // Beginning in SBML Level 2 Version 2, the 'charge' attribute on Species is deprecated and in
    // SBML Level 3 it does not exist at all.
    // Its use strongly discouraged. Its presence is considered a misfeature in earlier definitions of SBML
    this.charge = sp.getCharge();
    //charge = (charge > Util.LIMIT_CHARGE) ? 0 : charge;
    this.validateCharge = (sp.getModel().getVersion() > 2 ||
            (sp.getModel().getVersion() == 2 && sp.getModel().getLevel() >= 3)) ? false : true;

    // Ver si existe carga como atributo en el compuesto
    if (!sp.writeXMLAttributes().containsKey("charge")) {
      this.validateCharge = false;
    }

    // Recuperar la carga que aparece en las notas del SBML
    try {
		mtCharge.reset(sp.getNotesString());
	} catch (XMLStreamException e2) {
		e2.printStackTrace();
	}
    if (mtCharge.find()) {
      this.charge = Integer.parseInt(mtCharge.group(1));
      this.validateCharge = true;

      sp.setCharge(charge);
    }


    this.sbmlId = sp.getId().trim();
    // Limpia el id del SBML quitando la mencion a su localizacion (SEED SBMLs).
    this.sbmlCleanId = Util.cleanId(sbmlId); //sbmlId.replaceFirst("_[a-z]$", "");

    boolean changeName = false;

    // Recuperar la formula que aparece en las notas del SBML
    String sbmlFormula = Util.getFormulaFromSpecies(sp,isFBC);
    if (sbmlFormula!=null) {
      this.sbmlFormula = sbmlFormula;

      // Limpiar nombre
      this.sbmlName = cleanName(sp.getName()).trim();

      changeName = true;
    }
    // Recuperar la formula que aparece en el nombre y lo limpio
    else {
      this.sbmlName = cleanNameWithFm(sp.getName());
    }


    // Anadir syns a partir del nombre
    this.synList.add(sp.getName().trim());
    this.synList.add(sbmlName);
    this.synList.add(cleanNameToCreateSyn(sbmlName));
    this.synList.add(cleanNameGreek(sbmlName));

    // Recuperar syns
    try {
		mtSyn.reset(sp.getNotesString());
	} catch (XMLStreamException e) {
		e.printStackTrace();
	}
    if (mtSyn.find()) {
      String syn = mtSyn.group(1);
      this.synList.add(syn);
    }

    // Cambiar nombre en el modelo para incluir formula (para que sea compatible con cobra)
    if (changeName) {
      sp.setName(sp.getName().trim().concat("_").concat(sbmlFormula));
    }


    // Compartimentos
    this.compartment = sp.getCompartment().trim();
    compartmentAbb = Util.getCompartmentAbb(sp);

//    this.compartmentAbb = "c";
//    if (sp.getBoundaryCondition()) {
//      compartmentAbb = "b";
//    }
//    else if (Util.isCytosolic(compartment)) {
//      compartmentAbb = "c";
//    }
//    else if (Util.isExtracellular(compartment)) {
//      compartmentAbb = "e";
//    }
//    else {
//      mtCompartment.reset(sp.getId().trim());
//      if (mtCompartment.find()) {
//        compartmentAbb = mtCompartment.group(1);
//      }
//      else {
//        compartmentAbb = "x";
//        LOGGER.log(Level.WARNING, "Abrev. compartimento desconocida para {0}", sbmlId);
//      }
//    }


    // Ver si es compuesto de intercambio
    this.exchangeCompound = Util.isExchangeCompound(sp);


    // Buscar identificadores externos
    for (CVTerm cv : sp.getAnnotation().getListOfCVTerms()) {
      for (String str : cv.getResources()) {
        mtChebi.reset(str);
        if (mtChebi.find()) {
          this.addChebiId("CHEBI:"+mtChebi.group(1));
        }
        mtKegg.reset(str);
        if (mtKegg.find()) {
          String keggId = mtKegg.group(1);
          this.addKeggId(keggId);
        }
      }
    }
    // Guardar en la lista de xrefs el id del compuesto en el sbml
    if (!sbmlCleanId.startsWith("tmp_")) {
      this.addXref(Util.SOURCE_SBML, sbmlCleanId);
    }


    // Corregir formula
    if (this.sbmlFormula != null) {
      this.sbmlFormula = Util.getFormulaCorrected(sbmlFormula);

      // Si la formula es X o R
      if (this.sbmlFormula.matches("^[RX]$")) {
        // Guardar en la lista de xrefs la formula que aparece en el SBML
        this.addFm(Util.SOURCE_FORMULA_OTHER, sbmlFormula, charge);
        // Dejar la formula como null
        this.sbmlFormula = null;
      }
      else {
        // Guardar en la lista de xrefs la formula que aparece en el SBML corregida
        this.addFm(Util.SOURCE_FORMULA_SBML, sbmlFormula, charge);

        // Obtener forma neutra
        this.neutralFormula = Util.getFormulaNeutra(sbmlFormula, charge);
        // Guardar en la lista de xrefs la formula neutra
        this.addFm(Util.SOURCE_FORMULA_NEUTRAL, neutralFormula, 0);
      }
    }

//  // Ver si existe referencia a Kegg (solo si la carga es cero)
//  if (validateCharge && charge == 0) {
//    List<String> keggIdList = DbManager.INSTANCE.getSeedDbMgr().getKeggIdList(sbmlCleanId);
//    for (String keggId : keggIdList) {
//      this.addKeggId(keggId);
//    }
//  }
//
//    // Si existe referencia a Kegg recupero mas datos
//    Set<String> keggList = this.getKeggIdList();
//    if (!keggList.isEmpty()) {
//      KeggDbManager keggDb = DbManager.INSTANCE.getKeggDbMgr();
//      for (String keggId : keggList) {
//        String fmKegg = keggDb.getFormula(keggId);
//        if (fmKegg != null) {
//          // Corregir formula
//          fmKegg = Util.getFormulaCorrected(fmKegg);
//
//          // Guardar si es diferente
//          if (!(fmKegg.equals(neutralFormula) || fmKegg.equals(sbmlFormula))) {
//            // Guardar en la lista de xrefs la formula que aparece en Kegg
//            this.addFm(Util.SOURCE_FORMULA_OTHER, fmKegg, charge);
//
//            LOGGER.log(Level.FINE,
//                    "{0}: NO COINCIDE LA FORMULA DE KEGG: {1} con la del SBML {2} - Neutra: {3}",
//                    new Object[]{sbmlId, fmKegg, sbmlFormula, neutralFormula});
//          }
//        }
//
//        // Chebi
//        for (String chebiId : keggDb.getChebiIdList(keggId)) {
//          chebiId = "CHEBI:" + chebiId;
//          // Guardar en la lista de xrefs
//          this.addChebiId(chebiId);
//        }
//
//        // Guardar los sinonimos recuperados de kegg
//        for (String syn : keggDb.getSynList(keggId)) {
//          this.synList.add(syn);
//        }
//
//        // Guardar structures
//        for (String smiles : keggDb.getSmilesList(keggId)) {
//          this.addSmiles(smiles);
//        }
//        this.inchi = keggDb.getInchi(keggId);
//        this.inchikey = keggDb.getInchikey(keggId);
//
//      } // Fin for keggList
//    }

    // Guardar la formula recuperada de seed (solo si la carga es cero)
    if (validateCharge && charge == 0) {
      SeedDbManager seedDb = DbManager.INSTANCE.getSeedDbMgr();
      String seedFm = seedDb.getSeedFormula(sbmlCleanId);
      if (seedFm != null) {
        // Corregir formula
        seedFm = Util.getFormulaCorrected(seedFm);

        // Guardar si es diferente
        if (!(seedFm.equals(neutralFormula) || seedFm.equals(sbmlFormula))) {
          // Guardar en la lista de xrefs la formula que aparece en Kegg
          this.addFm(Util.SOURCE_FORMULA_OTHER, seedFm, charge);

          LOGGER.log(Level.FINE,
                  "{0}: NO COINCIDE LA FORMULA DE SEED: {1} con la del SBML {2} - Neutra: {3}",
                  new Object[]{sbmlId, seedFm, sbmlFormula, neutralFormula});
        }
      }

      // Guardar los sinonimos recuperados de seed
      for (String syn : seedDb.getSeedSynList(sbmlCleanId)) {
        this.synList.add(syn);
      }
    }
  }

  /**
   * Elimina elementos artificiales del nombre (formula, guiones bajos, etc)
   * y extrae la formula.
   *
   * @param str El nombre
   *
   * @return  El nombre sin elementos artificiales.
   */
  private String cleanNameWithFm(String str) {
    if (str.length() == 0)
      return null;

    if (str.contains("_")) {
      // Recuperar formula
      mtNameFormula.reset(str);
      if (mtNameFormula.find()) {
        str = mtNameFormula.group(1);
        this.sbmlFormula = mtNameFormula.group(2);
      }

      // Quitar la 'M' inicial si existe
      str = str.replaceFirst("^[Mm]_+", "");

      // Sustituir guiones bajos por blanco
      str = str.replaceAll("_+", " ");
    }
    return str.trim();
  }

  /**
   * Elimina elementos artificiales del nombre (guiones bajos, etc)
   *
   * @param str El nombre
   *
   * @return  El nombre sin elementos artificiales.
   */
  private String cleanName(String str) {
    if (str.length() == 0)
      return null;

    if (str.contains("_")) {
      // Quitar la 'M' inicial si existe
      str = str.replaceFirst("^[Mm]_+", "");

      // Sustituir guiones bajos por blanco
      str = str.replaceAll("_+", " ");
    }
    return str;
  }

  /**
   * Elimina ciertos elementos del nombre que pueden dificultar el match.
   *
   * @param str
   * @return El nombre modificado.
   */
  private String cleanNameToCreateSyn(String str) {
    if (str == null || str.length() == 0)
      return null;

    str = str.replaceAll("\\(?[Ee]\\.?\\s?coli\\)?", "");
    str = str.replaceAll("-E-?coli-", "");
    str = str.replaceAll("-?\\([Bb]\\.?\\s?subtilis\\)", "");
    str = str.replaceAll("\\([Ss]\\.?\\s?aureus\\)", "");
    str = str.replaceAll("\\([Hh]\\.?\\s?pylori\\)", "");
    str = str.replaceAll("\\([Mm]\\.?\\s?tb\\)", "");
    str = str.replaceAll("\\((mass|all|tuberculosis)\\)", "");
    str = str.replaceAll("-?\\s?obsolete", "");

    // Anadir blanco cuando no existe entre letra y numero (Ej: D-Ribulose5-phosphate)
    boolean found = mtNum.reset(str).find();
    if (found) {
      StringBuilder sb = new StringBuilder(str);
      sb.insert(mtNum.start()+1, ' ');
      str = sb.toString();
    }
    // Separar phosphate (Ej: Geranyldiphosphate --> Geranyl diphosphate)
    found = mtP.reset(str).find();
    if (found) {
      StringBuilder sb = new StringBuilder(str);
      sb.insert(mtP.start()+1, ' ');
      str = sb.toString();
    }

    return str.trim();
  }

  private String cleanNameGreek(String str) {
    if (str == null || str.length() == 0)
      return null;

    boolean hasGreekLetter = false;

    char[] chs = str.toCharArray();
    for (char ch : chs) {
      if (((ch >= 0x0370) && (ch < 0x0400)) || ((ch >= 0x1f00) && (ch < 0x2000))) {
        hasGreekLetter = true;
        break;
      }
    }

    if (hasGreekLetter) {
      String newstr = str;

      newstr = newstr.replaceAll("α", "alpha");
      newstr = newstr.replaceAll("β", "beta");
      newstr = newstr.replaceAll("[Γγ]", "gamma");
      newstr = newstr.replaceAll("[Δδ]", "delta");
      newstr = newstr.replaceAll("ε", "epsilon");
      newstr = newstr.replaceAll("ζ", "zeta");
      newstr = newstr.replaceAll("η", "eta");
      newstr = newstr.replaceAll("[Θθ]", "theta");
      newstr = newstr.replaceAll("κ", "kappa");
      newstr = newstr.replaceAll("[Λλ]", "lambda");
      newstr = newstr.replaceAll("μ", "mu");
      newstr = newstr.replaceAll("ν", "nu");
      newstr = newstr.replaceAll("[Ξξ]", "xi");
      newstr = newstr.replaceAll("[Ππ]", "pi");
      newstr = newstr.replaceAll("ρ", "rho");
      newstr = newstr.replaceAll("[Σσς]", "sigma");
      newstr = newstr.replaceAll("τ", "tau");
      newstr = newstr.replaceAll("[Φφ]", "phi");
      newstr = newstr.replaceAll("χ", "chi");
      newstr = newstr.replaceAll("[Ψψ]", "psi");
      newstr = newstr.replaceAll("[Ωω]", "omega");

      this.sbmlName = newstr.trim();
      return sbmlName;
    }

    return str;
  }

  public void update(WSCompound wscomp) {
    // Ver si existe estructura para calcular el pka
    this.structure = wscomp.getStructure();

    this.ofn = wscomp.getName();
    this.inchi = wscomp.getInchi();
    this.inchikey = wscomp.getInchiKey();
    this.mapping = wscomp.getMapping();
    this.addSmiles(wscomp.getSmiles());

    this.charge =  wscomp.getCharge();
    this.validateCharge = true;

    this.synList.add(this.ofn);
    this.synList.add(this.sbmlName);
    for (String syn : wscomp.getSynList()) {
      this.synList.add(syn);
    }

    Map<String, Set<String>> fmList = wscomp.getFormulaList();
    for (String source : fmList.keySet()) {
      for (String fm : fmList.get(source)) {
        this.addFm(source, fm, wscomp.getCharge());
      }
    }

    for (String source : wscomp.getXrefList().keySet()) {
      this.addXref(source, wscomp.getXrefList().get(source));
    }


  }

  public void addSyn(String syn) {
    if (syn != null)
      synList.add(syn);
  }

  public Set<String> getSynList() {
    return synList;
  }

  public Map<String,Set<Fm>> getFmList() {
    return fmList;
  }

  public Set<Fm> getFmList(String source) {
    return fmList.get(source);
  }

  public void addFm(String source, String fm, int charge) {
    if (fm != null && !fm.isEmpty()) {
      Set<Fm> list = fmList.get(source);
      if (list == null) {
        list = new HashSet<Fm>();
      }
      list.add(new Fm(fm, charge));
      fmList.put(source, list);
    }
  }

  public void addXref(String source, String xref) {
    if (xref != null && !xref.isEmpty()) {
      Set<String> list = xrefList.get(source);
      if (list == null) {
        list = new HashSet<String>();
      }
      list.add(xref);
      xrefList.put(source, list);
    }
  }

  public Map<String,Set<String>> getXrefList() {
    return xrefList;
  }

  public Set<String> getXrefList(String source) {
    return xrefList.get(source);
  }

  /**
   * @return the chemId
   */
  public int getChemId() {
    return chemId;
  }

  /**
   * @param chemId the chemId to set
   */
  public void setChemId(int chemId) {
    this.chemId = chemId;
    this.chemIdStr = "cp" + chemId + "_" + compartmentAbb;
  }

  /**
   * @return the chemId en formato "cp + chemId + compartmentAbb" (Ej: cp18)
   */
  public String getChemIdStr() {
    return chemIdStr;
  }

  /**
   * @return the ofn
   */
  public String getOfn() {
    return ofn;
  }

  /**
   * @param ofn the ofn to set
   */
  public void setOfn(String ofn) {
    this.ofn = ofn;
  }

  /**
   * @return the SBML id
   */
  public String getSbmlId() {
    return sbmlId;
  }

  /**
   * @return el id del sbml sin la mencion a su localizacion
   */
  public String getCleanSbmlId() {
    return sbmlCleanId;
  }

  /**
   * @return the SBML name
   */
  public String getSbmlName() {
    return sbmlName;
  }

  /**
   * @param name the SBML name to set
   */
  public void setSbmlName(String name) {
    this.sbmlName = name;
  }

  /**
   * @return the chebiId list
   */
  public Set<String> getChebiIdList() {
    return xrefList.get(Util.SOURCE_CHEBI);
  }

  /**
   * @param chebiId the chebiId to add
   */
  public void addChebiId(String chebiId) {
    if (chebiId != null && !chebiId.isEmpty()) {
      Set<String> list = xrefList.get(Util.SOURCE_CHEBI);
      list.add(chebiId);
      xrefList.put(Util.SOURCE_CHEBI, list);
    }
  }

  /**
   * @return the keggId list
   */
  public Set<String> getKeggIdList() {
    return xrefList.get(Util.SOURCE_KEGG);
  }

  /**
   * @param keggId the keggId to add
   */
  public void addKeggId(String keggId) {
    if (keggId != null && !keggId.isEmpty()) {
      Set<String> list = xrefList.get(Util.SOURCE_KEGG);
      list.add(keggId);
      xrefList.put(Util.SOURCE_KEGG, list);
    }
  }

  /**
   * @return the sbmlFormula
   */
  public String getSbmlFormula() {
    return sbmlFormula;
  }

  /**
   * @param sbmlFormula the sbmlFormula to set
   */
  public void setSbmlFormula(String sbmlFormula) {
    this.sbmlFormula = sbmlFormula;
  }

  /**
   * @return the neutralFormula
   */
  public String getNeutralFormula() {
    return neutralFormula;
  }

  /**
   * @param neutralFormula the neutralFormula to set
   */
  public void setNeutralFormula(String neutralFormula) {
    this.neutralFormula = neutralFormula;
  }

  /**
   * @return the smiles
   */
  public Set<String> getSmilesList() {
    return smilesList;
  }

  /**
   * @param smiles the smile to add
   */
  public void addSmiles(String smiles) {
    if (smiles != null)
      smilesList.add(smiles);
  }

  /**
   * @return the inchi
   */
  public String getInchi() {
    return inchi;
  }

  /**
   * @param inchi the inchi to set
   */
  public void setInchi(String inchi) {
    this.inchi = inchi;
  }

  /**
   * @return the inchikey
   */
  public String getInchikey() {
    return inchikey;
  }

  /**
   * @param inchikey the inchikey to set
   */
  public void setInchikey(String inchikey) {
    this.inchikey = inchikey;
  }

  /**
   * @return the charge
   */
  public int getCharge() {
    return charge;
  }

  /**
   * @param charge the charge to set
   */
  public void setCharge(int charge) {
    this.charge = charge;
  }

  /**
   * @return the validateCharge
   */
  public boolean validateCharge() {
    return validateCharge;
  }

  /**
   * @return the mapping
   */
  public int getMapping() {
    return mapping;
  }

  /**
   * @param mapping the mapping to set
   */
  public void setMapping(int mapping) {
    this.mapping = mapping;
  }

  /**
   * @return the compartment
   */
  public String getCompartment() {
    return compartment;
  }

  /**
   * @return the compartment abbrev
   */
  public String getCompartmentAbbrev() {
    return compartmentAbb;
  }

  /**
   * @return the exchangeCompound
   */
  public boolean isExchangeCompound() {
    return exchangeCompound;
  }

  /**
   * @param exchangeCompound the exchangeCompound to set
   */
  public void setExchangeCompound(boolean exchangeCompound) {
    this.exchangeCompound = exchangeCompound;
  }

  /**
   * @return the species
   */
  public Species getSpecies() {
    return species;
  }

  /**
   * @return the isPendingCurationXrefMap
   */
  public boolean isPendingCurationXrefMap() {
    return pendingCurationXrefMap;
  }

  /**
   * @param pending
   */
  public void setPendingCurationXrefMap(boolean pending) {
    this.pendingCurationXrefMap = pending;
  }

  /**
   * @return the isPendingCurationDbMap
   */
  public boolean isPendingCurationDbMap() {
    return pendingCurationDbMap;
  }

  /**
   * @param pending
   */
  public void setPendingCurationDbMap(boolean pending) {
    this.pendingCurationDbMap = pending;
  }

  /**
   * @return the manualCuration
   */
  public boolean isManualCuration() {
    return manualCuration;
  }

  /**
   * @param manualCuration the manualCuration to set
   */
  public void setManualCuration(boolean manualCuration) {
    this.manualCuration = manualCuration;
  }

  /**
   * @return the dorsal
   */
  public int getDorsal() {
    return dorsal;
  }

	public boolean isFBC() {
		return isFBC;
	}

  /**
   * @param dorsal the dorsal to set
   */
  public void setDorsal(int dorsal) {
    this.dorsal = dorsal;
  }

  /**
   * @return the structure
   */
  public String getStructure() {
    return structure;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (exchangeCompound) {
      sb.append("*");
    }
    sb.append(sbmlId).append(" - ").append(sbmlName).append( "[").append(ofn).append("]");
    if (!getKeggIdList().isEmpty()) {
      sb.append(" [kegg]");
    }
    return sb.toString();
  }
}
