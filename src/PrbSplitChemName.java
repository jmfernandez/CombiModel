import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.csic.cnb.util.DiffMatchPatch;


public class PrbSplitChemName {
  private static final Matcher mt = Pattern.compile("[a-z](\\d+)").matcher("");

  /**
   * @param args
   */
  public static void main(String[] args) {
    final DiffMatchPatch diff = new DiffMatchPatch();

    String[] chems = {
            "4-Hydroxy-2-oxoglutarate",
            "2-oxo-4-hydroxyglutarate",
            "3-methyl-6-methoxy-2-octaprenyl-1,4-benzoquinone",
            "2-Octaprenyl-3-methyl-6-methoxy-1,4-benzoquinone",
//            "L-2-Amino-3-oxobutanoate",
//            "Lysophosphatidylethanolamine(18:1)",
//            "Anteisoheptadecanoyllipoteichoic acid (n=24), linked, unsubstituted",
//            "heptosyl-(KDO)2-lipid_A",
//            "6-phosphonato-2-dehydro-3-deoxy-D-galactate(3-)",
//            "2-dehydro-3-deoxy-D-galactonate 6-phosphate",
//            "2-C-methyl-D-erythritol2-4-cyclodi3phosphate(3-)",
//            "3-deoxy-6-O-phosphonato-D-threo-hex-2-ulosonate",
//            "6-phosphonato-2-dehydro-3-deoxy-D-galactate trianion",
//            "6-phosphonato-2-dehydro-(3)-deoxy-D-galactate",
//            "2,5-diamino-6-(1-D-ribosylamino)pyrimidin-4(3H)-one 5'-phosphate(2-)",
//            "2 5 Diamino 61 ribosylamino 4 3H pyrimidinone 5 phosphate"
    };

    for (String chem : chems) {
      System.out.println(getChemicalName(splitChemicalName(chem)));
    }

    String s1 = "2-oxo-3-phenyl propanoate".toLowerCase();
    String s2 = "3-phenyl-2-oxo propanoate".toLowerCase();
    System.out.println("ML: "+diff.diff_levenshtein(diff.diff_main(s1, s2)));
    System.out.println("MLS: "+diff.diff_levenshtein(diff.diff_main(
            getChemicalName(splitChemicalName(s1)),
                    getChemicalName(splitChemicalName(s2)))));
  }

  public static List<String> splitChemicalName(String chem) {
    System.out.println();
    System.out.println(chem);

    boolean found = mt.reset(chem).find();
    while (found) {
      int start = mt.start(1);
      System.out.println("> ST: "+start);
      System.out.println("> "+chem.substring(0, start));
      System.out.println("> "+chem.substring(start));
      StringBuilder sb = new StringBuilder();
      sb.append(chem.substring(0, start)).append(" ").append(chem.substring(start));

      chem = sb.toString();

      System.out.println("> "+chem);

      found = mt.reset(chem).find(start);
    }


    chem = chem.replaceAll("\\s?\\(\\d*[-\\+]\\)$", ""); // quitar cargas al final del string
    chem = chem.replaceAll("[^A-Za-z0-9:=]", " ");
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

    System.out.println(list);

    Collections.sort(list);

    System.out.println(list);

    return list;
  }

  public static String getChemicalName(List<String> splitChemName) {
    StringBuilder sb = new StringBuilder();
    for (String s : splitChemName) {
      sb.append(s);
    }
    return sb.toString();
  }


}
