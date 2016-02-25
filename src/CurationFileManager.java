import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.csic.cnb.util.Util;

public class CurationFileManager {
  private final static Logger LOGGER = Logger.getLogger(CurationFileManager.class.getName());

  //Datos de la correccion manual
  private Map<String,String> chebiManualCuration;
  private Map<String,String> dbManualCuration;

  public CurationFileManager() {
    chebiManualCuration = new LinkedHashMap<String,String>();
    dbManualCuration = new LinkedHashMap<String,String>();

    BufferedReader br = null;
    try {
      String sCurrentLine;

      br = new BufferedReader(new FileReader(Util.USERDIR_MANFILE));
      while ((sCurrentLine = br.readLine()) != null) {
        if (sCurrentLine.startsWith("#") || sCurrentLine.trim().isEmpty())
          continue;

        String[] reg = sCurrentLine.split(Util.TAB);
        if (reg.length == 4) {
          chebiManualCuration.put(reg[0], reg[3]);
        }
        else {
          // db
          if (Boolean.parseBoolean(reg[5])) {
            dbManualCuration.put(reg[0], reg[4]);
          }
          // chebi
          else {
            chebiManualCuration.put(reg[0], reg[3]);
          }
        }
      }
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
    finally {
      try {
        if (br != null)
          br.close();
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }
  }

  public String getXrefManualCurationChebiId(String sbmlId) {
    return chebiManualCuration.get(sbmlId);
  }

  public String getDbManualCurationRefSbmlId(String sbmlId) {
    return dbManualCuration.get(sbmlId);
  }
}
