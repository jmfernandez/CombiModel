import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PrbCleanName {
  private Matcher mtNameFormula = Pattern.compile("^(.+)_([A-Za-z0-9]+)$").matcher("");
  private Matcher mtNum = Pattern.compile("[a-z]\\d+").matcher("");
  private Matcher mtP = Pattern.compile("l(?:phosphate|diphosphate)").matcher("");

  /**
   * @param args
   */
  public static void main(String[] args) {
    String str = "Isotetradecanoylcardiolipin_(B. subtilis)_C65H124O17P2";

    PrbCleanName prb = new PrbCleanName();
    str = prb.cleanName(str).trim();

    System.out.println(str);
  }

  private String cleanName(String str) {
    if (str.length() == 0)
      return null;

    System.out.println("1: "+str);

    if (str.contains("_")) {
      // Recuperar formula
      mtNameFormula.reset(str);
      if (mtNameFormula.find()) {
        str = mtNameFormula.group(1);
        mtNameFormula.group(2);
      }

      // Quitar la 'M' inicial si existe
      str = str.replaceFirst("^[Mm]_+", "");

      System.out.println("2: "+str);

      // Sustituir guiones bajos por blanco
      str = str.replaceAll("_+", " ");

      System.out.println("3: "+str);


    }
//    if (str.contains("_")) {
//      // Recuperar formula
//      mtNameFormula.reset(str);
//      if (mtNameFormula.find()) {
//        str = mtNameFormula.group(1);
//        setFormula(mtNameFormula.group(2));
//        this.xrefList.put(FORMULA, getFormula());
//      }
//      // Quitar la 'M' inicial si existe
//      str = str.replaceFirst("^[Mm]_+", "");
//      // Sustituir guiones bajos por guion normal
//      str = str.replaceAll("_+", "-");
//
//      // Corregir guiones entre numeros: 7-8 -> 7,8
//      boolean found = mtGuion.reset(str).find();
//      if (found) {
//        StringBuilder sb = new StringBuilder(str);
//        while (found) {
//          sb.setCharAt(mtGuion.start(1), ',');
//          found = mtGuion.find(mtGuion.end(1));
//        }
//        str = sb.toString();
//      }
//    }

    str = str.replaceAll(" Ecoli", "");
    str = str.replaceAll("-E-?coli-", "");
    str = str.replaceAll("-?\\(B. subtilis\\)", "");

    System.out.println("4: "+str);

    // Anadir blanco cuando no existe entre letra y numero (Ej: D-Ribulose5-phosphate)
    boolean found = mtNum.reset(str).find();
    if (found) {
      StringBuilder sb = new StringBuilder(str);
      sb.insert(mtNum.start()+1, ' ');
      str = sb.toString();
    }

    System.out.println("5: "+str);

    // Separar phosphate (Ej: Geranyldiphosphate --> Geranyl diphosphate)
    found = mtP.reset(str).find();
    if (found) {
      StringBuilder sb = new StringBuilder(str);
      sb.insert(mtP.start()+1, ' ');
      str = sb.toString();
    }

    System.out.println("6: "+str);

    return str;
  }
}
