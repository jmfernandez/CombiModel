import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import es.csic.cnb.ModelManager;
import es.csic.cnb.checker.ModelChecker;
import es.csic.cnb.db.DbManager;
import es.csic.cnb.log.LogMgr;
import es.csic.cnb.ws.ChebiException;


public class MainMetaMap {
  public static void main(String[] args) {
    //  log
    LogMgr log = new LogMgr();
    log.configureLog();


    DbManager db = DbManager.INSTANCE;
    db.connect();

    db.dropCompTables();
    db.createCompTables(false);


    ModelManager mdl = new ModelManager();

    //    long t1 = System.currentTimeMillis();
    //    System.out.println("==================================");
    //
    //    File fileIn = new File("/home/pdsanchez/Proyectos/cmodel/repository/bigg/Hpylori_iIT341.xml");
    //    //File fileIn = new File("/home/pdsanchez/Proyectos/cmodel/repository/test/Test1.xml");
    //    File fileOut = new File("/home/pdsanchez/temp/", "npka_" + fileIn.getName());
    //
    //    try {
    //      Map<String,String> modelAtts = new HashMap<String,String>();
    //      modelAtts.put("PH", "7.2");
    //
    //      mdl.writeNormalizedModel(fileIn, fileOut, modelAtts);
    //      //mdl.writeManualCurationModel();
    //    }
    //    catch (ChebiException e) {
    //      e.printStackTrace();
    //    }
    //
    //    long t2 = System.currentTimeMillis();
    //    long ms = (t2-t1);
    //    System.out.println("### TIEMPO TOTAL: " + ms + " ms. [" + getTime(ms) + "]");


    ////////////////////////////////////////////////////////////////////////////
    // REPOSITORIO
    ////////////////////////////////////////////////////////////////////////////
    long t1 = System.currentTimeMillis();
    int total = 0;

//    File fileIn = new File("/home/pdsanchez/Proyectos/cmodel/paper_corrections/parsed/model1.xml");
//    File fileOut = new File("/home/pdsanchez/Proyectos/cmodel/paper_corrections/parsed/n_model1.xml");

    //File dir = new File("/home/pdsanchez/Proyectos/cmodel/repository/bigg/");
    //File dir = new File("/home/pdsanchez/Proyectos/cmodel/repository/insilico/");
    //File dir = new File("/home/pdsanchez/Proyectos/cmodel/repository/seed/");
    File dir = new File("/home/pdsanchez/Proyectos/cmodel/repository/seed_err/models_to_check/");
    Map<String,String> modelAtts = new HashMap<String,String>();
    modelAtts.put("ph", "7.2");

    File[] flist = dir.listFiles();
    Arrays.sort(flist);
    for (File file : flist) {
      if (file.getName().endsWith(".xml")) {
        total++;

        long ts1 = System.currentTimeMillis();
        System.out.println();
        System.out.println("==================================");
        System.out.println("=== FILE " + total + ": " + file.getName());

        ModelChecker mdlCheck = new ModelChecker();
        boolean check = mdlCheck.isValid(file);
        if (check) {
          db.dropCompTables();
          db.createCompTables(false);

          try {
            mdl = new ModelManager();
            File fileOut = new File("/home/pdsanchez/temp/", "npka_" + file.getName());
            mdl.writeNormalizedModel(file, fileOut, modelAtts);
          }
          catch (ChebiException e) {
            e.printStackTrace();
          }
        }
        else {
          System.out.println(mdlCheck.getTraza());
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



    // Cerrar conexiones con la base de datos
    db.closeDbConections();
    // Cerrar el servidor de la base de datos
    db.shutdown();
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