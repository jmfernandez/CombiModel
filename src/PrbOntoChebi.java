import uk.ac.ebi.chebi.webapps.chebiWS.client.ChebiWebServiceClient;
import uk.ac.ebi.chebi.webapps.chebiWS.model.ChebiWebServiceFault_Exception;
import uk.ac.ebi.chebi.webapps.chebiWS.model.LiteEntity;
import uk.ac.ebi.chebi.webapps.chebiWS.model.LiteEntityList;
import uk.ac.ebi.chebi.webapps.chebiWS.model.RelationshipType;


public class PrbOntoChebi {

  public static boolean isConjugated(String compChebiId, String seedChebiId) {
    boolean ok = false;

    ChebiWebServiceClient client = new ChebiWebServiceClient();

    try {
      final LiteEntityList listAcid = client.getAllOntologyChildrenInPath(
              compChebiId, RelationshipType.IS_CONJUGATE_ACID_OF, true);
      for (LiteEntity liteAcid : listAcid.getListElement()) {
        String id = liteAcid.getChebiId();
        if (id.equalsIgnoreCase(seedChebiId)) {
          ok = true;
        }
      }

      if (!ok) {
        final LiteEntityList listBase = client.getAllOntologyChildrenInPath(
                compChebiId, RelationshipType.IS_CONJUGATE_BASE_OF, true);
        for (LiteEntity liteBase : listBase.getListElement()) {
          String id = liteBase.getChebiId();
          if (id.equalsIgnoreCase(seedChebiId)) {
            ok = true;
          }
        }
      }
    }
    catch(ChebiWebServiceFault_Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }

    return ok;
  }

  public static void main(String[] args) {
    String chebiId = "CHEBI:57761";

    System.out.println();
    System.out.println("---> "+chebiId + " - " + "CHEBI:16410");
    System.out.println(isConjugated(chebiId, "CHEBI:16410"));
    System.out.println("---> "+chebiId + " - " + "CHEBI:16411");
    System.out.println(isConjugated(chebiId, "CHEBI:164101"));
    System.out.println();

    try {
      ChebiWebServiceClient client = new ChebiWebServiceClient();

      LiteEntityList list = client.getAllOntologyChildrenInPath(chebiId, RelationshipType.IS_CONJUGATE_ACID_OF, true);
      for (LiteEntity lite : list.getListElement()) {
        String id = lite.getChebiId();

        System.out.println("> RelationshipType.IS_CONJUGATE_ACID_OF");
        System.out.println("> ID: "+ id +" --> " + lite.getChebiAsciiName() +
                " ["+lite.getSearchScore()+"] "+lite.getEntityStar());


//        // Recuperar y guardar si no existe
//        entity = client.getCompleteEntity(id);
//
//        System.out.println("> RelationshipType.IS_CONJUGATE_ACID_OF");
//        System.out.println("> ID: "+entity.getChebiId() +" --> " + entity.getChebiAsciiName()+
//                " ["+lite.getSearchScore()+"] "+entity.getStatus() + " - " + entity.getEntityStar());

      }

      list = client.getAllOntologyChildrenInPath(chebiId, RelationshipType.IS_CONJUGATE_BASE_OF, true);
      for (LiteEntity lite : list.getListElement()) {
        String id = lite.getChebiId();

        System.out.println("> RelationshipType.IS_CONJUGATE_BASE_OF");
        System.out.println("> ID: "+ id +" --> " + lite.getChebiAsciiName() +
                " ["+lite.getSearchScore()+"] "+lite.getEntityStar());

//        // Recuperar y guardar si no existe
//        entity = client.getCompleteEntity(chebiId);
//
//        System.out.println("> RelationshipType.IS_CONJUGATE_BASE_OF");
//        System.out.println("> ID: "+entity.getChebiId() +" --> " + entity.getChebiAsciiName()+
//                " ["+lite.getSearchScore()+"] "+entity.getStatus() + " - " + entity.getEntityStar());

      }

    }
    catch(ChebiWebServiceFault_Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }

  }
}
