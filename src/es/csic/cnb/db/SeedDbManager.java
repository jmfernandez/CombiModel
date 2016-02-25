package es.csic.cnb.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.h2.jdbcx.JdbcConnectionPool;

import es.csic.cnb.util.Util;

public class SeedDbManager {
  private final static Logger LOGGER = Logger.getLogger(SeedDbManager.class.getName());

  private static final String TAB = "\t";

  private JdbcConnectionPool cp;

  public SeedDbManager(JdbcConnectionPool cp) {
    this.cp = cp;
  }

  public void createTables() throws SQLException {
    Statement stmt = null;
    Connection conn = null;
    try {
      conn = cp.getConnection();
      stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE SEED_KEGG_FM" +
              "(SEEDID VARCHAR(8) PRIMARY KEY, " +
              "FM VARCHAR(64));");
      stmt.executeUpdate("CREATE TABLE SEED_KEGG_ID" +
              "(SEEDID VARCHAR(8), " +
              "KEGGID VARCHAR(8), " +
              "FOREIGN KEY(SEEDID) " +
              "REFERENCES SEED_KEGG_FM(SEEDID) ON DELETE CASCADE);");
      stmt.executeUpdate("CREATE TABLE SEED_KEGG_SYN" +
              "(SEEDID VARCHAR(8), " +
              "SYN VARCHAR_IGNORECASE(1024), " +
              "FOREIGN KEY(SEEDID) " +
              "REFERENCES SEED_KEGG_FM(SEEDID) ON DELETE CASCADE);");
    }
    finally {
      if (stmt != null)
        stmt.close();
      if (conn != null)
        conn.close();
    }

    // Cargar datos en las tablas
    loadData();
  }

  public void dropTables() throws SQLException {
    Statement stmt = null;
    Connection conn = null;
    try {
      conn = cp.getConnection();
      stmt = conn.createStatement();
      stmt.executeUpdate("DROP TABLE IF EXISTS SEED_KEGG_ID;");
      stmt.executeUpdate("DROP TABLE IF EXISTS SEED_KEGG_FM;");
      stmt.executeUpdate("DROP TABLE IF EXISTS SEED_KEGG_SYN;");
    }
    finally {
        if (stmt != null)
          stmt.close();
        if (conn != null)
          conn.close();
    }
  }

  private void loadData() {
    BufferedReader br = null;
    try {
      // Cargar tablas
      String sCurrentLine;
      br = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/resources/seed.txt")));
      while ((sCurrentLine = br.readLine()) != null) {
        if (sCurrentLine.startsWith("#"))
          continue;

        String[] reg = sCurrentLine.split(TAB);
        String seedId = reg[0].trim();
        String[] cidList = reg[1].split(",");
        String fm = Util.getFormulaCorrected(reg[2].trim());
        String[] synList = reg[3].split("#");

        this.insertSeedFormula(seedId, fm);
        for (String cid : cidList) {
          this.insertSeedPair(seedId, cid.trim());
        }
        for (String syn : synList) {
          this.insertSeedSyn(seedId, syn.trim());
        }
      }

    } catch (IOException e) {
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

  /**
   * Recupera la formula para un seedId.
   *
   * @param seedId
   * @return formula
   */
  public String getSeedFormula(String seedId) {
    String formula = null;

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT FM FROM SEED_KEGG_FM " +
              "WHERE SEEDID = ?");
      pstmt.setString(1, seedId);

      int cont = 0;
      result = pstmt.executeQuery();
      while(result.next()) {
        formula = result.getString(1);
        if (formula.isEmpty())
          formula = null;
        cont++;
      }
      // Verificar que no hay o solo hay una formula
      assert cont <= 1 : "Mas de una formula [seedId: " + seedId + "]";

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

    return formula;
  }

  /**
   * Recupera la lista de sinonimos para un seedId.
   *
   * @param seedId
   * @return lista de sinonimos
   */
  public List<String> getSeedSynList(String seedId) {
    List<String> list = new ArrayList<String>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT SYN FROM SEED_KEGG_SYN " +
              "WHERE SEEDID = ?");
      pstmt.setString(1, seedId);

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
   * Recupera el seedId asociado a un keggId.
   *
   * @param keggId
   * @return seedId
   */
  public String getSeedId(String keggId) {
    String seedId = null;

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT SEEDID FROM SEED_KEGG_ID " +
              "WHERE KEGGID = ?");
      pstmt.setString(1, keggId);

      int cont = 0;
      result = pstmt.executeQuery();
      while(result.next()) {
        seedId = result.getString(1);
        cont++;
      }
      // Verificar que no hay o solo hay una formula
      assert cont <= 1 : "Mas de un seedId [keggId: " + keggId + "]";

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

    return seedId;
  }

  /**
   * Recupera los keggId asociados a un seedId.
   *
   * @param seedId
   * @return lista con los identificadores de kegg
   */
  public List<String> getKeggIdList(String seedId) {
    List<String> list = new ArrayList<String>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT KEGGID FROM SEED_KEGG_ID " +
              "WHERE SEEDID = ?");
      pstmt.setString(1, seedId);

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

  public void insertSeedSyn(String seedId, String syn) {
    if (syn != null) {
      PreparedStatement pstmt = null;
      Connection conn = null;
      try {
        conn = cp.getConnection();
        pstmt = conn.prepareStatement(
                "INSERT INTO SEED_KEGG_SYN (SEEDID, SYN) VALUES (?,?)");
        pstmt.setString(1, seedId);
        pstmt.setString(2, syn);
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

  public void insertSeedFormula(String seedId, String fm) {
    if (fm != null) {
      PreparedStatement pstmt = null;
      Connection conn = null;
      try {
        conn = cp.getConnection();
        pstmt = conn.prepareStatement(
                "INSERT INTO SEED_KEGG_FM (SEEDID, FM) VALUES (?,?)");
        pstmt.setString(1, seedId);
        pstmt.setString(2, fm);
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


  public void insertSeedPair(String seedId, String keggId) {
    PreparedStatement pstmt = null;
    Connection conn = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "INSERT INTO SEED_KEGG_ID (SEEDID, KEGGID) VALUES (?,?)");
      pstmt.setString(1, seedId);
      pstmt.setString(2, keggId);
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
  
  public void scriptTo() throws SQLException {
	    Statement stmt = null;
	    Connection conn = null;
	    try {
	      conn = cp.getConnection();
	      stmt = conn.createStatement();
	      stmt.executeQuery("SCRIPT DROP TO '/home/pdsanchez/CNB/cmodel/db/sql/bkp_seed.sql'");
	    }
	    finally {
	      if (stmt != null)
	        stmt.close();
	      if (conn != null)
	        conn.close();
	    }
	  }
}
