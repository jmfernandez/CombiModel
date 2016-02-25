import uk.ac.ebi.chebi.webapps.chebiWS.client.ChebiWebServiceClient;
import es.csic.cnb.ws.ChebiException;


public class ChebiStatus {
   public static void main(String[] args) {
    try {
      System.out.println("OK: " + checkChebiStatus());
    } catch (ChebiException e) {
      e.printStackTrace();
      System.out.println(e);
      System.out.println("ERRRRRRRRRRRRRRRRRRRROR");
      System.out.println(e.getMessage());
    }
  }

  public static boolean checkChebiStatus() throws ChebiException {
    boolean ok = true;
    try {
      new ChebiWebServiceClient();
    }
    catch (Exception e) {
      ok = false;
      throw new ChebiException(e);
    }
    return ok;
  }
}
