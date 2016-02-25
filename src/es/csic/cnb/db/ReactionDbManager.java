package es.csic.cnb.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.csic.cnb.util.Util;

public enum ReactionDbManager {
  INSTANCE;

  private final static Logger LOGGER = Logger.getLogger(ReactionDbManager.class.getName());

  private static final String DRIVER = "org.h2.Driver";
  private static final String DB = Util.DB;
  private static final String DBNAME = "/db/reaction";
  private static final String USER = "user";
  private static final String PWD = "user";

  private Connection conn;
  private PreparedStatement pstmt;

  public void connect() {
    try {
      Class.forName(DRIVER);
      conn = DriverManager.getConnection(DB + Util.USERDIR + DBNAME, USER, PWD);

    } catch (ClassNotFoundException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  public void close() {
    try {
      if (pstmt != null) pstmt.close();
      conn.close();

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  public void createTables() {
    BufferedReader br = null;
    try {
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE REACTION " +
              "(RXID INT AUTO_INCREMENT PRIMARY KEY, " +
              "NAME VARCHAR(1024), " +
              "REVERSIBLE BOOLEAN, " +
              "FREQ INT DEFAULT 1, " +
              "EX BOOLEAN DEFAULT FALSE);");
      stmt.executeUpdate("CREATE TABLE REACRTN " +
              "(RXID INT NOT NULL, " +
              "CHEMID INT NOT NULL, " +
              "STCH DOUBLE, " +
              "FOREIGN KEY(RXID) " +
              "REFERENCES REACTION(RXID) ON DELETE CASCADE);");
      stmt.executeUpdate("CREATE TABLE REACPRD " +
              "(RXID INT NOT NULL, " +
              "CHEMID INT NOT NULL, " +
              "STCH DOUBLE, " +
              "FOREIGN KEY(RXID) " +
              "REFERENCES REACTION(RXID) ON DELETE CASCADE);");
//      // ?????? Propio de la reac para cada sbml
//      stmt.executeUpdate("CREATE TABLE REACPRM " +
//              "(RXID INT NOT NULL, " +
//              "LB INT, " +
//              "UB INT, " +
//              "OC INT, " +
//              "FV INT, " +
//              "UNITS VARCHAR(512), " +
//              "FOREIGN KEY(RXID) " +
//              "REFERENCES REACTION(RXID) ON DELETE CASCADE);");
      stmt.executeUpdate("CREATE TABLE REACXREF " +
              "(RXID INT NOT NULL, " +
              "XREF VARCHAR(1024), " +
              "SOURCE VARCHAR(32), " +
              "FOREIGN KEY(RXID) " +
              "REFERENCES REACTION(RXID) ON DELETE CASCADE);");

      stmt.executeUpdate("CREATE INDEX IDXREF ON REACXREF(XREF);");
      stmt.close();

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
    finally {
      try {
        if (br != null)
          br.close();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
  }

  public void dropTables() {
    try {
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("DROP TABLE IF EXISTS REACTION;");
      stmt.executeUpdate("DROP TABLE IF EXISTS REACRTN;");
      stmt.executeUpdate("DROP TABLE IF EXISTS REACPRD;");
//      stmt.executeUpdate("DROP TABLE IF EXISTS REACPRM;");
      stmt.executeUpdate("DROP TABLE IF EXISTS REACXREF;");
      stmt.close();
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  /**
   * Devuelve el siguiente rxId disponible.
   *
   * @return rxId
   */
  public synchronized int getNextId() {
    try {
      Statement stmt = conn.createStatement();
      ResultSet result = stmt.executeQuery("SELECT MAX(RXID) FROM REACTION");
      int id = (result.next()) ? result.getInt(1) + 1 : 1;

      stmt.close();
      result.close();

      return id;

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
    return -1;
  }

  /**
   * Recupera la frecuencia para un rxId.
   *
   * @param rxId
   * @return frecuencia
   */
  public int getFrequency(int rxId) {
    int fq = 0;
    try {
      pstmt = conn.prepareStatement(
              "SELECT FREQ FROM REACTION " +
              "WHERE RXID = ?");
      pstmt.setInt(1, rxId);

      int cont = 0;
      ResultSet result = pstmt.executeQuery();
      while(result.next()) {
        fq = result.getInt(1);
        cont++;
      }
      // Verificar que solo hay una freq
      assert cont == 1 : "Mas de una frecuencia [rxId: " + rxId + "]: " + cont;

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }

    return fq;
  }

  /**
   * Incrementa la frecuencia para un rxId.
   *
   * @param rxId
   */
  public synchronized void incrementFrequency(int rxId) {
    int fq = 0;
    try {
      pstmt = conn.prepareStatement(
              "SELECT FREQ FROM REACTION " +
              "WHERE RXID = ?");
      pstmt.setInt(1, rxId);

      int cont = 0;
      ResultSet result = pstmt.executeQuery();
      while(result.next()) {
        fq = result.getInt(1);
        cont++;
      }
      // Verificar que solo hay una freq
      assert cont == 1 : "Mas de una frecuencia [rxId: " + rxId + "]";

      pstmt = conn.prepareStatement("UPDATE REACTION SET FREQ=? WHERE RXID=?");
      pstmt.setInt(1, ++fq);
      pstmt.setInt(2, rxId);
      pstmt.executeUpdate();

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  /**
   * Marcar reaccion como de intercambio.
   *
   * @param rxId
   */
  public synchronized void exchangeReaction(int rxId) {
    try {
      pstmt = conn.prepareStatement("UPDATE REACTION SET EX=TRUE WHERE RXID=?");
      pstmt.setInt(1, rxId);
      pstmt.executeUpdate();

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  public String getName(int rxId) {
    String name = null;
    try {
      pstmt = conn.prepareStatement("SELECT NAME FROM REACTION WHERE RXID = ?");
      pstmt.setInt(1, rxId);

      int cont = 0;
      ResultSet result = pstmt.executeQuery();
      while(result.next()) {
        name = result.getString(1);
        cont++;
      }
      // Verificar que solo hay un ofn
      assert cont == 1 : "Mas de un nombre [rxId: " + rxId + "]";

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }

    return name;
  }

  /**
   * Devuelve una lista de rxIds para un xref.
   *
   * @param xref
   * @param source
   * @return Lista de rxIds.
   */
  public List<Integer> getRxIdListByXref(String xref, String source) {
    List<Integer> list = new ArrayList<Integer>();

    try {
      pstmt = conn.prepareStatement(
              "SELECT RXID FROM REACXREF " +
              "WHERE XREF = ? " +
              "AND SOURCE = ?");
      pstmt.setString(1, xref);
      pstmt.setString(2, source);

      ResultSet result = pstmt.executeQuery();
      while(result.next()) {
        list.add(result.getInt(1));
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }

    return list;
  }

  /**
   * Devuelve una lista de rxIds para Kegg.
   *
   * @param keggId
   * @return Lista de rxIds.
   */
  public List<Integer> getRxIdListForKegg(String keggId) {
    List<Integer> list = new ArrayList<Integer>();

    try {
      pstmt = conn.prepareStatement(
              "SELECT RXID FROM REACXREF " +
              "WHERE XREF = ? " +
              "AND SOURCE = ?");
      pstmt.setString(1, keggId);
      pstmt.setString(2, Util.SOURCE_KEGG);

      ResultSet result = pstmt.executeQuery();
      while(result.next()) {
        list.add(result.getInt(1));
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }

    return list;
  }

  /**
   * Devuelve una lista de rxIds para EC.
   *
   * @param ec
   * @return Lista de rxIds.
   */
  public List<Integer> getRxIdListForEc(String ec) {
    List<Integer> list = new ArrayList<Integer>();

    try {
      pstmt = conn.prepareStatement(
              "SELECT RXID FROM REACXREF " +
              "WHERE XREF = ? " +
              "AND SOURCE = ?");
      pstmt.setString(1, ec);
      pstmt.setString(2, Util.SOURCE_EC);

      ResultSet result = pstmt.executeQuery();
      while(result.next()) {
        list.add(result.getInt(1));
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }

    return list;
  }

  /**
   * Devuelve el rxId que corresponde al id de la reaccion del sbml.
   *
   * @param sbmlId
   * @return Lista de rxIds (solo puede tener uno o ningun valores)
   */
  public List<Integer> getRxIdForSbmlId(String sbmlId) {
    List<Integer> list = new ArrayList<Integer>();

    try {
      pstmt = conn.prepareStatement(
              "SELECT RXID FROM REACXREF " +
              "WHERE XREF = ? " +
              "AND SOURCE = ?");
      pstmt.setString(1, sbmlId);
      pstmt.setString(2, Util.SOURCE_SBML);

      int cont = 0;
      ResultSet result = pstmt.executeQuery();
      while(result.next()) {
        list.add(result.getInt(1));
      }

      // Verificar que solo hay un sbmlId o ninguno
      assert (cont <= 1) : "Mas de un rxId " + list + " para sbmlId: " + sbmlId;

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }

    return list;
  }

  /**
   * Recupera la lista de referencias a otras bases de datos a partir de un rxId.
   *
   * @param rxid
   * @return lista de xrefs
   */
  public Set<String> getXrefList(int rxId) {
    Set<String> list = new HashSet<String>();

    try {
      pstmt = conn.prepareStatement(
              "SELECT XREF FROM REACXREF " +
              "WHERE RXID = ?");
      pstmt.setInt(1, rxId);

      ResultSet result = pstmt.executeQuery();
      while(result.next()) {
        String xref = result.getString(1);
        list.add(xref);
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }

    return list;
  }

  /**
   * Recupera la lista de referencias a otras bases de datos a partir de un rxId
   * para una fuente de datos especificada.
   *
   * @param rxId
   * @param source
   * @return lista de xrefs
   */
  public Set<String> getXrefList(int rxId, String source) {
    Set<String> list = new HashSet<String>();

    try {
      pstmt = conn.prepareStatement(
              "SELECT XREF FROM REACXREF " +
              "WHERE RXID = ? " +
              "AND SOURCE = ?");
      pstmt.setInt(1, rxId);
      pstmt.setString(2, source);

      ResultSet result = pstmt.executeQuery();
      while(result.next()) {
        String xref = result.getString(1);
        list.add(xref);
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }

    return list;
  }

  public String getEc(int rxId) {
    String ec = null;
    try {
      pstmt = conn.prepareStatement("SELECT XREF FROM REACXREF WHERE SOURCE = ? AND RXID = ?");
      pstmt.setString(1, Util.SOURCE_EC);
      pstmt.setInt(2, rxId);

      int cont = 0;
      ResultSet result = pstmt.executeQuery();
      while(result.next()) {
        ec = result.getString(1);
        cont++;
      }
      // Verificar que solo hay un ec
      assert cont <= 1 : "Mas de un ec [rxId: " + rxId + "]: " + cont;

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }

    return ec;
  }

  public String getKeggId(int rxId) {
    String keggId = null;
    try {
      pstmt = conn.prepareStatement("SELECT XREF FROM REACXREF WHERE SOURCE = ? AND RXID = ?");
      pstmt.setString(1, Util.SOURCE_KEGG);
      pstmt.setInt(2, rxId);

      int cont = 0;
      ResultSet result = pstmt.executeQuery();
      while(result.next()) {
        keggId = result.getString(1);
        cont++;
      }
      // Verificar que solo hay un ec
      assert cont <= 1 : "Mas de un ec [rxId: " + rxId + "]: " + cont;

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }

    return keggId;
  }

  public List<String> getSbmlIdList(int rxId) {
    List<String> list = new ArrayList<String>();
    try {
      pstmt = conn.prepareStatement("SELECT XREF FROM REACXREF WHERE SOURCE = ? AND RXID = ?");
      pstmt.setString(1, Util.SOURCE_SBML);
      pstmt.setInt(2, rxId);

      ResultSet result = pstmt.executeQuery();
      while(result.next()) {
        list.add(result.getString(1));
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }

    return list;
  }

  public Map<Integer,Double> getReactantList(int rxId) {
    Map<Integer,Double> list = new HashMap<Integer,Double>();
    try {
      pstmt = conn.prepareStatement("SELECT CHEMID,STCH FROM REACRTN WHERE RXID = ?");
      pstmt.setInt(1, rxId);

      ResultSet result = pstmt.executeQuery();
      while(result.next()) {
        list.put(result.getInt(1), result.getDouble(2));
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }

    return list;
  }

  public Map<Integer,Double> getProductList(int rxId) {
    Map<Integer,Double> list = new HashMap<Integer,Double>();
    try {
      pstmt = conn.prepareStatement("SELECT CHEMID,STCH FROM REACPRD WHERE RXID = ?");
      pstmt.setInt(1, rxId);

      ResultSet result = pstmt.executeQuery();
      while(result.next()) {
        list.put(result.getInt(1), result.getDouble(2));
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }

    return list;
  }

  ////////////////////////////////////////////////////////
  // INSERTS
  ////////////////////////////////////////////////////////

  public synchronized void insertReaction(int rxId, String name, boolean reversible) {
    assert (name != null);
    try {
      pstmt = conn.prepareStatement("INSERT INTO REACTION (RXID,NAME,REVERSIBLE) VALUES (?,?,?)");
      pstmt.setInt(1, rxId);
      pstmt.setString(2, name);
      pstmt.setBoolean(3, reversible);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  public synchronized void insertReactant(int rxId, int chemId, double stoichiometry) {
    try {
      pstmt = conn.prepareStatement("INSERT INTO REACRTN (RXID,CHEMID,STCH) VALUES (?,?,?)");
      pstmt.setInt(1, rxId);
      pstmt.setInt(2, chemId);
      pstmt.setDouble(3, stoichiometry);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  public synchronized void insertProduct(int rxId, int chemId, double stoichiometry) {
    try {
      pstmt = conn.prepareStatement("INSERT INTO REACPRD (RXID,CHEMID,STCH) VALUES (?,?,?)");
      pstmt.setInt(1, rxId);
      pstmt.setInt(2, chemId);
      pstmt.setDouble(3, stoichiometry);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  public synchronized void insertReacXref(int rxId, String source, String xref) {
    if (xref != null && !xref.isEmpty()) {
      try {
        pstmt = conn.prepareStatement("INSERT INTO REACXREF (RXID,XREF,SOURCE) VALUES (?,?,?)");
        pstmt.setInt(1, rxId);
        pstmt.setString(2, xref);
        pstmt.setString(3, source);
        pstmt.executeUpdate();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }
  }
}
