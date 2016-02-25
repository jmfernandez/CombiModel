import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PrbFm {

  private static final Pattern PTT_SPLIT = Pattern.compile("(?=[A-Z][a-z]?\\d*)");
  private static Matcher mtFmError = Pattern.compile("^[R|X]$").matcher("");

  private static Matcher mtFmC = Pattern.compile("C(\\d+)").matcher("");

  private static Pattern p = Pattern.compile("\\((.*?)\\)n");

  private static Matcher mtHs = Pattern.compile("(H(\\d*)(?=[A-Z]|$))").matcher("");

  /**
   * @param args
   */
  public static void main(String[] args) {
    //String[] list = {"C43H71O6P1", "C43H0O6P1", "P1C43O6H7"};
    String[] list = {"C14H20O4(C5H8)n", "NH4.OH","HO4P","Hg","HgNO4H2", "R", "K", "H", "CO2", "C2H6"};

    for (String fm : list) {
      fm = getFormulaCorrected(fm);
      quitaHs(fm);

//      System.out.println();
//      System.out.println("FM:  "+fm);
//      fm = getFormulaCorrected(fm);
//      //fm = getFormulaNeutra("H2", 10000000);
//      System.out.println("NUM CS: "+getNumCs(fm));
//      System.out.println("FMC: "+fm);
    }
  }

  public static void quitaHs(String fm) {
    System.out.println();
    System.out.println(fm);

    if (mtHs.reset(fm).find()) {
      System.out.println(mtHs.group(1));
      System.out.println(mtHs.group(2));

      String sbmlFormulaSinH = mtHs.reset(fm).replaceAll("");
      System.out.println(fm + " --> " + sbmlFormulaSinH);
    }


  }


  public static int getNumCs(String fm) {
    System.out.println("------------------ "+fm);
    if (mtFmC.reset(fm).find()) {
      System.out.println("FM: "+fm+" --> "+mtFmC.group(1));
      return Integer.parseInt(mtFmC.group(1));
    }

    return 0;
  }

  public static String getFormulaCorrected(String fm) {
//    if (mtFmError.reset(fm).find()) {
//      return null;
//    }

    Matcher matcher = p.matcher(fm);
    while(matcher.find())
    {
        //System.out.println("found match:"+matcher.group(1));
        //System.out.println("substring:"+fm.substring(matcher.start(1), matcher.end(1)));
        //System.out.println();
    }


    StringBuilder sb = new StringBuilder();

    String[] array = PTT_SPLIT.split(fm);

    // Ordenar elementos
    Arrays.sort(array);

    //System.out.println(Arrays.toString(array));

    int l1 = array.length;
    for (int i=0; i<l1; i++) {
      String tk = array[i];
      if (tk.isEmpty()) continue;

      // Eliminar valor 0 o 1 asociado a un elemento (ej: C43H71O6P1 -> C43H71O6P)
      if (tk.matches("^[A-Za-z]+[01]$")) {
        tk = tk.substring(0, tk.length()-1);
      }

      sb.append(tk);
    }

    return sb.toString();
  }

  public static String getFormulaNeutra(String fm, int carga) {
    String[] array = PTT_SPLIT.split(fm);

    if (carga == 0 || carga > 100) {
      return fm;
    }
    // Corregir H para igualar cargas a cero
    else {
      int l1 = array.length;

      // Existe H
      if (fm.contains("H")) {
        for (int i=0; i<l1; i++) {
          if (array[i].isEmpty()) continue;
          System.out.println(array[i]);

          //if (array[i].startsWith("H")) {
          if (array[i].matches("^H[^a-z]?\\d*$")) {
            String val = array[i].substring(1);
            System.out.println(" - "+val);
            int newVal = (val.isEmpty()) ? 1 : Integer.valueOf(val);
            System.out.println(" - "+newVal);
            newVal -= carga;
            array[i] = "H" + newVal;
          }
        }
      }
      // No existe H
      else {
        String[] newArray = Arrays.copyOf(array, l1 + 1);
        newArray[l1] = (Math.abs(carga) == 1) ? "H" : "H" + Math.abs(carga);
        array = newArray;
      }

      // Ordenar elementos
      Arrays.sort(array);

      // Concatenar
      StringBuilder sb = new StringBuilder();
      for (String s : array) {
        // Eliminar valor 0 o 1 asociado a un elemento (ej: C43H71O6P1 -> C43H71O6P)
        if (s.matches("^[A-Za-z]+[01]$")) {
          s = s.substring(0, s.length()-1);
        }
        sb.append(s);
      }

      return sb.toString();
    }
  }

}
