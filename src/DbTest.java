import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import es.csic.cnb.util.Util;


public class DbTest {
  private static final String DRIVER = "org.h2.Driver";
  //private static final String DB = "jdbc:h2:";
  private static final String DB = "jdbc:h2:tcp://localhost/";
  private static final String DBNAME = "/db/chemNames";
  private static final String USER = "user";
  private static final String PWD = "user";
  
  private Connection conn;
  private PreparedStatement pstmt;
  
  public void connect() {
    try {
      Class.forName(DRIVER);
      conn = DriverManager.getConnection(DB + Util.USERDIR + DBNAME, USER, PWD);

    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public void close() {
    try {
      if (pstmt != null) pstmt.close();
      conn.close();
    
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
  
  
//  public List<Integer> getChemIdListByXref(String xref, String source) {
//    List<Integer> list = new ArrayList<Integer>();
//
//    try {
//      pstmt = conn.prepareStatement( 
//              "SELECT CHEMID FROM COMPXREF " +
//              "WHERE XREF = ? " +
//              "AND SOURCE = ?");
//      pstmt.setString(1, xref);
//      pstmt.setString(2, source);
//      
//      ResultSet result = pstmt.executeQuery();
//      while(result.next()) {
//        list.add(result.getInt(CHEMID));
//      }
//    } catch (SQLException e) {
//      LOGGER.log(Level.SEVERE, e.getMessage(), e);
//    }
//    
//    return list;
//  }
  
  
  
  public static void main(String[] args) {
    DbTest db = new DbTest();
    db.connect();
    
    
    db.close();
  }
}
