package es.csic.cnb.db;

import java.net.BindException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.tools.Server;

import es.csic.cnb.util.Util;

public enum DbManager {
  INSTANCE;

  private final static Logger LOGGER = Logger.getLogger(DbManager.class.getName());

  private Server server;

  private static final String DB = Util.DB;
  private static final String USER = "user";
  private static final String PWD = "user";

  private static final String DBNAME_1 = "/db/compound";
  private static final String DBNAME_2 = "/db/kegg";
  private static final String DBNAME_3 = "/db/seed";

  private JdbcConnectionPool cp1;
  private JdbcConnectionPool cp2;
  private JdbcConnectionPool cp3;

  private CompoundDbManager compDbMgr;
  private KeggDbManager keggDbMgr;
  private SeedDbManager seedDbMgr;

  private boolean isConnected = false;

  public void connect() {
	//LOGGER.log(Level.INFO, "1. " + server);
	  
	
	  
    if (!isConnected) {
      //LOGGER.log(Level.INFO, "2. Not connected: " + server);
      
      // start the TCP Server
      try {
        if (server == null) {
          server = Server.createTcpServer(
                  new String[] { "-tcpPort", String.valueOf(Util.DB_PORT), "-tcpAllowOthers" });
          
          //LOGGER.log(Level.INFO, "4. createTcpServer: " + server);
        }
        if (!server.isRunning(false)) {
          server.start();

          //LOGGER.log(Level.INFO, "5. server not running - start: " + server);
          LOGGER.log(Level.INFO, server.getStatus());
        }

      } catch (SQLException e) {
        // Address already in use
        if (e.getCause() instanceof BindException) {
          String msg = e.getCause().getMessage();
          
          //LOGGER.log(Level.INFO, "6. BindException: " + server);
          
          if (server != null)
            msg += " at " + server.getURL();

          //LOGGER.log(Level.INFO, "7. BindException - server not null: " + server);
          LOGGER.log(Level.INFO, msg);
        }
        
        //LOGGER.log(Level.INFO, "8. BindException: " + server);
      }

      // Crear pools de conexiones a las bases de datos
      cp1 = JdbcConnectionPool.create(DB + Util.USERDIR + DBNAME_1, USER, PWD);
      compDbMgr = new CompoundDbManager(cp1);
      LOGGER.log(Level.INFO, "Connect "+ DB + Util.USERDIR + DBNAME_1);

      cp2 = JdbcConnectionPool.create(DB + Util.USERDIR + DBNAME_2, USER, PWD);
      keggDbMgr = new KeggDbManager(cp2);
      LOGGER.log(Level.INFO, "Connect "+ DB + Util.USERDIR + DBNAME_2);

      cp3 = JdbcConnectionPool.create(DB + Util.USERDIR + DBNAME_3, USER, PWD);
      seedDbMgr = new SeedDbManager(cp3);
      LOGGER.log(Level.INFO, "Connect "+ DB + Util.USERDIR + DBNAME_3);

      isConnected = true;
    }
  }

  public boolean isConnected() {
    return isConnected;
  }

  /**
   * Cierra el pool de conexiones.
   */
  public void closeDbConections() {
    if (isConnected) {
      cp1.dispose();
      cp2.dispose();
      cp3.dispose();
      
      cp1 = null;
      cp2 = null;
      cp3 = null;

      isConnected = false;
    }
  }

  /**
   * Cierra el servidor.
   */
  public void shutdown() {
    server.stop();
    server.shutdown();
  }

  public CompoundDbManager getCompoundDbMgr() {
    return compDbMgr;
  }

  public KeggDbManager getKeggDbMgr() {
    return keggDbMgr;
  }

  public SeedDbManager getSeedDbMgr() {
    return seedDbMgr;
  }

  public void createAllTables(boolean loadCurationFile) {
    try {
      getCompoundDbMgr().createTables(loadCurationFile);
      getKeggDbMgr().createTables();
      getSeedDbMgr().createTables();
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  public void dropAllTables() {
    try {
      getCompoundDbMgr().dropTables();
      getKeggDbMgr().dropTables();
      getSeedDbMgr().dropTables();
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  public void createCompTables(boolean loadCurationFile) {
    try {
      getCompoundDbMgr().createTables(loadCurationFile);
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  public void dropCompTables() {
    try {
      getCompoundDbMgr().dropTables();
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }
}
