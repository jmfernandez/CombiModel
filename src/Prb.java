import java.io.IOException;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.SBMLDocument;


public class Prb extends JFrame {

  private static final Pattern PTT_SPLIT = Pattern.compile("(?=[A-Z][a-z]?\\d*)");

  public Prb(SBMLDocument document) {
    super(document.getModel().getId());
    getContentPane().add(new JScrollPane(new JTree(document)));
    pack();
    setVisible(true);
    }

  /**
   * @param args
   * @throws IOException
   * @throws XMLStreamException
   * @throws SQLException
   * @throws ClassNotFoundException
   */
  public static void main(String[] args) {

 // Creates a DecimalFormat object
    DecimalFormat d = new DecimalFormat("#.##");
    // Get the positive suffix.
    System.out.println("The Rounding Mode is: " + d.getRoundingMode());
    System.out.println(d.format(0.386));

            d.setRoundingMode(RoundingMode.HALF_DOWN );
    System.out.println("The Rounding Mode is: "+d.getRoundingMode());

    System.out.println(d.format(0.386));

    System.exit(0);

//    //new Prb((new SBMLReader()).readSBML("/home/pdsanchez/Documentos/FBA/SBMLs/BIOMD0000000191_SBML-L2V4.xml"));
//    //new Prb((new SBMLReader()).readSBML("/home/pdsanchez/Documentos/FBA/SBMLs/iIT341.xml"));

    //String fm = "C3H5O6P";
    //String fm = "C10H12N5O3R";
    //String fm = "C25H40N7O19P3S";
    String fm1 = "C34H30N4O4Mg"; // -1
    String fm2 = "C34H30MgN4O4"; // -2
    //String fm1 = "C4H7N2O3"; // -1
    //String fm2 = "C4H8N2O3";
    //String fm1 = "C4H9N2O3"; // +1
    //String fm2 = "C4H8N2O3";
    //String fm1 = "CHO3"; // -1
    //String fm2 = "CH2O3";
    //String fm1 = "C2O3R"; // -1
    //String fm2 = "C2HO3R";

//    String[] array = fm.split("(?=[A-Z][a-z]?\\d*)");
//    System.out.println(Arrays.toString(array));

    compareFormulas(fm1, -1, fm2, -2);
  }

  private static int compareFormulas(String fm1, int carga1, String fm2, int carga2) {
    // Si la formula contiene parentesis no hacemos map
    if (fm2.contains("(") || fm1.contains("(")) {
      return 1;
    }
    else {
      String[] array1 = PTT_SPLIT.split(fm1);
      String[] array2 = PTT_SPLIT.split(fm2);

      // Si las cargas son iguales no corrigo H
      if (carga1 == carga2) {
        // Mapear
        return mapFormulas(array1, array2);
      }
      // Corregir H para igualar cargas a cero
      else if (carga1 != 0 && carga2 == 0) {
        int l1 = array1.length;

        // Existe H
        if (fm1.contains("H")) {
          for (int i=0; i<l1; i++) {
            if (array1[i].startsWith("H")) {
              String val = array1[i].substring(1);
              int newVal = (val.isEmpty()) ? 1 : Integer.valueOf(val);
              newVal -= carga1;
              array1[i] = "H" + newVal;
            }
          }
        }
        // No existe H
        else {
          String[] newArray = Arrays.copyOf(array1, l1 + 1);
          newArray[l1] = (Math.abs(carga1) == 1) ? "H" : "H" + Math.abs(carga1);
          array1 = newArray;
        }

        // Mapear
        return mapFormulas(array1, array2);
      }
      // Otros casos
      else {
        System.out.println("OTRO");
        return 1;
      }
    }
  }

  private static int mapFormulas(String[] fma1, String[] fma2) {
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

    System.out.println(Arrays.toString(fma1));
    System.out.println(Arrays.toString(fma2));
    System.out.println(":::DIFF "+(l1 - totEq));

    // Devolver la diferencia
    return (l1 - totEq);
  }

}
