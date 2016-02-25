package es.csic.cnb.rmi.server;

import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import es.csic.cnb.rmi.RMIInterfaz;
import es.csic.cnb.util.Util;

public class RMIServer {
  private static Registry registry;

  public static void main(String[] args) {
    try {
      String name = RMIInterfaz.class.getSimpleName();
      Remote remoteObj = new RMIInterfazImpl();

      registry = LocateRegistry.createRegistry(Util.RMI_PORT);

      // create a new service
      registry.rebind(name, remoteObj);

      System.out.println("Registered: " + name + " -> " + remoteObj.getClass().getName());

    } catch (Exception e) {
      e.printStackTrace();
    }

    System.out.println("RMI Server is ready.");

//    // Detectar System.exit()
//    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
//      public void run() {
//        // Do what you want when the application is stopping
//        System.out.println("CERRANDO..........");
//      }
//    }));
  }
}
