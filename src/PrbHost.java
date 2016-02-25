import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

import es.csic.cnb.util.Util;


public class PrbHost {

  /**
   * @param args
   */
  public static void main(String[] args) {
    // TODO Auto-generated method stub
    try {
      System.out.println(InetAddress.getLocalHost().getCanonicalHostName());
      System.out.println(InetAddress.getLocalHost().getHostName());
      System.out.println(InetAddress.getLocalHost().getHostAddress());

      System.out.println();
      System.out.println(Util.RMI_HOST);
      InetAddress addr = InetAddress.getByName(Util.RMI_HOST);
      String host = addr.getHostName();
      System.out.println(host);

      System.out.println();
      InetAddress[] addrs = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
      for (InetAddress a : addrs) {
        System.out.println(Arrays.toString(a.getAddress()));
      }

      System.out.println();
      Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
      for (NetworkInterface iface : Collections.list(ifaces)) {
        System.out.println("Real iface addresses: " + iface.getDisplayName());
        Enumeration<InetAddress> raddrs = iface.getInetAddresses();
        for (InetAddress raddr : Collections.list(raddrs)) {
          System.out.println("\t" + raddr.getHostName());
          System.out.println("\t" + raddr.getHostAddress());
          System.out.println();
        }
      }
    } catch (UnknownHostException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SocketException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}
