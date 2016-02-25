import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;



public class PrbModifSbml {

  /**
   * @param args
   * @throws IOException
   * @throws XMLStreamException
   */
  public static void main(String[] args) throws XMLStreamException, IOException {
    SBMLReader reader = new SBMLReader();
    SBMLWriter wr = new SBMLWriter();

    SBMLDocument doc = reader.readSBML(new File("/home/pdsanchez/Documentos/FBA/SBMLs/BiGG/Hpylori_iIT341.xml"));

    Map<String, String> atts = new HashMap<String, String>();
    atts.put("ph", "7.2");
    atts.put("coef", "1");
    doc.setSBMLDocumentAttributes(atts);

//    Model model = doc.getModel();

    wr.write(doc, new File("/home/pdsanchez/temp/n_Hpylori_iIT341.xml"));
  }

}
