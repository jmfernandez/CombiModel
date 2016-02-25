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
//public class CreateSBMLAmplModel2 {
//  private static final int LEVEL = 2;
//  private static final int VERSION = 1;
//
//  private SBMLDocument doc;
//  private Model model;
//
//  public CreateSBMLAmplModel2() {
//    doc = new SBMLDocument(LEVEL, VERSION);
//    model = doc.createModel("model2");
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
//    parseFile("/home/pdsanchez/Proyectos/cmodel/paper_corrections2/parsed_ampliado/pmodel2.txt");
//  }
//
////  # COMPOUNDS
////  # CPD ID  ABBV  OFN FM  CHARGE  COMPARTMENT BOUNDARY
////  # RX  ID  REACTION  LB  UB  REVERSIBLE
////  # RXr ID  COMP  STEQIOMETRY
////  # RXp ID  COMP  STEQIOMETRY
////  # EOR
//
//  @SuppressWarnings("deprecation")
//  private void parseFile(String fpath) {
//    BufferedReader objReader = null;
//    try {
//      Reaction rx = null;
//      double lb = 0;
//      double ub = 0;
//      int idx = 0;
//      String line;
//
//      objReader = new BufferedReader(new FileReader(fpath));
//      while ((line = objReader.readLine()) != null) {
//        if (line.startsWith("#")) continue;
//        String[] reg = line.split("\t");
//        if (reg[0].equals("CPD")) {
//          createSp(reg[1], reg[2], reg[3], reg[4], reg[5], reg[6], reg[7]);
//        }
//        else if (reg[0].equals("RX")) {
//          idx++;
//          rx = createRx("RX"+idx, reg[1], reg[5]);
//          //rx.appendNotes("Reaction: " + reg[2]);
//
//          reg[3] = reg[3].replaceFirst(",", ".");
//          reg[4] = reg[4].replaceFirst(",", ".");
//          lb = Double.parseDouble(reg[3]);
//          ub = Double.parseDouble(reg[4]);
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
//  @SuppressWarnings("deprecation")
//  private void createSp(String id, String abb, String ofn, String fm, String charge, String cpt, String boundary) {
//    Species sp = model.createSpecies(id);
//    sp.setCompartment(cpt);
//    sp.setName(ofn);
//    sp.setCharge(Integer.parseInt(charge));
//    sp.setBoundaryCondition(Boolean.parseBoolean(boundary));
//
//    sp.appendNotes("FORMULA: " + fm);
//
//    if (!abb.isEmpty()) sp.appendNotes("SYN: " + abb);
//  }
//
//  public void writeModel(File fout) throws SBMLException, XMLStreamException, IOException {
//    SBMLWriter.write(doc, fout, "CreateSBMLAmplModel2", "1.0");
//  }
//
//  /**
//   * @param args
//   * @throws IOException
//   * @throws XMLStreamException
//   * @throws SBMLException
//   */
//  public static void main(String[] args) throws SBMLException, XMLStreamException, IOException {
//    CreateSBMLAmplModel2 m2 = new CreateSBMLAmplModel2();
//
//    File fout = new File("/home/pdsanchez/Proyectos/cmodel/paper_corrections2/parsed_ampliado/model2.xml");
//    m2.writeModel(fout);
//  }
//
//}
