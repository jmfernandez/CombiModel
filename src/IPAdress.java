import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;


public class IPAdress {

  public static void main(String[] args) {
    try {
      System.out.println(InetAddress.getLocalHost().getHostName());
      System.out.println(InetAddress.getLocalHost().getHostAddress());


    } catch (UnknownHostException e) {
      e.printStackTrace();
    }

    try {
      NetworkInterface iface = NetworkInterface.getByName("eth0");
      Enumeration<InetAddress> raddrs = iface.getInetAddresses();
      for (InetAddress raddr : Collections.list(raddrs)) {
//        System.out.println("\t" + raddr.isAnyLocalAddress());
//        System.out.println("\t" + raddr.isLinkLocalAddress());
//        System.out.println("\t" + raddr.isLoopbackAddress());
//        System.out.println("\t" + raddr.isSiteLocalAddress());
//        System.out.println("\t" + raddr.isMulticastAddress());
//        System.out.println("\t" + raddr.isMCGlobal());
//        System.out.println("\t" + raddr.isMCLinkLocal());
//        System.out.println("\t" + raddr.isMCNodeLocal());
//        System.out.println("\t" + raddr.isMCOrgLocal());
//        System.out.println("\t" + raddr.isMCSiteLocal());
        if (!raddr.isLinkLocalAddress()) {
        System.out.println("\t" + raddr.getHostName());
        System.out.println("\t" + raddr.getHostAddress());
        System.out.println();
        }
      }
    } catch (SocketException e) {
      e.printStackTrace();
    }

  }

}
