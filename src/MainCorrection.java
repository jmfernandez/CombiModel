import java.io.File;
import java.io.IOException;

import es.csic.cnb.ModelManager;
import es.csic.cnb.db.DbManager;
import es.csic.cnb.log.LogMgr;
import es.csic.cnb.ws.ChebiException;


public class MainCorrection {
  /**
   * @param args
   * @throws IOException
   * @throws SecurityException
   */
  public static void main(String[] args) {
    //  log
    LogMgr log = new LogMgr();
    log.configureLog();


    DbManager db = DbManager.INSTANCE;
    db.connect();


    ModelManager mdl = new ModelManager();

//    p.validateSBML("/home/pdsanchez/Documentos/FBA/SBMLs/samples/Clostridium_beijerinckii.xml");
//    System.exit(0);



    //////////////////////////////////////////////////////////////////////////////////////
    // COMPLETO
    //////////////////////////////////////////////////////////////////////////////////////
    long t1 = System.currentTimeMillis();
    System.out.println("==================================");
    File fileIn = new File("/home/pdsanchez/temp/borrar/Mtuberculosis_iNJ661.xml");
    //File fileIn = new File("/home/pdsanchez/temp/borrar/Hpylori_iIT341.xml");
    File fileOut = new File("/home/pdsanchez/temp/borrar/", "np_"+fileIn.getName());

//    mdl.parseAndWriteModel(fileIn, fileOut);
    try {
      mdl.writeNormalizedModel(fileIn, fileOut, null);
    } catch (ChebiException e) {
      e.printStackTrace();
    }

    long t2 = System.currentTimeMillis();
    long ms = (t2-t1);
    System.out.println("### TIEMPO TOTAL: " + ms + " ms. [" + getTime(ms) + "]");




//    //////////////////////////////////////////////////////////////////////////////////////
//    // SOLO REAC EN MODELO NORMALIZADO
//    //////////////////////////////////////////////////////////////////////////////////////
//    long t1 = System.currentTimeMillis();
//    System.out.println("==================================");
//    //File fileIn = new File("/home/pdsanchez/Proyectos/TestCModel/SBMLs/Corrections/corregido_Thermotoga_maritima.xml");
//    //File fileIn = new File("/home/pdsanchez/Proyectos/TestCModel/SBMLs/Corrections/Seed206672.1.corrected.xml");
//    //File fileOut = new File("/home/pdsanchez/Proyectos/TestCModel/SBMLs/Corrections/", "corregido2_"+fileIn.getName());
//
//    File fileIn = new File("/home/pdsanchez/Proyectos/TestCModel/SBMLs/Prb/Ec_iJR904_GlcMM.xml");
//    File fileOut = new File("/home/pdsanchez/Proyectos/TestCModel/SBMLs/Prb/", "n_"+fileIn.getName());
//
//    mdl.parseAndWriteModel(fileIn, fileOut);
//
//    long t2 = System.currentTimeMillis();
//    long ms = (t2-t1);
//    System.out.println("### TIEMPO TOTAL: " + ms + " ms. [" + getTime(ms) + "]");




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
