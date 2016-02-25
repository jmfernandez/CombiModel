//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
//import java.io.IOException;
//
//import javax.xml.stream.XMLStreamException;
//
//import org.sbml.jsbml.ASTNode;
//import org.sbml.jsbml.Compartment;
//import org.sbml.jsbml.KineticLaw;
//import org.sbml.jsbml.LocalParameter;
//import org.sbml.jsbml.Model;
//import org.sbml.jsbml.Reaction;
//import org.sbml.jsbml.SBMLDocument;
//import org.sbml.jsbml.SBMLException;
//import org.sbml.jsbml.SBMLWriter;
//import org.sbml.jsbml.Species;
//import org.sbml.jsbml.SpeciesReference;
//import org.sbml.jsbml.Unit;
//import org.sbml.jsbml.UnitDefinition;
//
//
//public class CreateSBMLAmplModel1 {
//  private static final int LEVEL = 2;
//  private static final int VERSION = 1;
//
//  private SBMLDocument doc;
//  private Model model;
//
//  public CreateSBMLAmplModel1() {
//    doc = new SBMLDocument(LEVEL, VERSION);
//    model = doc.createModel("model1");
//    model.setName("");
//
//    UnitDefinition unitDefinition = new UnitDefinition("mmol_per_gDW_per_hr", LEVEL, VERSION);
//    unitDefinition.getListOfUnits().add(new Unit(1, -3, Unit.Kind.MOLE, 1, LEVEL, VERSION));
//    unitDefinition.getListOfUnits().add(new Unit(1, 0, Unit.Kind.GRAM, -1, LEVEL, VERSION));
//    unitDefinition.getListOfUnits().add(new Unit(0.000277777777777778, 0, Unit.Kind.SECOND, -1, LEVEL, VERSION));
//    model.addUnitDefinition(unitDefinition);
//
//    model.addCompartment(new Compartment("c", "Cytoplasm", LEVEL, VERSION));
//    model.addCompartment(new Compartment("e", "Extracellular", LEVEL, VERSION));
//    model.addCompartment(new Compartment("b", "Extracellular", LEVEL, VERSION));
//
//    parseFile("/home/pdsanchez/Proyectos/cmodel/paper_corrections2/parsed_ampliado/pmodel1.txt");
//  }
//
//  @SuppressWarnings("deprecation")
//  private void parseFile(String fpath) {
//    BufferedReader objReader = null;
//    try {
//      Reaction rx = null;
//      double lb = 0;
//      double ub = 0;
//      String line;
//
////      # CPD ID  ABBV  OFN  NAME  CAS CHEBI FM  INCHIKEY CHARGE  COMPARTMENT BOUNDARY
////      # CPD ID  ABBV  OFN NAME  COMPARTMENT BOUNDARY
////      # CPD ID  ABBV  OFN NAME  SYN FM  CH  COMPARTMENT BOUNDARY
//      objReader = new BufferedReader(new FileReader(fpath));
//      while ((line = objReader.readLine()) != null) {
//        if (line.startsWith("#")) continue;
//        String[] reg = line.split("\t");
//        if (reg[0].equals("CPD")) {
//          //createSp(reg[1], reg[3], reg[2], reg[6], reg[7], reg[8], reg[9], reg[10], reg[11]);
//          //createSp(reg[1], reg[3], reg[2], reg[5], reg[6]);
//          createSp(reg[1], reg[3], reg[2], reg[5], reg[6], reg[7], reg[8], reg[9]);
//        }
//        else if (reg[0].equals("RX")) {
//          rx = createRx(reg[1], reg[2], reg[7]);
//          //rx.appendNotes("Reaction: " + reg[3]);
//          //rx.appendNotes("E.C: " + reg[4]);
//
//          reg[5] = reg[5].replaceFirst(",", ".");
//          reg[6] = reg[6].replaceFirst(",", ".");
//          lb = Double.parseDouble(reg[5]);
//          ub = Double.parseDouble(reg[6]);
//        }
//        else if (reg[0].equals("RXr")) {
//          SpeciesReference specref = rx.createReactant();
//          specref.setSpecies(reg[1]);
//          //specref.setName(reg[2]);
//          specref.setStoichiometry(Double.parseDouble(reg[3]));
//        }
//        else if (reg[0].equals("RXp")) {
//          SpeciesReference specref = rx.createProduct();
//          specref.setSpecies(reg[1]);
//          //specref.setName(reg[2]);
//          specref.setStoichiometry(Double.parseDouble(reg[3]));
//        }
//        else if (reg[0].equals("EOR") && rx != null) {
//          KineticLaw kl = rx.createKineticLaw();
//          kl.setMath(new ASTNode("FLUX_VALUE"));
//
//          LocalParameter param = kl.createLocalParameter("LOWER_BOUND");
//          param.setValue(lb);
//          param.setUnits("mmol_per_gDW_per_hr");
//          param.setExplicitlyConstant(false);
//
//          param = kl.createLocalParameter("UPPER_BOUND");
//          param.setValue(ub);
//          param.setUnits("mmol_per_gDW_per_hr");
//          param.setExplicitlyConstant(false);
//
//          param = kl.createLocalParameter("FLUX_VALUE");
//          param.setValue(0);
//          param.setUnits("mmol_per_gDW_per_hr");
//          param.setExplicitlyConstant(false);
//
//          param = kl.createLocalParameter("OBJECTIVE_COEFFICIENT");
//          if (rx.getId().equals("BIOMASS1")) {
//            param.setValue(1);
//          }
//          else {
//            param.setValue(0);
//          }
//          param.setUnits("mmol_per_gDW_per_hr");
//          param.setExplicitlyConstant(false);
//        }
//      }
//    }
//    catch (IOException e) {
//      e.printStackTrace();
//    }
//    finally {
//      try {
//        if (objReader != null)
//          objReader.close();
//      } catch (IOException ex) {
//        ex.printStackTrace();
//      }
//    }
//  }
//
//  private Reaction createRx(String id, String name, String rev) {
//    id = id.replaceAll("-", "_");
//
//    Reaction rx = model.createReaction(id);
//    rx.setName(name);
//    rx.setReversible(Boolean.parseBoolean(rev));
//
//    return rx;
//  }
//
////  @SuppressWarnings("deprecation")
//  //private void createSp(String id, String ofn, String abbv, String chebi, String fm, String ikey, String charge, String cpt, String boundary) {
//  //private void createSp(String id, String ofn, String abbv, String cpt, String boundary) {
//  private void createSp(String id, String ofn, String abbv, String syn, String fm, String charge, String cpt, String boundary) {
//    Species sp = model.createSpecies(id);
//    sp.setCompartment(cpt);
//    if (ofn.isEmpty()) {
//      sp.setName(abbv);
//    }
//    else {
//      ofn = ofn.replaceAll("_", " ");
//      sp.setName(ofn);
//    }
//    sp.setBoundaryCondition(Boolean.parseBoolean(boundary));
//
//    if (!charge.equals("NULL")) {
//      sp.setCharge(Integer.parseInt(charge));
//    }
//    if (!fm.isEmpty()) sp.appendNotes("FORMULA: " + fm);
//
////    if (!chebi.isEmpty()) sp.appendNotes("chebi:"+chebi);
////    if (!ikey.isEmpty()) sp.appendNotes("INCHIKEY: " + ikey);
//
//    if (!abbv.isEmpty()) sp.appendNotes("SYN: " + abbv);
//
////    if (!syn.isEmpty() && syn.equalsIgnoreCase(abbv)) {
////      sp.appendNotes("SYN: " + abbv);
////    }
//  }
//
//  public void writeModel(File fout) throws SBMLException, XMLStreamException, IOException {
//    SBMLWriter.write(doc, fout, "CreateSBMLAmplModel1", "1.0");
//  }
//
//  /**
//   * @param args
//   * @throws IOException
//   * @throws XMLStreamException
//   * @throws SBMLException
//   */
//  public static void main(String[] args) throws SBMLException, XMLStreamException, IOException {
//    CreateSBMLAmplModel1 m1 = new CreateSBMLAmplModel1();
//
//    File fout = new File("/home/pdsanchez/Proyectos/cmodel/paper_corrections2/parsed_ampliado/model1.xml");
//    m1.writeModel(fout);
//  }
//
//}
