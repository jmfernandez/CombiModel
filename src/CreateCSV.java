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


public class CreateCSV {
  //private final static String SBML_FILE = "/home/pdsanchez/Proyectos/cmodel/repository/bigg/Hpylori_iIT341.xml";
  private final static String SBML_FILE = "/home/pdsanchez/Proyectos/cmodel/repository/bigg/Saureus_iSB619.xml";

  //private final static String CSV_FILE = "/home/pdsanchez/Proyectos/cmodel/paper_corrections3/models/Hpylori.csv";
  private final static String CSV_FILE = "/home/pdsanchez/Proyectos/cmodel/paper_corrections3/models/Saureus.csv";

  private Matcher mtFormula = Pattern.compile("FORMULA:\\s?([A-Za-z0-9]+)").matcher("");
  private Matcher mtGene = Pattern.compile("GENE ASSOCIATION:\\s?\\((.+)\\)").matcher("");
  private Matcher mtProt = Pattern.compile("NAME:.+#ABBREVIATION:\\s?(.+)#").matcher("");

  private SBMLDocument doc;
  private Model model;

  @SuppressWarnings("deprecation")
  public CreateCSV() {
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

        sbCpd.append(sp.getId()).append(Util.TAB);
        sbCpd.append(sp.getName()).append(Util.TAB);
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

  /**
   * @param args
   * @throws IOException
   * @throws XMLStreamException
   * @throws SBMLException
   */
  public static void main(String[] args) throws SBMLException, XMLStreamException, IOException {
    CreateCSV m1 = new CreateCSV();
  }

}
