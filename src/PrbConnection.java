import java.net.BindException;
import java.sql.SQLException;

import org.h2.tools.Server;


public class PrbConnection {

  public static void main (String[] args) {
    Server server = null;
    try {
      server = Server.createTcpServer(new String[] { "-tcpPort", "9123", "-tcpAllowOthers" });

      System.out.println();
      System.out.println(">Connecting...");
      System.out.println("SERVER PORT: "+server.getPort());
      System.out.println("SERVER URL: "+server.getURL());
      System.out.println("SERVER ST: "+server.getStatus());
      System.out.println("SERVER RUN: "+server.isRunning(false));

      server.start();

      System.out.println(">Start...");
      System.out.println("SERVER PORT: "+server.getPort());
      System.out.println("SERVER URL: "+server.getURL());
      System.out.println("SERVER ST: "+server.getStatus());
      System.out.println("SERVER RUN: "+server.isRunning(false));

    } catch (SQLException e) {
      e.printStackTrace();

      Throwable t = e.getCause();
      // Address already in use
      if (t instanceof BindException) {
        System.out.println(t.getMessage());
        System.out.println(e.getCause().getMessage() + " at " + server.getURL());
      }
    }

    while(true) {}

    //System.out.println("STOP!!!!!!");
    //server.stop();
  }
}
