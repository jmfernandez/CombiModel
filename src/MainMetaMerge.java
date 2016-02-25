import java.io.File;
import java.util.HashMap;
import java.util.Map;

import es.csic.cnb.ModelManager;
import es.csic.cnb.checker.ModelChecker;
import es.csic.cnb.db.DbManager;
import es.csic.cnb.log.LogMgr;
import es.csic.cnb.ws.ChebiException;


public class MainMetaMerge {
  public static void main(String[] args) {
    //  log
    LogMgr log = new LogMgr();
    log.configureLog();


    DbManager db = DbManager.INSTANCE;
    db.connect();

//    db.dropCompTables();
//    db.createCompTables(false);


    ModelManager mdl = new ModelManager();

    long t1 = System.currentTimeMillis();
    System.out.println("==================================");

    //File fileIn = new File("/home/pdsanchez/Proyectos/cmodel/repository/bigg/Hpylori_iIT341.xml");
    //File fileIn = new File("/home/pdsanchez/Proyectos/cmodel/repository/bigg/Saureus_iSB619.xml");
    File fileIn = new File("/home/pdsanchez/Proyectos/cmodel/repository/seed/iJR904.xml");
    //File fileIn = new File("/home/pdsanchez/Proyectos/cmodel/repository/seed/Seed272634.1.149.xml");
    File fileOut = new File("/home/pdsanchez/Proyectos/cmodel/paper_corrections4/models/", "n_" + fileIn.getName());

    //File fileIn = new File("/home/pdsanchez/Proyectos/cmodel/paper_corrections2/parsed_ampliado/model1.xml");
    //File fileOut = new File("/home/pdsanchez/Proyectos/cmodel/paper_corrections2/parsed_ampliado/normal/", "n_" + fileIn.getName());
    //File fileIn = new File("/home/pdsanchez/Proyectos/cmodel/paper_corrections/parsed_ampliado/model1.xml");
    //File fileOut = new File("/home/pdsanchez/Proyectos/cmodel/paper_corrections/parsed_ampliado/normal/", "n_" + fileIn.getName());

    try {
      Map<String,String> modelAtts = new HashMap<String,String>();
      modelAtts.put("ph", "7.2");

      ModelChecker mdlCheck = new ModelChecker();
      boolean check = mdlCheck.isValid(fileIn);
      if (check) {
        mdl.writeNormalizedModel(fileIn, fileOut, modelAtts);
        mdl.writeManualCurationModel();
      }
      else {
        System.out.println(mdlCheck.getTraza());
      }

    }
    catch (ChebiException e) {
      e.printStackTrace();
    }

    long t2 = System.currentTimeMillis();
    long ms = (t2-t1);
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