package es.csic.cnb.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public enum PropertiesMgr {
  INSTANCE;

  private final static Logger LOGGER = Logger.getLogger(PropertiesMgr.class.getName());

  private static final String PROPFILE = "cmodel.properties";

  private static Properties props;

  static {
    props = new Properties();
    try {
      props.load(PropertiesMgr.class.getResourceAsStream("/configuration/" + PROPFILE));

      String[] paths = props.getProperty("cmodel.path").split(",");
      for (String s : paths) {
        s = s.trim();
        File parent = new File(s);
        if (parent.exists()) {
          props.setProperty("cmodel.path", s);
          File f = new File(parent, PROPFILE);
          if (f.exists()) {
            InputStream fin = new FileInputStream(f);
            props.load(fin);
          }
          else {
            props.storeToXML(new FileOutputStream(f), "cmodel properties");
          }
          break;
        }
      }

      automaticHost();

      //String path = PropertiesMgr.class.getProtectionDomain().getCodeSource().getLocation().getPath();
      //File f = new File(path).getParentFile();
      //f = new File(f, PROPFILE);

    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  public String getLevel() {
    return props.getProperty("log.level", "OFF");
  }

  public String getFilelogLevel() {
    return props.getProperty("log.fh.level", "OFF");
  }

  public String getConsoleLevel() {
    return props.getProperty("log.ch.level", "OFF");
  }

  public String getCmodelPath() {
    return props.getProperty("cmodel.path", System.getProperty("user.dir"));
  }

  public int getRMIPort() {
    String port = props.getProperty("rmi.port", "1099");
    return Integer.parseInt(port);
  }

  public String getRMIHost() {
    return props.getProperty("rmi.host", "localhost");
  }

  public int getDbPort() {
    String port = props.getProperty("db.port", "8082");
    return Integer.parseInt(port);
  }

  public String getDbHost() {
    return props.getProperty("db.host", "localhost");
  }

  /**
   * Metodo que cambia el host segun la maquina en la que corra la app.
   * Solo se ejecuta si "automatic.host = true"
   */
  private static void automaticHost() {
    boolean isAuto = Boolean.parseBoolean(props.getProperty("automatic.host", "false"));
    if (isAuto) {
      try {
        NetworkInterface iface = NetworkInterface.getByName("eth0");
        Enumeration<InetAddress> raddrs = iface.getInetAddresses();
        for (InetAddress raddr : Collections.list(raddrs)) {
          if (!raddr.isLinkLocalAddress()) {
            props.setProperty("rmi.host", raddr.getHostAddress());
            props.setProperty("db.host", raddr.getHostAddress());

            LOGGER.log(Level.INFO, "Autohost: {0} - {1}", new Object[] {raddr.getHostName(), raddr.getHostAddress()});
          }
        }
      } catch (SocketException e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
      }
    }
  }
}
