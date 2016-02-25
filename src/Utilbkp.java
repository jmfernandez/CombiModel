

import java.util.Arrays;
import java.util.regex.Pattern;

public class Utilbkp {
  private static final Pattern PTT_SPLIT = Pattern.compile("(?=[A-Z][a-z]?\\d*)");
  
  public static int compareFormulas(String fm1, String fm2) {
    if (fm2.matches("^[A-Za-z0-9]+$")) {
      String[] fma1 = PTT_SPLIT.split(fm1);
      String[] fma2 = PTT_SPLIT.split(fm2);
      
      // Mapear
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

      // Devolver la diferencia
      return (l1 - totEq);
    }
    else {
      return 1;
    }
  }
  
  
  
  public static String[] getFormulaCorrected(String fm, int carga) {
    String[] array = PTT_SPLIT.split(fm);

    // Corregir H para igualar cargas a cero
    if (carga != 0) {
      int l1 = array.length;
      
      // Existe H
      if (fm.contains("H")) {
        for (int i=0; i<l1; i++) {
          if (array[i].startsWith("H")) {
            String val = array[i].substring(1);
            int newVal = (val.isEmpty()) ? 1 : Integer.valueOf(val);
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
    }
    
    return array;
  }
  
  
  public static int compareFormulas1(String fm1, int carga1, String fm2, int carga2) {
    // Si la formula contiene parentesis no hacemos map
    if (fm2.contains("(") || fm1.contains("(")) {
      return 1;
    }
    else {
      String[] array1 = PTT_SPLIT.split(fm1);
      String[] array2 = PTT_SPLIT.split(fm2);
      // Mapear
      return mapFormulas(array1, array2);

      //      // Si las cargas son iguales no corrigo H
      //      if (carga1 == carga2) {
      //        // Mapear
      //        return mapFormulas(array1, array2);
      //      }
      //      // Corregir H para igualar cargas a cero
      //      else if (carga1 != 0 && carga2 == 0) {
      //        int l1 = array1.length;
      //        
      //        // Existe H
      //        if (fm1.contains("H")) {
      //          for (int i=0; i<l1; i++) {
      //            if (array1[i].startsWith("H")) {
      //              String val = array1[i].substring(1);
      //              int newVal = (val.isEmpty()) ? 1 : Integer.valueOf(val);
      //              newVal -= carga1;
      //              array1[i] = "H" + newVal;
      //            }
      //          }
      //        }
      //        // No existe H
      //        else {
      //          String[] newArray = Arrays.copyOf(array1, l1 + 1);
      //          newArray[l1] = (Math.abs(carga1) == 1) ? "H" : "H" + Math.abs(carga1);
      //          array1 = newArray;
      //        }
      //        
      //        // Mapear
      //        return mapFormulas(array1, array2);
      //      }
      //      // Otros casos
      //      else {
      //        System.out.println("OTRO");
      //        return 1;
      //      }
    }
  }
  
  public static int compareFormulas(String fm1, int carga1, String fm2, int carga2) {
    // Si la formula contiene parentesis no hacemos map
    if (fm2.contains("(") || fm1.contains("(")) {
      return 1;
    }
    else {
      String[] array1 = PTT_SPLIT.split(fm1);
      String[] array2 = PTT_SPLIT.split(fm2);
      // Mapear
      return mapFormulas(array1, array2);

      //      // Si las cargas son iguales no corrigo H
      //      if (carga1 == carga2) {
      //        // Mapear
      //        return mapFormulas(array1, array2);
      //      }
      //      // Corregir H para igualar cargas a cero
      //      else if (carga1 != 0 && carga2 == 0) {
      //        int l1 = array1.length;
      //        
      //        // Existe H
      //        if (fm1.contains("H")) {
      //          for (int i=0; i<l1; i++) {
      //            if (array1[i].startsWith("H")) {
      //              String val = array1[i].substring(1);
      //              int newVal = (val.isEmpty()) ? 1 : Integer.valueOf(val);
      //              newVal -= carga1;
      //              array1[i] = "H" + newVal;
      //            }
      //          }
      //        }
      //        // No existe H
      //        else {
      //          String[] newArray = Arrays.copyOf(array1, l1 + 1);
      //          newArray[l1] = (Math.abs(carga1) == 1) ? "H" : "H" + Math.abs(carga1);
      //          array1 = newArray;
      //        }
      //        
      //        // Mapear
      //        return mapFormulas(array1, array2);
      //      }
      //      // Otros casos
      //      else {
      //        System.out.println("OTRO");
      //        return 1;
      //      }
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

    // Devolver la diferencia
    return (l1 - totEq);
  }
}
