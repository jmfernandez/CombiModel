package es.csic.cnb.db;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.h2.jdbcx.JdbcConnectionPool;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import es.csic.cnb.data.Fm;
import es.csic.cnb.data.NetCompound;
import es.csic.cnb.util.Util;

public class CompoundDbManager {
  private final static Logger LOGGER = Logger.getLogger(CompoundDbManager.class.getName());

  private static Matcher mt = Pattern.compile("\\W").matcher("");

  private static final String CHEMID = "CHEMID";

  private JdbcConnectionPool cp;

  public CompoundDbManager(JdbcConnectionPool cp) {
    this.cp = cp;
  }

  // En COMPXREF se almacenan referencias a otras bases de datos, SMILES, INCHI e INCHIKEY
  public void createTables(boolean loadCurationFile) throws SQLException {
    Statement stmt = null;
    Connection conn = null;
    try {
      conn = cp.getConnection();
      stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE COMPOUND " +
              "(CHEMID INT AUTO_INCREMENT PRIMARY KEY, " +
              "OFN VARCHAR(1024), " +
              "FREQ INT DEFAULT 1, " +
              "EX BOOLEAN DEFAULT FALSE, " +
              "CHARGE INT DEFAULT 0, " +
              "MAPPING INT DEFAULT 0);");
      stmt.executeUpdate("CREATE TABLE COMPSYN " +
              "(CHEMID INT NOT NULL, " +
              "SYN VARCHAR_IGNORECASE(1024), " +
              "FOREIGN KEY(CHEMID) " +
              "REFERENCES COMPOUND(CHEMID) ON DELETE CASCADE);");
      stmt.executeUpdate("CREATE TABLE COMPNSYN " +
              "(CHEMID INT NOT NULL, " +
              "SYN VARCHAR_IGNORECASE(1024), " +
              "FOREIGN KEY(CHEMID) " +
              "REFERENCES COMPOUND(CHEMID) ON DELETE CASCADE);");
      stmt.executeUpdate("CREATE TABLE COMPFM " +
              "(CHEMID INT NOT NULL, " +
              "FM VARCHAR_IGNORECASE(256), " +
              "SOURCE VARCHAR(32), " +
              "CHARGE INT DEFAULT 0, " +
              "FOREIGN KEY(CHEMID) " +
              "REFERENCES COMPOUND(CHEMID) ON DELETE CASCADE);");
      stmt.executeUpdate("CREATE TABLE COMPXREF " +
              "(CHEMID INT NOT NULL, " +
              "XREF VARCHAR(1024), " +
              "SOURCE VARCHAR(32), " +
              "FOREIGN KEY(CHEMID) " +
              "REFERENCES COMPOUND(CHEMID) ON DELETE CASCADE);");

      stmt.executeUpdate("CREATE INDEX IDXSYN ON COMPSYN(SYN);");
      stmt.executeUpdate("CREATE INDEX IDXNSYN ON COMPNSYN(SYN);");
      stmt.executeUpdate("CREATE INDEX IDXFM ON COMPFM(FM);");
      stmt.executeUpdate("CREATE INDEX IDXREF ON COMPXREF(XREF);");

      // Cargar datos del fichero de curacion manual
      if (loadCurationFile) {
        loadAndInsertCurationFile();
      }
    }
    finally {
      if (stmt != null)
        stmt.close();
      if (conn != null)
        conn.close();
    }
  }

  public void dropTables() throws SQLException {
    Statement stmt = null;
    Connection conn = null;
    try {
      conn = cp.getConnection();
      stmt = conn.createStatement();
      stmt.executeUpdate("DROP TABLE IF EXISTS COMPOUND;");
      stmt.executeUpdate("DROP TABLE IF EXISTS COMPSYN;");
      stmt.executeUpdate("DROP TABLE IF EXISTS COMPNSYN;");
      stmt.executeUpdate("DROP TABLE IF EXISTS COMPFM;");
      stmt.executeUpdate("DROP TABLE IF EXISTS COMPXREF;");
    }
    finally {
      if (stmt != null)
        stmt.close();
      if (conn != null)
        conn.close();
    }
  }
  
  
  public void scriptTo() throws SQLException {
	    Statement stmt = null;
	    Connection conn = null;
	    try {
	      conn = cp.getConnection();
	      stmt = conn.createStatement();
	      stmt.executeQuery("SCRIPT DROP TO '/home/pdsanchez/CNB/cmodel/db/sql/bkp_compound.sql'");
	    }
	    finally {
	      if (stmt != null)
	        stmt.close();
	      if (conn != null)
	        conn.close();
	    }
	  }

  /**
   * Devuelve el siguiente chemId disponible.
   *
   * @return chemId
   */
  public int getNextId() {
    int id = -1;

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT MAX(CHEMID) AS CHEMID FROM COMPOUND");

      result = pstmt.executeQuery();
      id = (result.next()) ? result.getInt(CHEMID) + 1 : 1;

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return id;
  }

  /**
   * Devuelve una lista de chemIds para un xref.
   *
   * @param xref
   * @param source
   * @return Lista de chemIds.
   */
  public List<Integer> getChemIdListByXref(String xref, String source) {
    List<Integer> list = new ArrayList<Integer>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT CHEMID FROM COMPXREF " +
              "WHERE XREF = ? " +
              "AND SOURCE = ?");
      pstmt.setString(1, xref);
      pstmt.setString(2, source);

      result = pstmt.executeQuery();
      while(result.next()) {
        list.add(result.getInt(CHEMID));
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return list;
  }

  /**
   * Devuelve una lista de chemIds para Chebi.
   *
   * @param chebiId
   * @return Lista de chemIds.
   */
  public List<Integer> getChemIdListForChebi(String chebiId) {
    List<Integer> list = new ArrayList<Integer>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT CHEMID FROM COMPXREF " +
              "WHERE XREF = ? " +
              "AND SOURCE = ?");
      pstmt.setString(1, chebiId);
      pstmt.setString(2, Util.SOURCE_CHEBI);

      result = pstmt.executeQuery();
      while(result.next()) {
        list.add(result.getInt(CHEMID));
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }
    return list;
  }

  /**
   * Devuelve una lista de chemIds para Kegg.
   *
   * @param keggId
   * @return Lista de chemIds.
   */
  public List<Integer> getChemIdListForKegg(String keggId) {
    List<Integer> list = new ArrayList<Integer>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT CHEMID FROM COMPXREF " +
              "WHERE XREF = ? " +
              "AND SOURCE = ?");
      pstmt.setString(1, keggId);
      pstmt.setString(2, Util.SOURCE_KEGG);

      result = pstmt.executeQuery();
      while(result.next()) {
        list.add(result.getInt(CHEMID));
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return list;
  }

  /**
   * Devuelve una lista de chemIds para la formula del sbml.
   *
   * @param fm
   * @return Lista de chemIds.
   */
  public List<Integer> getChemIdListForSbmlFormula(String fm) {
    List<Integer> list = new ArrayList<Integer>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT CHEMID FROM COMPFM " +
              "WHERE FM = ? " +
              "AND SOURCE = ?");
      pstmt.setString(1, fm);
      pstmt.setString(2, Util.SOURCE_FORMULA_SBML);

      result = pstmt.executeQuery();
      while(result.next()) {
        list.add(result.getInt(CHEMID));
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return list;
  }

  /**
   * Devuelve una lista de chemIds para la formula neutras.
   *
   * @param fm
   * @return Lista de chemIds.
   */
  public List<Integer> getChemIdListForNeutralFormula(String fm) {
    List<Integer> list = new ArrayList<Integer>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT CHEMID FROM COMPFM " +
              "WHERE FM = ? " +
              "AND SOURCE = ?");
      pstmt.setString(1, fm);
      pstmt.setString(2, Util.SOURCE_FORMULA_NEUTRAL);

      result = pstmt.executeQuery();
      while(result.next()) {
        list.add(result.getInt(CHEMID));
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return list;
  }

  /**
   * Devuelve una lista de chemIds para otras formulas adicionales.
   *
   * @param fm
   * @return Lista de chemIds.
   */
  public List<Integer> getChemIdListForOtherFormula(String fm) {
    List<Integer> list = new ArrayList<Integer>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT CHEMID FROM COMPFM " +
              "WHERE FM = ? " +
              "AND SOURCE = ?");
      pstmt.setString(1, fm);
      pstmt.setString(2, Util.SOURCE_FORMULA_OTHER);

      result = pstmt.executeQuery();
      while(result.next()) {
        list.add(result.getInt(CHEMID));
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return list;
  }

  /**
   * Devuelve una lista de chemIds para estructuras.
   *
   * @param st
   * @return Lista de chemIds.
   */
  public List<Integer> getChemIdListForStructures(String st) {
    List<Integer> list = new ArrayList<Integer>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT CHEMID FROM COMPXREF " +
              "WHERE XREF = ? " +
              "AND SOURCE = ?");
      pstmt.setString(1, st);
      pstmt.setString(2, Util.SOURCE_STRUCTURE);

      result = pstmt.executeQuery();
      while(result.next()) {
        list.add(result.getInt(CHEMID));
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return list;
  }

  /**
   * Devuelve el chemId que corresponde al id del sbml.
   *
   * @param sbmlId
   * @return Lista de chemIds (solo puede tener uno o ningun valores)
   */
  public List<Integer> getChemIdForSbmlId(String sbmlId) {
    List<Integer> list = new ArrayList<Integer>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT CHEMID FROM COMPXREF " +
              "WHERE XREF = ? " +
              "AND SOURCE = ?");
      pstmt.setString(1, sbmlId);
      pstmt.setString(2, Util.SOURCE_SBML);

      int cont = 0;
      result = pstmt.executeQuery();
      while(result.next()) {
        list.add(result.getInt(CHEMID));
      }

      // Verificar que solo hay un sbmlId o ninguno
      assert (cont <= 1) : "Mas de un chemId " + list + " para sbmlId: " + sbmlId;

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return list;
  }

  /**
   * Busqueda de chemIds por sinonimo sin tener en cuenta el case (mays-minusculas).
   *
   * @param syn
   * @return Lista de chemIds.
   */
  public List<Integer> getChemIdListBySyn(String syn) {
    List<Integer> list = new ArrayList<Integer>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT CHEMID FROM COMPSYN " +
              "WHERE SYN = ?");
      pstmt.setString(1, syn);

      result = pstmt.executeQuery();
      while(result.next()) {
        list.add(result.getInt(CHEMID));
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return list;
  }

  /**
   * Busqueda de chemIds por sinonimo normalizado.
   *
   * @param syn
   * @return Lista de chemIds.
   */
  public List<Integer> getChemIdListByNormalSyn(String syn) {
    List<Integer> list = new ArrayList<Integer>();

    syn = Util.getChemicalName(Util.splitChemicalName(syn)).toLowerCase();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT CHEMID FROM COMPSYN " +
              "WHERE SYN = ?");
      pstmt.setString(1, syn);

      result = pstmt.executeQuery();
      while(result.next()) {
        list.add(result.getInt(CHEMID));
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return list;
  }

  /**
   * Recupera el OFN para un chemId.
   *
   * @param chemid
   * @return ofn
   */
  public String getOfn(int chemId) {
    String ofn = null;

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT OFN FROM COMPOUND " +
              "WHERE CHEMID = ?");
      pstmt.setInt(1, chemId);

      int cont = 0;
      result = pstmt.executeQuery();
      while(result.next()) {
        ofn = result.getString(1);
        cont++;
      }
      // Verificar que solo hay un ofn
      assert cont == 1 : "Mas de un ofn [chemId: " + chemId + "]";

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return ofn;
  }

  /**
   * Recupera la carga para un chemId.
   *
   * @param chemid
   * @return carga
   */
  public int getCharge(int chemId) {
    int chg = 0;

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT CHARGE FROM COMPOUND " +
              "WHERE CHEMID = ?");
      pstmt.setInt(1, chemId);

      int cont = 0;
      result = pstmt.executeQuery();
      while(result.next()) {
        chg = result.getInt(1);
        cont++;
      }
      // Verificar que solo hay una carga
      assert cont == 1 : "Mas de una carga [chemId: " + chemId + "]";

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return chg;
  }

  /**
   * Recupera la frecuencia para un chemId.
   *
   * @param chemid
   * @return frecuencia
   */
  public int getFrequency(int chemId) {
    int fq = 0;

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT FREQ FROM COMPOUND " +
              "WHERE CHEMID = ?");
      pstmt.setInt(1, chemId);

      int cont = 0;
      result = pstmt.executeQuery();
      while(result.next()) {
        fq = result.getInt(1);
        cont++;
      }
      // Verificar que solo hay una freq
      assert cont == 1 : "Mas de una frecuencia [chemId: " + chemId + "]";

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return fq;
  }

  /**
   * Incrementa la frecuencia para un chemId.
   *
   * @param chemid
   */
  public void incrementFrequency(int chemId) {
    int fq = 0;

    Connection conn = null;
    PreparedStatement pstmt1 = null;
    PreparedStatement pstmt2 = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt1 = conn.prepareStatement(
              "SELECT FREQ FROM COMPOUND " +
              "WHERE CHEMID = ?");
      pstmt1.setInt(1, chemId);

      int cont = 0;
      result = pstmt1.executeQuery();
      while(result.next()) {
        fq = result.getInt(1);
        cont++;
      }
      // Verificar que solo hay una freq
      assert cont == 1 : "Mas de una frecuencia [chemId: " + chemId + "]";

      pstmt2 = conn.prepareStatement("UPDATE COMPOUND SET FREQ=? WHERE CHEMID=?");
      pstmt2.setInt(1, ++fq);
      pstmt2.setInt(2, chemId);
      pstmt2.executeUpdate();

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt1 != null)
          pstmt1.close();
        if (pstmt2 != null)
          pstmt2.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }
  }

  /**
   * Marcar compuesto como de intercambio.
   *
   * @param chemId
   */
  public void exchangeReaction(int chemId) {
    PreparedStatement pstmt = null;
    Connection conn = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement("UPDATE COMPOUND SET EX=TRUE WHERE CHEMID=?");
      pstmt.setInt(1, chemId);
      pstmt.executeUpdate();
    }
    catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
    finally {
      try {
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }
  }

  /**
   * Recupera la lista de sinonimos para un chemId.
   *
   * @param chemid
   * @return lista de sinonimos
   */
  public Set<String> getSynList(int chemId) {
    Set<String> list = new HashSet<String>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT SYN FROM COMPSYN " +
              "WHERE CHEMID = ?");
      pstmt.setInt(1, chemId);

      result = pstmt.executeQuery();
      while(result.next()) {
        list.add(result.getString(1));
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return list;
  }

  /**
   * Recupera la lista de sinonimos normalizados para un chemId.
   *
   * @param chemid
   * @return lista de sinonimos
   */
  public Set<String> getNormalSynList(int chemId) {
    Set<String> list = new HashSet<String>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT SYN FROM COMPNSYN " +
              "WHERE CHEMID = ?");
      pstmt.setInt(1, chemId);

      result = pstmt.executeQuery();
      while(result.next()) {
        list.add(result.getString(1));
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return list;
  }

  /**
   * Recupera la lista de referencias a otras bases de datos a partir de un chemId.
   *
   * @param chemid
   * @return lista de xrefs
   */
  public Set<String> getXrefList(int chemId) {
    Set<String> list = new HashSet<String>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT XREF FROM COMPXREF " +
              "WHERE CHEMID = ?");
      pstmt.setInt(1, chemId);

      result = pstmt.executeQuery();
      while(result.next()) {
        String xref = result.getString(1);
        list.add(xref);
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return list;
  }

  /**
   * Recupera la lista de referencias a otras bases de datos a partir de un chemId
   * para una fuente de datos especificada.
   *
   * @param chemId
   * @param source
   * @return lista de xrefs
   */
  public Set<String> getXrefList(int chemId, String source) {
    Set<String> list = new HashSet<String>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT XREF FROM COMPXREF " +
              "WHERE CHEMID = ? " +
              "AND SOURCE = ?");
      pstmt.setInt(1, chemId);
      pstmt.setString(2, source);

      result = pstmt.executeQuery();
      while(result.next()) {
        String xref = result.getString(1);
        list.add(xref);
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return list;
  }

  /**
   * Recupera la lista de formulas a partir de un chemId.
   *
   * @param chemid
   * @return hash source-formula
   */
  public Set<String> getFmList(int chemId) {
    Set<String> list = new HashSet<String>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT FM FROM COMPFM " +
              "WHERE CHEMID = ?");
      pstmt.setInt(1, chemId);

      result = pstmt.executeQuery();
      while(result.next()) {
        String fm = result.getString(1);
        list.add(fm);
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return list;
  }

  /**
   * Recupera la lista de formulas del sbml a partir de un chemId.
   *
   * @param chemId
   * @return lista de formulas del sbml
   */
  public Set<String> getSbmlFormulaList(int chemId) {
    Set<String> list = new HashSet<String>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT FM FROM COMPFM " +
              "WHERE CHEMID = ? " +
              "AND SOURCE = ?");
      pstmt.setInt(1, chemId);
      pstmt.setString(2, Util.SOURCE_FORMULA_SBML);

      result = pstmt.executeQuery();
      while(result.next()) {
        String fm = result.getString(1);
        list.add(fm);
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return list;
  }

  /**
   * Recupera la lista de formulas neutra a partir de un chemId.
   *
   * @param chemId
   * @return lista de formulas neutra
   */
  public Set<String> getNeutralFormulaList(int chemId) {
    Set<String> list = new HashSet<String>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT FM FROM COMPFM " +
              "WHERE CHEMID = ? " +
              "AND SOURCE = ?");
      pstmt.setInt(1, chemId);
      pstmt.setString(2, Util.SOURCE_FORMULA_NEUTRAL);

      result = pstmt.executeQuery();
      while(result.next()) {
        String fm = result.getString(1);
        list.add(fm);
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return list;
  }

  /**
   * Recupera la lista de Inchis y Smiles a partir de un chemId.
   *
   * @param chemId
   * @return lista con los Inchis y Smiles
   */
  public List<String> getStructureList(int chemId) {
    List<String> list = new ArrayList<String>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT XREF FROM COMPXREF " +
              "WHERE CHEMID = ? " +
              "AND SOURCE = ?");
      pstmt.setInt(1, chemId);
      pstmt.setString(2, Util.SOURCE_STRUCTURE);

      result = pstmt.executeQuery();
      while(result.next()) {
        list.add(result.getString(1));
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return list;
  }

  /**
   * Recupera la lista de ids correspondientes a sbmls asociados a un chemId.
   *
   * @param chemId
   * @return lista con los ids correspondientes a sbmls
   */
  public List<String> getSbmlIdList(int chemId) {
    List<String> list = new ArrayList<String>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT XREF FROM COMPXREF " +
              "WHERE CHEMID = ? " +
              "AND SOURCE = ?");
      pstmt.setInt(1, chemId);
      pstmt.setString(2, Util.SOURCE_SBML);

      result = pstmt.executeQuery();
      while(result.next()) {
        list.add(result.getString(1));
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return list;
  }

  /**
   * Recupera la lista de ids de ChEBI a partir de un chemId.
   *
   * @param chemId
   * @return lista con los ids de ChEBI
   */
  public Set<String> getChebiIdList(int chemId) {
    Set<String> list = new HashSet<String>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT XREF FROM COMPXREF " +
              "WHERE CHEMID = ? " +
              "AND SOURCE = ?");
      pstmt.setInt(1, chemId);
      pstmt.setString(2, Util.SOURCE_CHEBI);

      result = pstmt.executeQuery();
      while(result.next()) {
        String id = result.getString(1);
        list.add(id);
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return list;
  }

  /**
   * Recupera la lista de ids de Kegg a partir de un chemId.
   *
   * @param chemId
   * @return Lista con los ids de Kegg
   */
  public Set<String> getKeggIdList(int chemId) {
    Set<String> list = new HashSet<String>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT XREF FROM COMPXREF " +
              "WHERE CHEMID = ? " +
              "AND SOURCE = ?");
      pstmt.setInt(1, chemId);
      pstmt.setString(2, Util.SOURCE_KEGG);

      result = pstmt.executeQuery();
      while(result.next()) {
        String id = result.getString(1);
        list.add(id);
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return list;
  }


  /**
   * @return Total de compuestos insertados en la base de datos.
   */
  public int getTotalCompounds() {
    int total = 0;

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT COUNT(*) FROM COMPOUND");

      result = pstmt.executeQuery();
      while(result.next()) {
        total = result.getInt(1);
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return total;
  }

  /**
   * @return Total de compuestos no mapeados con ddbbs externas.
   */
  public int getTotalCompoundsNoMapping() {
    int total = 0;

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT COUNT(*) FROM COMPOUND " +
              "WHERE MAPPING = 0");

      result = pstmt.executeQuery();
      while(result.next()) {
        total = result.getInt(1);
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return total;
  }

  /**
   * @return Total de compuestos de intercambio.
   */
  public int getTotalExchangeCompounds() {
    int total = 0;

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT COUNT(*) FROM COMPOUND " +
              "WHERE EX = TRUE");

      result = pstmt.executeQuery();
      while(result.next()) {
        total = result.getInt(1);
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return total;
  }

  /**
   * @return Total de compuestos de intercambio no mapeados con ddbbs externas.
   */
  public int getTotalExchangeCompoundsNoMapping() {
    int total = 0;

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT COUNT(*) FROM COMPOUND " +
              "WHERE EX = TRUE AND MAPPING = 0");

      result = pstmt.executeQuery();
      while(result.next()) {
        total = result.getInt(1);
      }

    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      try {
        if (result != null)
          result.close();
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }

    return total;
  }


  ////////////////////////////////////////////////////////
  // INSERTS
  ////////////////////////////////////////////////////////

  public void insertCompound(int id, String ofn, int charge, int mapping) {
    assert (ofn != null);

    PreparedStatement pstmt = null;
    Connection conn = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "INSERT INTO COMPOUND (CHEMID,OFN,CHARGE,MAPPING) VALUES (?,?,?,?)");
      pstmt.setInt(1, id);
      pstmt.setString(2, ofn);
      pstmt.setInt(3, charge);
      pstmt.setInt(4, mapping);
      pstmt.executeUpdate();
    }
    catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
    finally {
      try {
        if (pstmt != null)
          pstmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }
  }

  public void insertCompSyn(int id, String syn) {
    if (syn != null && !syn.isEmpty()) {
      PreparedStatement pstmt1 = null;
      PreparedStatement pstmt2 = null;
      Connection conn = null;
      try {
        conn = cp.getConnection();
        pstmt1 = conn.prepareStatement(
                "INSERT INTO COMPSYN (CHEMID,SYN) VALUES (?,?)");
        pstmt1.setInt(1, id);
        pstmt1.setString(2, syn);
        pstmt1.executeUpdate();

        if (mt.reset(syn).find()) {
          Set<String> list = this.getNormalSynList(id);
          // Normalizado
          syn = Util.getChemicalName(Util.splitChemicalName(syn)).toLowerCase();
          if (!list.contains(syn)) {
            pstmt2 = conn.prepareStatement("INSERT INTO COMPNSYN (CHEMID,SYN) VALUES (?,?)");
            pstmt2.setInt(1, id);
            pstmt2.setString(2, syn);
            pstmt2.executeUpdate();
          }
        }
      }
      catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
      finally {
        try {
          if (pstmt1 != null)
            pstmt1.close();
          if (pstmt2 != null)
            pstmt2.close();
          if (conn != null)
            conn.close();
        } catch (SQLException e) {
          LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
      }
    }
  }

  public void insertCompFm(int id, String source, String fm, int charge) {
    if (fm != null && !fm.isEmpty()) {
      PreparedStatement pstmt = null;
      Connection conn = null;
      try {
        conn = cp.getConnection();
        pstmt = conn.prepareStatement(
                "INSERT INTO COMPFM (CHEMID,FM,SOURCE,CHARGE) VALUES (?,?,?,?)");
        pstmt.setInt(1, id);
        pstmt.setString(2, fm);
        pstmt.setString(3, source);
        pstmt.setInt(4, charge);
        pstmt.executeUpdate();
      }
      catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
      finally {
        try {
          if (pstmt != null)
            pstmt.close();
          if (conn != null)
            conn.close();
        } catch (SQLException e) {
          LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
      }
    }
  }

  public void insertCompXref(int id, String source, String xref) {
    if (xref != null && !xref.isEmpty()) {
      PreparedStatement pstmt = null;
      Connection conn = null;
      try {
        conn = cp.getConnection();
        pstmt = conn.prepareStatement(
                "INSERT INTO COMPXREF (CHEMID,XREF,SOURCE) VALUES (?,?,?)");
        pstmt.setInt(1, id);
        pstmt.setString(2, xref);
        pstmt.setString(3, source);
        pstmt.executeUpdate();
      }
      catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
      finally {
        try {
          if (pstmt != null)
            pstmt.close();
          if (conn != null)
            conn.close();
        } catch (SQLException e) {
          LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
      }
    }
  }

  ///////////////////////////////////////////////////////////////////
  ///// INSERTAR O ACTUALIZAR ENTRADAS DURANTE LA NORMALIZACION
  ///////////////////////////////////////////////////////////////////

  /**
   * Metodo interno que inserta los datos del compuesto en la base de datos.
   * @param comp
   * @return
   */
  public int insertInDb(NetCompound comp) {
    int chemId = getNextId();

    comp.setChemId(chemId);
    comp.setPendingCurationDbMap(false);
    comp.setPendingCurationXrefMap(false);

    if (comp.getOfn() == null) {
      if (comp.getSbmlName() != null) {
        insertCompound(chemId, comp.getSbmlName(), comp.getCharge(), comp.getMapping());
      }
      else {
        insertCompound(chemId, comp.getSbmlId(), comp.getCharge(), comp.getMapping());
      }
    }
    else {
      insertCompound(chemId, comp.getOfn(), comp.getCharge(), comp.getMapping());
    }

    if (comp.isExchangeCompound()) {
      exchangeReaction(chemId);
    }

    // Sinonimos
    for (String syn : comp.getSynList()) {
      insertCompSyn(chemId, syn);
    }

    // Xrefs (incluye xrefs, inchi, smile)
    Set<String> xrefDbList = new HashSet<String>(); // Cache

    Map<String,Set<String>> xrefList = comp.getXrefList();
    for (String source : xrefList.keySet()) {
      Set<String> set = xrefList.get(source);
      for (String xref : set) {
        insertCompXref(chemId, source, xref);
        xrefDbList.add(xref);
      }
    }

    if (comp.getInchi() != null &&
            !xrefDbList.contains(comp.getInchi())) {
      insertCompXref(chemId, Util.SOURCE_STRUCTURE, comp.getInchi());
      xrefDbList.add(comp.getInchi());
    }
    if (comp.getInchikey() != null &&
            !xrefDbList.contains(comp.getInchikey())) {
      insertCompXref(chemId, Util.SOURCE_STRUCTURE, comp.getInchikey());
      xrefDbList.add(comp.getInchikey());
    }
    for (String sm : comp.getSmilesList()) {
      if (!xrefDbList.contains(sm)) {
        insertCompXref(chemId, Util.SOURCE_STRUCTURE, sm);
        xrefDbList.add(sm);
      }
    }

    // Formulas
    Set<String> fmDbList = new HashSet<String>(); // Cache

    if (comp.getSbmlFormula() != null) {
      insertCompFm(chemId, Util.SOURCE_FORMULA_SBML, comp.getSbmlFormula(), comp.getCharge());
      fmDbList.add(comp.getSbmlFormula());
    }
    if (comp.getNeutralFormula() != null) {
      insertCompFm(chemId, Util.SOURCE_FORMULA_NEUTRAL, comp.getNeutralFormula(), 0);
      fmDbList.add(comp.getNeutralFormula());
    }

    Map<String,Set<Fm>> fmList = comp.getFmList();
    for (String source : fmList.keySet()) {
      Set<Fm> set = fmList.get(source);
      for (Fm fm : set) {
        if (!fmDbList.contains(fm.getFm())) {
          insertCompFm(chemId, source, fm.getFm(), fm.getCharge());
          fmDbList.add(fm.getFm());
        }
      }
    }

    return chemId;
  }

  /**
   * Metodo interno que inserta nuevos datos del compuesto en la base de datos.
   * @param chemId
   * @param comp
   */
  public void updateDb(int chemId, NetCompound comp) {
    comp.setChemId(chemId);

    comp.setPendingCurationDbMap(false);
    comp.setPendingCurationXrefMap(false);

    if (comp.isExchangeCompound()) {
      exchangeReaction(chemId);
    }

    // Sinonimos
    Set<String> synDbList = getSynList(chemId); // Cache

    if (comp.getOfn() != null &&
            !synDbList.contains(comp.getOfn())) {
      insertCompSyn(chemId, comp.getOfn());
      synDbList.add(comp.getOfn());
    }
    if (comp.getSbmlName() != null &&
            !synDbList.contains(comp.getSbmlName())) {
      insertCompSyn(chemId, comp.getSbmlName());
      synDbList.add(comp.getSbmlName());
    }

    for (String syn : comp.getSynList()) {
      if (!synDbList.contains(syn)) {
        insertCompSyn(chemId, syn);
        synDbList.add(syn);
      }
    }

    // Xrefs (incluye nombres, inchi, smile)
    Set<String> xrefDbList = getXrefList(chemId); // Cache

    if (comp.getSbmlId() != null &&
            !xrefDbList.contains(comp.getCleanSbmlId())) {
      insertCompXref(chemId, Util.SOURCE_SBML, comp.getCleanSbmlId());
      xrefDbList.add(comp.getCleanSbmlId());
    }
    for (String chebiId : comp.getChebiIdList()) {
      if (!xrefDbList.contains(chebiId)) {
        insertCompXref(chemId, Util.SOURCE_CHEBI, chebiId);
        xrefDbList.add(chebiId);
      }
    }
    for (String keggId : comp.getKeggIdList()) {
      if (!xrefDbList.contains(keggId)) {
        insertCompXref(chemId, Util.SOURCE_KEGG, keggId);
        xrefDbList.add(keggId);
      }
    }
    if (comp.getInchi() != null &&
            !xrefDbList.contains(comp.getInchi())) {
      insertCompXref(chemId, Util.SOURCE_STRUCTURE, comp.getInchi());
      xrefDbList.add(comp.getInchi());
    }
    for (String sm : comp.getSmilesList()) {
      if (!xrefDbList.contains(sm)) {
        insertCompXref(chemId, Util.SOURCE_STRUCTURE, sm);
        xrefDbList.add(sm);
      }
    }

    // Formulas
    Set<String> fmDbList = getFmList(chemId); // Cache

    if (comp.getSbmlFormula() != null &&
            !fmDbList.contains(comp.getSbmlFormula())) {
      insertCompFm(chemId, Util.SOURCE_FORMULA_SBML, comp.getSbmlFormula(), comp.getCharge());
      fmDbList.add(comp.getSbmlFormula());
    }
    if (comp.getNeutralFormula() != null &&
            !fmDbList.contains(comp.getNeutralFormula())) {
      insertCompFm(chemId, Util.SOURCE_FORMULA_NEUTRAL, comp.getNeutralFormula(), 0);
      fmDbList.add(comp.getNeutralFormula());
    }

    incrementFrequency(chemId);
  }

  private void loadAndInsertCurationFile() {
    XStream xstream = new XStream(new StaxDriver());

    BufferedReader br = null;
    try {
      String sCurrentLine;

      br = new BufferedReader(new FileReader(Util.USERDIR_MANFILE));
      while ((sCurrentLine = br.readLine()) != null) {
        if (sCurrentLine.startsWith("#") || sCurrentLine.trim().isEmpty())
          continue;

        // sbmlId TAB date TAB sbmlName TAB ofn TAB carga TAB mapping TAB exchange TAB
        // xml_syn TAB xml_xref TAB xml_fm TAB EOR
        String[] reg = sCurrentLine.split(Util.TAB);
        String sbmlId = reg[0];
        String name = reg[2];
        String ofn = reg[3];
        int charge = Integer.parseInt(reg[4]);
        int mapping = Util.MAPPING_MANFILE; //Integer.parseInt(reg[5]);
        boolean exchange = Boolean.parseBoolean(reg[6]);

        // Xrefs (IDs sbml)
        Set<Integer> chemIdList = new HashSet<Integer>();

        @SuppressWarnings("unchecked")
        Map<String,Set<String>> xrefList = (Map<String, Set<String>>) xstream.fromXML(reg[8]);
        for (String source : xrefList.keySet()) {
          Set<String> set = xrefList.get(source);
          if (source.equals(Util.SOURCE_SBML)) {
            for (String xref : set) {
              chemIdList.addAll(getChemIdForSbmlId(xref));
            }
          }
        }


        int lsize = chemIdList.size();

        // No existe referencia => Insert
        if (lsize == 0) {
          int chemId = getNextId();

          if (ofn.isEmpty()) {
            if (name.isEmpty()) {
              insertCompound(chemId, sbmlId, charge, mapping);
            }
            else {
              insertCompound(chemId, name, charge, mapping);
            }
          }
          else {
            insertCompound(chemId, ofn, charge, mapping);
          }

          if (exchange) {
            exchangeReaction(chemId);
          }

          // Sinonimos
          @SuppressWarnings("unchecked")
          Set<String> synList = (Set<String>) xstream.fromXML(reg[7]);
          for (String syn : synList) {
            insertCompSyn(chemId, syn);
          }

          // Xrefs (incluye xrefs, inchi, smile)
          for (String source : xrefList.keySet()) {
            Set<String> set = xrefList.get(source);
            for (String xref : set) {
              insertCompXref(chemId, source, xref);
            }
          }

          // Formulas
          @SuppressWarnings("unchecked")
          Map<String,Set<Fm>> fmList = (Map<String, Set<Fm>>) xstream.fromXML(reg[9]);
          for (String source : fmList.keySet()) {
            Set<Fm> set = fmList.get(source);
            for (Fm fm : set) {
              insertCompFm(chemId, source, fm.getFm(), fm.getCharge());
            }
          }
        }

        // Existe referencia => Update
        else if (lsize == 1) {
          int chemId = chemIdList.iterator().next();

          if (exchange) {
            exchangeReaction(chemId);
          }

          // Sinonimos
          Set<String> synDbList = getSynList(chemId); // Cache

          if (ofn != null && !synDbList.contains(ofn)) {
            insertCompSyn(chemId, ofn);
            synDbList.add(ofn);
          }
          if (name != null && !synDbList.contains(name)) {
            insertCompSyn(chemId, name);
            synDbList.add(name);
          }

          @SuppressWarnings("unchecked")
          Set<String> synList = (Set<String>) xstream.fromXML(reg[7]);
          for (String syn : synList) {
            if (!synDbList.contains(syn)) {
              insertCompSyn(chemId, syn);
              synDbList.add(syn);
            }
          }

          // Xrefs (incluye nombres, inchi, smile)
          Set<String> xrefDbList = getXrefList(chemId); // Cache

          if (sbmlId != null && !xrefDbList.contains(sbmlId)) {
            insertCompXref(chemId, Util.SOURCE_SBML, sbmlId);
            xrefDbList.add(sbmlId);
          }

          for (String source : xrefList.keySet()) {
            Set<String> set = xrefList.get(source);
            for (String xref : set) {
              if (!xrefDbList.contains(xref)) {
                insertCompXref(chemId, source, xref);
                xrefDbList.add(xref);
              }
            }
          }

          // Formulas
          Set<String> fmDbList = getFmList(chemId); // Cache

          @SuppressWarnings("unchecked")
          Map<String,Set<Fm>> fmList = (Map<String, Set<Fm>>) xstream.fromXML(reg[9]);
          for (String source : fmList.keySet()) {
            Set<Fm> set = fmList.get(source);
            for (Fm fm : set) {
              if (!fmDbList.contains(fm.getFm())) {
                insertCompFm(chemId, source, fm.getFm(), fm.getCharge());
                fmDbList.add(fm.getFm());
              }
            }
          }

          incrementFrequency(chemId);
        }

        // Mas de una referencia (error)
        else {
          LOGGER.log(Level.SEVERE, "Several references to db: {0}", chemIdList);
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
}
