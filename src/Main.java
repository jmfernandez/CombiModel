import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import es.csic.cnb.ModelManager;
import es.csic.cnb.ModelParsing;
import es.csic.cnb.db.DbManager;
import es.csic.cnb.log.LogMgr;
import es.csic.cnb.ws.ChebiException;


public class Main {
  /**
   * @param args
 * @throws InterruptedException 
 * @throws SQLException 
   * @throws IOException
   * @throws SecurityException
   */
  public static void main(String[] args) throws InterruptedException, SQLException {
    //  log
    LogMgr log = new LogMgr();
    log.configureLog();



    DbManager db = DbManager.INSTANCE;
    db.connect();
    
//    //BKP
//    db.getCompoundDbMgr().scriptTo();
//    db.getKeggDbMgr().scriptTo();
//    db.getSeedDbMgr().scriptTo();
    
//    //Thread.sleep(1000*60*15);
//    
//    // Cerrar conexiones con la base de datos
//    db.closeDbConections();
//    // Cerrar el servidor de la base de datos
//    db.shutdown();
//    System.exit(0);

    
    
    
//    db.dropCompTables();
//    db.createCompTables(true);


//    db.dropAllTables();
//    db.createAllTables(false);


//    db.closeDbConections();
//    db.shutdown();
//    System.exit(0);


//    KeggDbManager.INSTANCE.dropTables();
//    KeggDbManager.INSTANCE.createTables();

//    db.dropReactionTables();
//    db.createReactionTables();

    ModelManager mdl = new ModelManager();
    ModelParsing p = new ModelParsing();

//    p.validateSBML("/home/pdsanchez/Descargas/Seed458817.3.xml");
//    System.exit(0);


    long t1 = System.currentTimeMillis();
    System.out.println("==================================");
    //File fileIn = new File("/home/pdsanchez/Documentos/FBA/SBMLs/samples/BIOMD0000000191_SBML-L2V4.xml");
    //File fileIn = new File("/home/pdsanchez/Documentos/FBA/SBMLs/seed_models/Seed158879.1.corrected.xml");
    //File fileIn = new File("/home/pdsanchez/Documentos/FBA/SBMLs/BiGG/Saureus_iSB619.xml");
    //File fileIn = new File("/home/pdsanchez/Documentos/FBA/SBMLs/samples/iEY140.xml");
    //File fileIn = new File("/home/pdsanchez/Documentos/FBA/SBMLs/seed_models/Seed451709.4.xml");
    //File fileIn = new File("/home/pdsanchez/Documentos/FBA/SBMLs/samples/Test.xml");
    //File fileIn = new File("/home/pdsanchez/Documentos/FBA/SBMLs/seed_models/Seed292415.3.xml");
    //File fileIn = new File("/home/pdsanchez/Documentos/FBA/SBMLs/samples/Clostridium_beijerinckii.xml");
    //File fileIn = new File("/home/pdsanchez/Documentos/FBA/SBMLs/seed_models/Seed158879.1.corrected.xml");
    //File fileIn = new File("/home/pdsanchez/Documentos/FBA/SBMLs/BiGG/Ecoli_iAF1260.xml");
    //File fileIn = new File("/home/pdsanchez/Documentos/FBA/SBMLs/scripts_cmodel/Seed158879.1.corrected.xml");
    //File fileIn = new File("/home/pdsanchez/Documentos/FBA/SBMLs/scripts_cmodel/Seed206672.1.corrected.xml");
    //File fileOut = new File("/home/pdsanchez/Documentos/FBA/SBMLs/scripts_cmodel/prb/", "prb_"+fileIn.getName());

    //File fileIn = new File("/home/pdsanchez/Proyectos/TestCModel/Prb02/borrar/Mtuberculosis_iNJ661.xml");
    File fileIn = new File("/home/pdsanchez/Descargas/models/err/model-8009249678813742106.xml");
    File fileOut = new File("/home/pdsanchez/Descargas/output/n_model1.xml");

    //File fileOut = new File("/home/pdsanchez/temp/", fileIn.getName());
    //mdl.writeNormalizedModel(fileIn, fileOut); //Test.xml
    //mdl.writeManualCurationModel();

    try {

//      mdl.parseAndWriteModel(fileIn, fileOut);

        mdl.writeNormalizedModel(fileIn, fileOut);

//      mdl.writeManualCurationModel();
    } catch (ChebiException e) {
      e.printStackTrace();
    }

    //fileIn = new File("/home/pdsanchez/Documentos/FBA/SBMLs/BiGG/Ecoli_iAF1260.xml");
    //fileOut = new File("/home/pdsanchez/temp/", fileIn.getName());
    //mdl.writeNormalizedModel(fileIn, fileOut); //Test.xml

    long t2 = System.currentTimeMillis();
    long ms = (t2-t1);
    System.out.println("### TIEMPO TOTAL: " + ms + " ms. [" + getTime(ms) + "]");




//      long t1 = System.currentTimeMillis();
//      System.out.println("==================================");
//      //Network m2 = p.parseSBML(new File("/home/pdsanchez/Documentos/FBA/SBMLs/samples/BIOMD0000000191_SBML-L2V4.xml"));
//      //System.out.println(m2);
//      Network m0 = p.parseSBML(new File("/home/pdsanchez/Documentos/FBA/SBMLs/BiGG/Saureus_iSB619.xml"));
//      System.out.println(m0);
//      Network m1 = p.parseSBML(new File("/home/pdsanchez/Documentos/FBA/SBMLs/seed_models/Seed158879.1.corrected.xml"));
//      System.out.println(m1);
//      long t2 = System.currentTimeMillis();
//      long ms = (t2-t1);
//      System.out.println("### TIEMPO TOTAL: " + ms + " ms. [" + getTime(ms) + "]");


//      long t1 = System.currentTimeMillis();
//      System.out.println("==================================");
//      //Network m2 = p.parseSBML(new File("/home/pdsanchez/Documentos/FBA/SBMLs/samples/BIOMD0000000191_SBML-L2V4.xml"));
//      //System.out.println(m2);
//      Network m0 = p.parseSBML(new File("/home/pdsanchez/Documentos/FBA/SBMLs/samples/iEY140.xml")); //Test.xml
//      System.out.println(m0);
//      long t2 = System.currentTimeMillis();
//      long ms = (t2-t1);
//      System.out.println("### TIEMPO TOTAL: " + ms + " ms. [" + getTime(ms) + "]");


//      ////////////////////////////////////////////////////////////////////////////
//      // SEED
//      ////////////////////////////////////////////////////////////////////////////
//      long t1 = System.currentTimeMillis();
//      int total = 0;
//      File dir = new File("/home/pdsanchez/Documentos/FBA/SBMLs/seed_models/");
//      for (File file : dir.listFiles()) {
//        if (file.getName().endsWith(".xml")) {
//          total++;
//
//          long ts1 = System.currentTimeMillis();
//          System.out.println();
//          System.out.println("==================================");
//          System.out.println("=== FILE " + total + ": " + file.getName());
//          Network m0 = p.parseSBML(file);
//          System.out.println(m0);
//          long ts2 = System.currentTimeMillis();
//          long ms = (ts2-ts1);
//          System.out.println("--- TIEMPO: " + ms + " ms. [" + getTime(ms) + "]");
//          System.out.println("----------------------------------");
//        }
//      }
//      long t2 = System.currentTimeMillis();
//      long ms = (t2-t1);
//      System.out.println();
//      System.out.println("### FICHEROS PROCESADOS: " + total);
//      System.out.println("### TIEMPO TOTAL: " + ms + " ms. [" + getTime(ms) + "]");





//      ////////////////////////////////////////////////////////////////////////////
//      // BIGG
//      ////////////////////////////////////////////////////////////////////////////
//      long t1 = System.currentTimeMillis();
//      int total = 0;
//      File dir = new File("/home/pdsanchez/Documentos/FBA/SBMLs/BiGG");
//      for (File file : dir.listFiles()) {
//        if (file.getName().endsWith(".xml")) {
//          total++;
//
//          long ts1 = System.currentTimeMillis();
//          System.out.println();
//          System.out.println("==================================");
//          System.out.println("=== FILE " + total + ": " + file.getName());
//          Network m0 = p.parseSBML(file);
//          System.out.println(m0);
//          long ts2 = System.currentTimeMillis();
//          long ms = (ts2-ts1);
//          System.out.println("--- TIEMPO: " + ms + " ms. [" + getTime(ms) + "]");
//          System.out.println("----------------------------------");
//        }
//      }
//      long t2 = System.currentTimeMillis();
//      long ms = (t2-t1);
//      System.out.println();
//      System.out.println("### FICHEROS PROCESADOS: " + total);
//      System.out.println("### TIEMPO TOTAL: " + ms + " ms. [" + getTime(ms) + "]");




//    ////////////////////////////////////////////////////////////////////////////
//    // SEED && BIGG && INSILICO
//    ////////////////////////////////////////////////////////////////////////////
//    long t1 = System.currentTimeMillis();
//    int total = 0;
//    File dir = new File("/home/pdsanchez/Documentos/FBA/SBMLs/seed_models/");
//    for (File file : dir.listFiles()) {
//      if (file.getName().endsWith(".xml")) {
//        total++;
//
//        long ts1 = System.currentTimeMillis();
//        System.out.println();
//        System.out.println("==================================");
//        System.out.println("=== FILE " + total + ": " + file.getName());
//        try {
//          mdl.loadModel(file);
//        } catch (ChebiException e) {
//          e.printStackTrace();
//        }
//
//        long ts2 = System.currentTimeMillis();
//        long ms = (ts2-ts1);
//        System.out.println("--- TIEMPO: " + ms + " ms. [" + getTime(ms) + "]");
//        System.out.println("----------------------------------");
//      }
//    }
//    mdl.writeManualCurationModel();
//
//    dir = new File("/home/pdsanchez/Documentos/FBA/SBMLs/BiGG");
//    for (File file : dir.listFiles()) {
//      if (file.getName().endsWith(".xml")) {
//        total++;
//
//        long ts1 = System.currentTimeMillis();
//        System.out.println();
//        System.out.println("==================================");
//        System.out.println("=== FILE " + total + ": " + file.getName());
//        try {
//          mdl.loadModel(file);
//        } catch (ChebiException e) {
//          e.printStackTrace();
//        }
//
//        long ts2 = System.currentTimeMillis();
//        long ms = (ts2-ts1);
//        System.out.println("--- TIEMPO: " + ms + " ms. [" + getTime(ms) + "]");
//        System.out.println("----------------------------------");
//      }
//    }
////    dir = new File("/home/pdsanchez/Documentos/FBA/SBMLs/insilicoOrg");
////    for (File file : dir.listFiles()) {
////      if (file.getName().endsWith(".xml")) {
////        total++;
////
////        long ts1 = System.currentTimeMillis();
////        System.out.println();
////        System.out.println("==================================");
////        System.out.println("=== FILE " + total + ": " + file.getName());
////        mdl.loadModel(file);
////        long ts2 = System.currentTimeMillis();
////        long ms = (ts2-ts1);
////        System.out.println("--- TIEMPO: " + ms + " ms. [" + getTime(ms) + "]");
////        System.out.println("----------------------------------");
////      }
////    }
//    long t2 = System.currentTimeMillis();
//    long ms = (t2-t1);
//    System.out.println();
//    System.out.println("### FICHEROS PROCESADOS: " + total);
//    System.out.println("### TIEMPO TOTAL: " + ms + " ms. [" + getTime(ms) + "]");
//
//    mdl.writeManualCurationModel();



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
