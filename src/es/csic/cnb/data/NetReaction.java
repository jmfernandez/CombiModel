package es.csic.cnb.data;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SpeciesReference;

import es.csic.cnb.db.CompoundDbManager;
import es.csic.cnb.db.DbManager;
import es.csic.cnb.util.Util;

public class NetReaction {
  private Matcher mtKegg = Pattern.compile("kegg\\.reaction:(R\\d+)$").matcher("");
  private Matcher mtEc = Pattern.compile("ec-code:([\\d\\.]+)$").matcher("");
  private Matcher mtGo = Pattern.compile("obo\\.go:GO%3A(\\d+)$").matcher("");

  private Matcher mtKeggNt = Pattern.compile("KEGG_RID:\\s?(R\\d+)").matcher("");
  private Matcher mtEcNt = Pattern.compile("(?:PROTEIN_CLASS|EC Number):\\s?([\\d\\.]+)").matcher("");

  private int rxId;

  private String sbmlId;
  private String sbmlName;

  private boolean reversible;

  private boolean exchangeReaction = false;

  private Map<Integer,Double> reactantList;
  private Map<Integer,Double> productList;
  private Map<String,String> xrefList;

  // Parameters
  private double lb;
  private String lbUnits;
  private double ub;
  private String ubUnits;
  private double oc;
  private String ocUnits;
  private double fv;
  private String fvUnits;

  private CompoundDbManager db;

//  private Reaction reaction;


  public NetReaction(Reaction reaction,boolean isFBC) throws SQLException, XMLStreamException {
    db = DbManager.INSTANCE.getCompoundDbMgr();

    this.sbmlId = reaction.getId();
    this.sbmlName = reaction.getName();
    this.reversible = reaction.getReversible();

    this.reactantList = new HashMap<Integer,Double>();
    this.productList = new HashMap<Integer,Double>();
    this.xrefList = new HashMap<String,String>();

//    this.reaction = reaction;

    // Ver si es reaccion de intercambio
    this.exchangeReaction = Util.isExchangeReaction(reaction);
    // Eliminar del xml la referencia que indica que es reaccion de intercambio
    Util.removeExchangeResource(reaction);

    // Buscar algunos identificadores externos
    for (CVTerm cv : reaction.getAnnotation().getListOfCVTerms()) {
      for (String str : cv.getResources()) {
        mtKegg.reset(str);
        if (mtKegg.find()) {
          String keggId = mtKegg.group(1);
          this.addKeggId(keggId);
        }
        mtEc.reset(str);
        if (mtEc.find()) {
          String ec = mtEc.group(1);
          this.addEc(ec);
        }
        mtGo.reset(str);
        if (mtGo.find()) {
          this.xrefList.put(Util.SOURCE_GO, ("GO:"+mtGo.group(1)));
        }
      }
    }
    // Guardar en la lista de xrefs el id del compuesto en el sbml
    this.addXref(Util.SOURCE_SBML, sbmlId);

    // Recuperar algunos identificadores externos que aparecen en las notas del SBML
    String notes = reaction.getNotesString();
    mtKeggNt.reset(notes);
    if (mtKeggNt.find()) {
      String keggId = mtKeggNt.group(1);
      this.addKeggId(keggId);
    }
    mtEcNt.reset(notes);
    if (mtEcNt.find()) {
      String ec = mtEcNt.group(1);
      this.addEc(ec);
    }


    // Guardar compuestos
    for (SpeciesReference spr : reaction.getListOfReactants()) {
      for (int cpd : db.getChemIdForSbmlId(Util.cleanId(spr.getSpecies()))) {
        reactantList.put(cpd, spr.getStoichiometry());
      }
    }

    for (SpeciesReference spr : reaction.getListOfProducts()) {
      for (int cpd : db.getChemIdForSbmlId(Util.cleanId(spr.getSpecies()))) {
        productList.put(cpd, spr.getStoichiometry());
      }
    }

    //    // Modifiers
    //    for (ModifierSpeciesReference spr : reaction.getListOfModifiers()) {
    //      System.out.println(spr.getSpecies());
    //      if (spr.getSpeciesInstance() != null) {
    //        System.out.println("getSpeciesInstance NOT NULL");
//        System.out.println(spr.getSpeciesInstance().getId() + ": " + spr.getSpeciesInstance().getName());
//      }
//      System.out.println("Modifiers: "+spr.getSpecies());
//    }

	if(isFBC) {
		// Using FBC extension, as it is available
		FBCReactionPlugin fbcReaction = (FBCReactionPlugin)reaction.getPlugin(FBCConstants.shortLabel);
		
		if(fbcReaction.isSetLowerFluxBound()) {
			Parameter lbParam = fbcReaction.getLowerFluxBoundInstance();
			lb = lbParam.getValue();
			lbUnits = lbParam.getUnits();
		}
		
		if(fbcReaction.isSetUpperFluxBound()) {
			Parameter ubParam = fbcReaction.getUpperFluxBoundInstance();
			ub = ubParam.getValue();
			ubUnits = ubParam.getUnits();
		}
		
		// No kinetic law is needed in these cases
	} else {
		KineticLaw kl = Util.getKineticLaw(reaction);
		if(kl != null) {
		    // Parametros de la reaccion
		    for (LocalParameter lp : kl.getListOfLocalParameters()) {
		      String key = lp.getId();
		      if (key.equalsIgnoreCase(Util.LOCAL__LOWER_BOUND_PARAM)) {
			lb = lp.getValue();
			lbUnits = lp.getUnits();
		      }
		      else if (key.equalsIgnoreCase(Util.LOCAL__UPPER_BOUND_PARAM)) {
			ub = lp.getValue();
			ubUnits = lp.getUnits();
		      }
		      else if (key.equalsIgnoreCase(Util.LOCAL__OBJECTIVE_COEFFICIENT_PARAM)) {
			oc = lp.getValue();
			ocUnits = lp.getUnits();
		      }
		      else if (key.equalsIgnoreCase(Util.LOCAL__FLUX_VALUE_PARAM)) {
			fv = lp.getValue();
			fvUnits = lp.getUnits();
		      }
		    }
		}
	}
  }

  /**
   * @return the lb
   */
  public double getLb() {
    return lb;
  }

  /**
   * @param lb the lb to set
   */
  public void setLb(double lb) {
    this.lb = lb;
  }

  /**
   * @return the lbUnits
   */
  public String getLbUnits() {
    return lbUnits;
  }

  /**
   * @param lbUnits the lbUnits to set
   */
  public void setLbUnits(String lbUnits) {
    this.lbUnits = lbUnits;
  }

  /**
   * @return the ub
   */
  public double getUb() {
    return ub;
  }

  /**
   * @param ub the ub to set
   */
  public void setUb(double ub) {
    this.ub = ub;
  }

  /**
   * @return the ubUnits
   */
  public String getUbUnits() {
    return ubUnits;
  }

  /**
   * @param ubUnits the ubUnits to set
   */
  public void setUbUnits(String ubUnits) {
    this.ubUnits = ubUnits;
  }

  /**
   * @return the oc
   */
  public double getOc() {
    return oc;
  }

  /**
   * @param oc the oc to set
   */
  public void setOc(double oc) {
    this.oc = oc;
  }

  /**
   * @return the ocUnits
   */
  public String getOcUnits() {
    return ocUnits;
  }

  /**
   * @param ocUnits the ocUnits to set
   */
  public void setOcUnits(String ocUnits) {
    this.ocUnits = ocUnits;
  }

  /**
   * @return the fv
   */
  public double getFv() {
    return fv;
  }

  /**
   * @param fv the fv to set
   */
  public void setFv(double fv) {
    this.fv = fv;
  }

  /**
   * @return the fvUnits
   */
  public String getFvUnits() {
    return fvUnits;
  }

  /**
   * @param fvUnits the fvUnits to set
   */
  public void setFvUnits(String fvUnits) {
    this.fvUnits = fvUnits;
  }

  /**
   * @return the rxId
   */
  public int getRxId() {
    return rxId;
  }

  /**
   * @param rxId the rxId to set
   */
  public void setRxId(int rxId) {
    this.rxId = rxId;
  }

  /**
   * @return the id
   */
  public String getSbmlId() {
    return sbmlId;
  }

  /**
   * @return the name
   */
  public String getSbmlName() {
    return sbmlName;
  }

  /**
   * @return the reversible
   */
  public boolean isReversible() {
    return reversible;
  }

  /**
   * @return the exchangeReaction
   */
  public boolean isExchangeReaction() {
    return exchangeReaction;
  }

  /**
   * @param exchangeReaction the exchangeReaction to set
   */
  public void setExchangeReaction(boolean exchangeReaction) {
    this.exchangeReaction = exchangeReaction;
  }

  /**
   * @return the reactantList
   */
  public Map<Integer, Double> getReactantList() {
    return reactantList;
  }

  /**
   * @return the productList
   */
  public Map<Integer, Double> getProductList() {
    return productList;
  }

  /**
   * @return the keggId
   */
  public String getKeggId() {
    return xrefList.get(Util.SOURCE_KEGG);
  }

  /**
   * @param keggId the keggId to add
   */
  public void addKeggId(String keggId) {
    xrefList.put(Util.SOURCE_KEGG, keggId);
  }

  /**
   * @return the ec
   */
  public String getEc() {
    return xrefList.get(Util.SOURCE_EC);
  }

  /**
   * @param ec the ec to add
   */
  public void addEc(String ec) {
    xrefList.put(Util.SOURCE_EC, ec);
  }

  public void addXref(String source, String xref) {
    if (xref != null && !xref.isEmpty()) {
      xrefList.put(source, xref);
    }
  }

  public Map<String,String> getXrefList() {
    return xrefList;
  }

  public String getXref(String source) {
    return xrefList.get(source);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (exchangeReaction) {
      sb.append("*");
    }
    sb.append(sbmlId).append(": ").append(sbmlName);
    sb.append(" --> ").append(this.getKeggId());
    return sb.toString();
  }
}
