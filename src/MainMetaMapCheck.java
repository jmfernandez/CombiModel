import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import es.csic.cnb.checker.ModelChecker;

public class MainMetaMapCheck {
  public static void main(String[] args) throws IOException {
    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("/home/pdsanchez/temp/model_error.txt")));

    ////////////////////////////////////////////////////////////////////////////
    // REPOSITORIO
    ////////////////////////////////////////////////////////////////////////////
    long t1 = System.currentTimeMillis();
    int total = 0;

    File dir = new File("/home/pdsanchez/Proyectos/cmodel/repository/seed/");

    for (File file : dir.listFiles()) {
      if (file.getName().endsWith(".xml")) {
        total++;

        long ts1 = System.currentTimeMillis();
        System.out.println();
        System.out.println("==================================");
        System.out.println("=== FILE " + total + ": " + file.getName());

        ModelChecker mdlCheck = new ModelChecker();
        boolean check = mdlCheck.isValid(file);
        if (!check) {
          System.err.println("REMOVE: " + file.getName());
          System.out.println(mdlCheck.getTraza());

          out.println("REMOVE: " + file.getName());
          out.println(mdlCheck.getTraza());
          out.flush();
        }

        long ts2 = System.currentTimeMillis();
        long ms = (ts2-ts1);
        System.out.println("--- TIEMPO: " + ms + " ms. [" + getTime(ms) + "]");
        System.out.println("----------------------------------");
      }
    }

    long t2 = System.currentTimeMillis();
    long ms = (t2-t1);
    System.out.println();
    System.out.println("### FICHEROS PROCESADOS: " + total);
    System.out.println("### TIEMPO TOTAL: " + ms + " ms. [" + getTime(ms) + "]");

    out.close();
  }

  private static String getTime(long ms) {
    long sec = ms/1000;
    long min = sec/60;
    long hr = min/60;

    String time = ms + "ms.";
    if (hr > 0) {
      //time = hr + "h.";
      long hours = min / 60;
      long minutes = min % 60;
      time = hours + "h. " + minutes + "min.";

    }
    else if (min > 0) {
      time = min + "min.";
    }
    else if (sec > 0) {
      time = sec + "sg.";
    }

    return time;
  }
}