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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.h2.jdbcx.JdbcConnectionPool;

import es.csic.cnb.util.Util;

public class KeggDbManager {
  private final static Logger LOGGER = Logger.getLogger(KeggDbManager.class.getName());

  private static final String TAB = "\t";

  private JdbcConnectionPool cp;

  public KeggDbManager(JdbcConnectionPool cp) {
    this.cp = cp;
  }

  public void createTables() throws SQLException {
    Statement stmt = null;
    Connection conn = null;
    try {
      conn = cp.getConnection();
      stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE KEGG_COMPOUND" +
              "(ID VARCHAR(8) PRIMARY KEY, " +
              "FM VARCHAR(5012)," +
              "MASS DOUBLE);");
      stmt.executeUpdate("CREATE TABLE KEGG_SYN" +
              "(ID VARCHAR(8), " +
              "SYN VARCHAR_IGNORECASE(1024), " +
              "FOREIGN KEY(ID) " +
              "REFERENCES KEGG_COMPOUND(ID) ON DELETE CASCADE);");
      stmt.executeUpdate("CREATE TABLE KEGG_ST" +
              "(ID VARCHAR(8), " +
              "ST VARCHAR_IGNORECASE(1024), " +
              "SOURCE VARCHAR(32), " +
              "FOREIGN KEY(ID) " +
              "REFERENCES KEGG_COMPOUND(ID) ON DELETE CASCADE);");
      stmt.executeUpdate("CREATE TABLE KEGG_REACTION" +
              "(ID VARCHAR(8), " +
              "REACTION VARCHAR(8), " +
              "FOREIGN KEY(ID) " +
              "REFERENCES KEGG_COMPOUND(ID) ON DELETE CASCADE);");
      stmt.executeUpdate("CREATE TABLE KEGG_EC" +
              "(ID VARCHAR(8), " +
              "EC VARCHAR(16), " +
              "FOREIGN KEY(ID) " +
              "REFERENCES KEGG_COMPOUND(ID) ON DELETE CASCADE);");
      stmt.executeUpdate("CREATE TABLE KEGG_XREF" +
              "(ID VARCHAR(8), " +
              "XREF VARCHAR(64), " +
              "SOURCE VARCHAR(64), " +
              "FOREIGN KEY(ID) " +
              "REFERENCES KEGG_COMPOUND(ID) ON DELETE CASCADE);");
      stmt.executeUpdate("CREATE TABLE KEGG_PATHWAY" +
              "(ID VARCHAR(8), " +
              "PATHWAY VARCHAR(8), " +
              "DESCRIPTION VARCHAR(1024), " +
              "FOREIGN KEY(ID) " +
              "REFERENCES KEGG_COMPOUND(ID) ON DELETE CASCADE);");
    }
    finally {
      if (stmt != null)
        stmt.close();
      if (conn != null)
        conn.close();
    }


    // Cargar datos una vez creadas la tablas
    loadData();
  }

  public void dropTables() throws SQLException {
    Statement stmt = null;
    Connection conn = null;
    try {
      conn = cp.getConnection();
      stmt = conn.createStatement();
      stmt.executeUpdate("DROP TABLE IF EXISTS KEGG_COMPOUND;");
      stmt.executeUpdate("DROP TABLE IF EXISTS KEGG_SYN;");
      stmt.executeUpdate("DROP TABLE IF EXISTS KEGG_ST;");
      stmt.executeUpdate("DROP TABLE IF EXISTS KEGG_REACTION;");
      stmt.executeUpdate("DROP TABLE IF EXISTS KEGG_EC;");
      stmt.executeUpdate("DROP TABLE IF EXISTS KEGG_XREF;");
      stmt.executeUpdate("DROP TABLE IF EXISTS KEGG_PATHWAY;");
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

      Map<String,ExkeggData> dataMap = new HashMap<String,ExkeggData>();

      br = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/resources/keggexdb.txt")));
      while ((sCurrentLine = br.readLine()) != null) {
        if (sCurrentLine.startsWith("#"))
          continue;

        String[] reg = sCurrentLine.split(TAB);
        String id = reg[0].trim();
        String fm = Util.getFormulaCorrected(reg[1].trim());
        String[] names = reg[4].split("#");
        String[] smiles = reg[5].split("#");

        ExkeggData data = new ExkeggData(fm, reg[2], reg[3]);
        for (String s : names) {
          data.addName(s);
        }
        for (String s : smiles) {
          data.addSmiles(s);
        }
        dataMap.put(id, data);
      }

      br = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/resources/keggdb.txt")));
      while ((sCurrentLine = br.readLine()) != null) {
        if (sCurrentLine.startsWith("#"))
          continue;

        String[] reg = sCurrentLine.split(TAB);
        String id = reg[0].trim();
        String fm = Util.getFormulaCorrected(reg[1].trim());
        double mass = (reg[7].isEmpty()) ? 0.0 : Double.parseDouble(reg[7]);
        this.insertCompound(id, fm, mass);

        ExkeggData data = dataMap.get(id);
        if (data != null) {
          if (data.getFm().equals(fm)) {
            // Sinonimos
            Set<String> names = data.getNameList();
            String[] synList = reg[2].split(";");
            for (String syn : synList) {
              names.add(syn);
            }
            for (String syn : names) {
              this.insertCompSyn(id, syn);
            }

            // Sts
            Set<String> smiles = data.getSmilesList();
            for (String st : smiles) {
              this.insertStructure(id, Util.SOURCE_ST_SMILES, st);
            }
            if (data.getInchi() != null && !data.getInchi().isEmpty()) {
              this.insertStructure(id, Util.SOURCE_ST_INCHI, data.getInchi());
            }
            if (data.getInchikey() != null && !data.getInchikey().isEmpty()) {
              this.insertStructure(id, Util.SOURCE_ST_INCHIKEY, data.getInchikey());
            }
          }
          else {
            System.err.println("NO COINCIDE fm: "+fm+" - "+data.getFm()+" para "+id);
          }
        }
        else {
          // Sinonimos
          if (!reg[2].isEmpty()) {
            String[] synList = reg[2].split(";");
            for (String syn : synList) {
              this.insertCompSyn(id, syn);
            }
          }
        }

        // Reacciones
        if (!reg[3].isEmpty()) {
          String[] reactionList = reg[3].split(";");
          for (String reaction : reactionList) {
            this.insertReaction(id, reaction);
          }
        }

        // Enzimas
        if (!reg[4].isEmpty()) {
          String[] ecList = reg[4].split(";");
          for (String ec : ecList) {
            this.insertEc(id, ec);
          }
        }

        // xrefPair -> DB|id
        if (!reg[5].isEmpty()) {
          String[] xrefList = reg[5].split(";");
          for (String xrefPair : xrefList) {
            String[] pair = xrefPair.split("\\|");
            this.insertXref(id, pair[0], pair[1]);
          }
        }

        // pathPair -> id|description
        if (!reg[6].isEmpty()) {
          String[] pathList = reg[6].split(";");
          for (String pathPair : pathList) {
            String[] pair = pathPair.split("\\|");
            this.insertPathway(id, pair[0], pair[1]);
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


  /**
   * Recupera la formula para un keggId.
   *
   * @param keggId
   * @return formula
   */
  public String getFormula(String id) {
    String formula = null;

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT FM FROM KEGG_COMPOUND " +
              "WHERE ID = ?");
      pstmt.setString(1, id);

      int cont = 0;
      result = pstmt.executeQuery();
      while(result.next()) {
        formula = result.getString(1);
        if (formula.isEmpty())
          formula = null;
        cont++;
      }

      // Verificar que no hay o solo hay una formula
      assert cont <= 1 : "Mas de una formula [keggId: " + id + "]";

    }
    catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
    finally {
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
   * Recupera la masa (Mol weight) para un keggId.
   *
   * @param keggId
   * @return mass
   */
  public double getMass(String id) {
    double mass = 0.0;

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT MASS FROM KEGG_COMPOUND " +
              "WHERE ID = ?");
      pstmt.setString(1, id);

      int cont = 0;
      result = pstmt.executeQuery();
      while(result.next()) {
        mass = result.getDouble(1);
        cont++;
      }
      // Verificar que no hay o solo hay una formula
      assert cont <= 1 : "Mas de una masa [keggId: " + id + "]";

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

    return mass;
  }

  /**
   * Recupera la lista de sinonimos para un keggId.
   *
   * @param keggId
   * @return lista de sinonimos
   */
  public List<String> getSynList(String id) {
    List<String> list = new ArrayList<String>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT SYN FROM KEGG_SYN " +
              "WHERE ID = ?");
      pstmt.setString(1, id);

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
   * Recupera la lista de smiles para un keggId.
   *
   * @param keggId
   * @return lista de smiles
   */
  public List<String> getSmilesList(String id) {
    List<String> list = new ArrayList<String>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT ST FROM KEGG_ST " +
              "WHERE ID = ? " +
              "AND SOURCE = ?");
      pstmt.setString(1, id);
      pstmt.setString(2, Util.SOURCE_ST_SMILES);

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
   * Recupera el inchi para un keggId.
   *
   * @param keggId
   * @return inchi
   */
  public String getInchi(String id) {
    String inchi = null;

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT ST FROM KEGG_ST " +
              "WHERE ID = ?" +
      "AND SOURCE = ?");
      pstmt.setString(1, id);
      pstmt.setString(2, Util.SOURCE_ST_INCHI);

      int cont = 0;
      result = pstmt.executeQuery();
      while(result.next()) {
        inchi = result.getString(1);
        if (inchi.isEmpty())
          inchi = null;
        cont++;
      }
      // Verificar que no hay o solo hay un inchi
      assert cont <= 1 : "Mas de un inchi [keggId: " + id + "]";

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

    return inchi;
  }

  /**
   * Recupera el inchikey para un keggId.
   *
   * @param keggId
   * @return inchikey
   */
  public String getInchikey(String id) {
    String inchi = null;

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT ST FROM KEGG_ST " +
              "WHERE ID = ?" +
              "AND SOURCE = ?");
      pstmt.setString(1, id);
      pstmt.setString(2, Util.SOURCE_ST_INCHIKEY);

      int cont = 0;
      result = pstmt.executeQuery();
      while(result.next()) {
        inchi = result.getString(1);
        if (inchi.isEmpty())
          inchi = null;
        cont++;
      }
      // Verificar que no hay o solo hay un inchikey
      assert cont <= 1 : "Mas de un inchikey [keggId: " + id + "]";

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

    return inchi;
  }

  /**
   * Recupera los chebiId para un keggId.
   *
   * @param keggId
   * @return lista de chebiId
   */
  public List<String> getChebiIdList(String id) {
    List<String> chebiIdList = new ArrayList<String>();

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "SELECT XREF FROM KEGG_XREF " +
              "WHERE ID = ? AND SOURCE = 'ChEBI'");
      pstmt.setString(1, id);

      result = pstmt.executeQuery();
      while(result.next()) {
        chebiIdList.add(result.getString(1));
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

    return chebiIdList;
  }

  public void insertCompound(String id, String fm, double mass) {
    Connection conn = null;
    PreparedStatement pstmt = null;
    try {
      conn = cp.getConnection();
      pstmt = conn.prepareStatement(
              "INSERT INTO KEGG_COMPOUND (ID,FM, MASS) VALUES (?,?,?)");
      pstmt.setString(1, id);
      pstmt.setString(2, fm);
      pstmt.setDouble(3, mass);
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

  public void insertCompSyn(String id, String syn) {
    if (syn != null) {
      Connection conn = null;
      PreparedStatement pstmt = null;
      try {
        conn = cp.getConnection();
        pstmt = conn.prepareStatement(
                "INSERT INTO KEGG_SYN (ID,SYN) VALUES (?,?)");
        pstmt.setString(1, id);
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

  public void insertStructure(String id, String source, String st) {
    if (st != null) {
      Connection conn = null;
      PreparedStatement pstmt = null;
      try {
        conn = cp.getConnection();
        pstmt = conn.prepareStatement(
                "INSERT INTO KEGG_ST (ID,ST,SOURCE) VALUES (?,?,?)");
        pstmt.setString(1, id);
        pstmt.setString(2, st);
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

  public void insertReaction(String id, String reaction) {
    if (reaction != null) {
      Connection conn = null;
      PreparedStatement pstmt = null;
      try {
        conn = cp.getConnection();
        pstmt = conn.prepareStatement(
                "INSERT INTO KEGG_REACTION (ID,REACTION) VALUES (?,?)");
        pstmt.setString(1, id);
        pstmt.setString(2, reaction);
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

  public void insertEc(String id, String ec) {
    if (ec != null) {
      Connection conn = null;
      PreparedStatement pstmt = null;
      try {
        conn = cp.getConnection();
        pstmt = conn.prepareStatement("INSERT INTO KEGG_EC (ID,EC) VALUES (?,?)");
        pstmt.setString(1, id);
        pstmt.setString(2, ec);
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

  public void insertXref(String id, String source, String xref) {
    if (xref != null) {
      Connection conn = null;
      PreparedStatement pstmt = null;
      try {
        conn = cp.getConnection();
        pstmt = conn.prepareStatement("INSERT INTO KEGG_XREF (ID,XREF,SOURCE) VALUES (?,?,?)");
        pstmt.setString(1, id);
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

  public void insertPathway(String id, String pathway, String description) {
    if (pathway != null) {
      Connection conn = null;
      PreparedStatement pstmt = null;
      try {
        conn = cp.getConnection();
        pstmt = conn.prepareStatement("INSERT INTO KEGG_PATHWAY (ID,PATHWAY,DESCRIPTION) VALUES (?,?,?)");
        pstmt.setString(1, id);
        pstmt.setString(2, pathway);
        pstmt.setString(3, description);
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
  
  public void scriptTo() throws SQLException {
	    Statement stmt = null;
	    Connection conn = null;
	    try {
	      conn = cp.getConnection();
	      stmt = conn.createStatement();
	      stmt.executeQuery("SCRIPT DROP TO '/home/pdsanchez/CNB/cmodel/db/sql/bkp_kegg.sql'");
	    }
	    finally {
	      if (stmt != null)
	        stmt.close();
	      if (conn != null)
	        conn.close();
	    }
	  }


  /**
   *
   *
   * @author Pablo D. Sánchez (pd.sanchez@cnb.csic.es)
   */
  class ExkeggData {
    private String fm;
    private String inchi;
    private String inchikey;
    private Set<String> nameList;
    private Set<String> smilesList;

    public ExkeggData() {}

    public ExkeggData(String fm, String inchi, String inchikey) {
      this.fm = fm;
      this.inchi = inchi;
      this.inchikey = inchikey;

      nameList = new HashSet<String>();
      smilesList = new HashSet<String>();
    }

    /**
     * @return the fm
     */
    public String getFm() {
      return fm;
    }

    /**
     * @param fm the fm to set
     */
    public void setFm(String fm) {
      this.fm = fm;
    }

    /**
     * @return the inchi
     */
    public String getInchi() {
      return inchi;
    }

    /**
     * @param inchi the inchi to set
     */
    public void setInchi(String inchi) {
      this.inchi = inchi;
    }

    /**
     * @return the inchikey
     */
    public String getInchikey() {
      return inchikey;
    }

    /**
     * @param inchikey the inchikey to set
     */
    public void setInchikey(String inchikey) {
      this.inchikey = inchikey;
    }

    /**
     * @return the nameList
     */
    public Set<String> getNameList() {
      return nameList;
    }

    /**
     * @param name the name to add
     */
    public void addName(String name) {
      nameList.add(name);
    }

    /**
     * @return the smilesList
     */
    public Set<String> getSmilesList() {
      return smilesList;
    }

    /**
     * @param smiles the smiles to add
     */
    public void addSmiles(String smiles) {
      smilesList.add(smiles);
    }
  }
}
