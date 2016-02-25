package es.csic.cnb.log;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.csic.cnb.util.PropertiesMgr;
import es.csic.cnb.util.Util;

public class LogMgr {
  private static final Logger LOGGER = Logger.getLogger("es.csic.cnb");

  public void configureLog() {
    LOGGER.setUseParentHandlers(false);

    // se establece el nivel predeterminado global
    LOGGER.setLevel(Level.parse(PropertiesMgr.INSTANCE.getLevel()));


    // manejador de consola
    ConsoleHandler ch = new ConsoleHandler();
    ch.setLevel(Level.parse(PropertiesMgr.INSTANCE.getConsoleLevel()));
    ch.setFormatter(new CustomFormatter()); //formatter

    LOGGER.addHandler(ch);


    // Crear manejador de archivo
    //String filepath = Util.USERDIR + "/log/combimodel.log";
    String filepath = Util.USERDIR + "/log/cmodel_%g.log";

    FileHandler fh = null;
    try {
      // new FileHandler(pattern, limit, count, append)
      fh = new FileHandler(filepath, 25600000, 3, false);
      //fh = new FileHandler(filepath, false);

      fh.setLevel(Level.parse(PropertiesMgr.INSTANCE.getFilelogLevel())); // level
      fh.setFormatter(new CustomFormatter()); //formatter

      // agregar el manejador de archivo al log
      LOGGER.addHandler(fh);

    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
