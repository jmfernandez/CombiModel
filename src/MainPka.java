import java.io.File;
import java.util.HashMap;
import java.util.Map;

import es.csic.cnb.ModelManager;
import es.csic.cnb.db.DbManager;
import es.csic.cnb.log.LogMgr;
import es.csic.cnb.ws.ChebiException;


public class MainPka {

  /**
   * @param args
   */
  public static void main(String[] args) {
    // log
    LogMgr log = new LogMgr();
    log.configureLog();


    DbManager db = DbManager.INSTANCE;
    db.connect();


//    db.dropAllTables();
//    db.createAllTables(false);

//    db.dropCompTables();
//    db.createCompTables(false);

    ModelManager mdl = new ModelManager();

    long t1 = System.currentTimeMillis();
    System.out.println("==================================");

    //File fileIn = new File("/home/pdsanchez/Proyectos/cmodel/repository/bigg/Hpylori_iIT341.xml");
    //File fileIn = new File("/home/pdsanchez/Proyectos/cmodel/repository/test/Test1.xml");
    //File fileOut = new File("/home/pdsanchez/temp/", "npka_" + fileIn.getName());
    
//    File fileIn = new File("/home/pdsanchez/Descargas/models/err/model-8009249678813742106.xml");
//    File fileOut = new File("/home/pdsanchez/Descargas/output/n_model1.xml");
    
    File fileIn = new File("/home/pdsanchez/Descargas/models/err/model-5303566665259445472.xml");
    File fileOut = new File("/home/pdsanchez/Descargas/output/n_model2.xml");
    
//    File fileIn = new File("/home/pdsanchez/Descargas/models/prb/prb_model.xml");
//    File fileOut = new File("/home/pdsanchez/Descargas/models/prb/normal_prb_model.xml");

    try {
      Map<String,String> modelAtts = new HashMap<String,String>();
      //modelAtts.put("ph", "18.2");
      modelAtts.put("ph", "7.2");

      mdl.writeNormalizedModel(fileIn, fileOut, modelAtts);
      //mdl.writeManualCurationModel();
    }
    catch (ChebiException e) {
      e.printStackTrace();
    }

    long t2 = System.currentTimeMillis();
    long ms = (t2-t1);
    System.out.println("### TIEMPO TOTAL: " + ms + " ms. [" + getTime(ms) + "]");


//    try {
//      Thread.sleep(1000 * 60 * 5);
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    }

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
