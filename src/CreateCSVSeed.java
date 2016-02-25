import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;

import es.csic.cnb.util.Util;


public class CreateCSVSeed {
  //private final static String SBML_FILE = "/home/pdsanchez/Proyectos/cmodel/repository/seed/iJR904.xml";
  private final static String SBML_FILE = "/home/pdsanchez/Proyectos/cmodel/repository/seed/Seed272634.1.149.xml";

  //private final static String CSV_FILE = "/home/pdsanchez/Proyectos/cmodel/paper_corrections4/models/iJR904.csv";
  private final static String CSV_FILE = "/home/pdsanchez/Proyectos/cmodel/paper_corrections4/models/Seed272634.1.149.csv";

  private Matcher mtNameFormula = Pattern.compile("^(.+)_((?:[A-Z][a-z]?\\d*)+)$").matcher("");
  private Matcher mtFormula = Pattern.compile("FORMULA:\\s?([A-Za-z0-9]+)").matcher("");
  private Matcher mtGene = Pattern.compile("GENE_ASSOCIATION:\\s?(.+)</html:p>").matcher("");
  private Matcher mtProt = Pattern.compile("PROTEIN_CLASS:\\s?(.+)</html:p>").matcher("");

  private SBMLDocument doc;
  private Model model;

  @SuppressWarnings("deprecation")
  public CreateCSVSeed() {
    try {
      doc = SBMLReader.read(new File(SBML_FILE));
      model = doc.getModel();

      StringBuilder sbCpd = new StringBuilder();
      sbCpd.append("ABBREVIATION").append(Util.TAB);
      sbCpd.append("NAME").append(Util.TAB);
      sbCpd.append("FORMULA").append(Util.TAB);
      sbCpd.append("CHARGE").append(Util.TAB);
      sbCpd.append("COMPARTMENT").append(Util.TAB);
      sbCpd.append("BOUNDARY").append(Util.NEWLINE);
      for (Species sp : model.getListOfSpecies()) {
        String fm = "";
        mtFormula.reset(sp.getNotesString());
        if (mtFormula.find()) {
          fm = mtFormula.group(1);
        }

        String[] str = cleanNameWithFm(sp.getName());
        String name = str[0];
        fm = str[1];

        sbCpd.append(sp.getId()).append(Util.TAB);
        sbCpd.append(name).append(Util.TAB);
        sbCpd.append(fm).append(Util.TAB);
        sbCpd.append(sp.getCharge()).append(Util.TAB);
        sbCpd.append(sp.getCompartment()).append(Util.TAB);
        sbCpd.append(sp.getBoundaryCondition()).append(Util.NEWLINE);
      }

      StringBuilder sbReaction = new StringBuilder();
      sbReaction.append("ABBREVIATION").append(Util.TAB);
      sbReaction.append("NAME").append(Util.TAB);
      sbReaction.append("EQUATION").append(Util.TAB);
      sbReaction.append("LOWER BOUND").append(Util.TAB);
      sbReaction.append("UPPER BOUND").append(Util.TAB);
      sbReaction.append("GENE").append(Util.TAB);
      sbReaction.append("PROTEIN").append(Util.NEWLINE);
      for (Reaction rx : model.getListOfReactions()) {
        if (!rx.getListOfReactants().isEmpty() && !rx.getListOfProducts().isEmpty()) {
          StringBuilder sbRx = new StringBuilder();
          for (SpeciesReference spr : rx.getListOfReactants()) {
            if (spr.getStoichiometry() != 1) {
              sbRx.append("(").append(spr.getStoichiometry()).append(") ").append(spr.getSpecies()).append(" + ");
            }
            else {
              sbRx.append(spr.getSpecies()).append(" + ");
            }
          }
          sbRx.delete(sbRx.lastIndexOf(" + "), sbRx.length());
          String rev = (rx.isReversible()) ? " <==> " : " --> ";
          sbRx.append(rev);
          // Recuperar productos
          for (SpeciesReference spr : rx.getListOfProducts()) {
            if (spr.getStoichiometry() != 1) {
              sbRx.append("(").append(spr.getStoichiometry()).append(") ").append(spr.getSpecies()).append(" + ");
            }
            else {
              sbRx.append(spr.getSpecies()).append(" + ");
            }
          }
          sbRx.delete(sbRx.lastIndexOf(" + "), sbRx.length());

          double lb = rx.getKineticLaw().getLocalParameter("LOWER_BOUND").getValue();
          double ub = rx.getKineticLaw().getLocalParameter("UPPER_BOUND").getValue();

          String gene = "";
          mtGene.reset(rx.getNotesString());
          if (mtGene.find()) {
            gene = mtGene.group(1);
            gene = gene.replaceAll("[\\(\\)]", "");
            //gene = gene.replaceAll(" and ", ", ");
          }

          String prot = "";
          mtProt.reset(rx.getNotesString());
          if (mtProt.find()) {
            prot = mtProt.group(1);
          }

          sbReaction.append(rx.getId()).append(Util.TAB);
          sbReaction.append(rx.getName()).append(Util.TAB);
          sbReaction.append(sbRx).append(Util.TAB);
          sbReaction.append(lb).append(Util.TAB);
          sbReaction.append(ub).append(Util.TAB);
          sbReaction.append(gene).append(Util.TAB);
          sbReaction.append(prot).append(Util.NEWLINE);
        }
      }

      BufferedWriter wr = new BufferedWriter(new FileWriter(CSV_FILE));
      wr.append(sbReaction);
      wr.newLine();
      wr.append(sbCpd);
      wr.flush();
      wr.close();

    } catch (XMLStreamException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String[] cleanNameWithFm(String str) {
    if (str.length() == 0)
      return null;

    String fm = "";
    if (str.contains("_")) {
      // Recuperar formula
      mtNameFormula.reset(str);
      if (mtNameFormula.find()) {
        str = mtNameFormula.group(1);
        fm = mtNameFormula.group(2);
      }

      // Quitar la 'M' inicial si existe
      str = str.replaceFirst("^[Mm]_+", "");

      // Sustituir guiones bajos por blanco
      str = str.replaceAll("_+", " ");
    }

    return new String[]{str, fm};
  }


  /**
   * @param args
   * @throws IOException
   * @throws XMLStreamException
   * @throws SBMLException
   */
  public static void main(String[] args) throws SBMLException, XMLStreamException, IOException {
    CreateCSVSeed m1 = new CreateCSVSeed();
  }

}
