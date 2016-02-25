import uk.ac.ebi.chebi.webapps.chebiWS.client.ChebiWebServiceClient;
import uk.ac.ebi.chebi.webapps.chebiWS.model.ChebiWebServiceFault_Exception;
import uk.ac.ebi.chebi.webapps.chebiWS.model.Entity;
import uk.ac.ebi.chebi.webapps.chebiWS.model.LiteEntity;
import uk.ac.ebi.chebi.webapps.chebiWS.model.LiteEntityList;
import uk.ac.ebi.chebi.webapps.chebiWS.model.OntologyDataItem;
import uk.ac.ebi.chebi.webapps.chebiWS.model.OntologyDataItemList;
import uk.ac.ebi.chebi.webapps.chebiWS.model.RelationshipType;
import uk.ac.ebi.chebi.webapps.chebiWS.model.SearchCategory;
import uk.ac.ebi.chebi.webapps.chebiWS.model.StarsCategory;


public class PrbChebi {

  /**
   * @param args
   */
  public static void main(String[] args) {
    String name = "CHEBI:57761";

    try {
      ChebiWebServiceClient client = new ChebiWebServiceClient();
      LiteEntityList list = client.getLiteEntity(name, SearchCategory.CHEBI_ID, 10, StarsCategory.THREE_ONLY);
      for (LiteEntity lite : list.getListElement()) {
        String chebiId = lite.getChebiId();
        // Recuperar y guardar si no existe
        Entity entity = client.getCompleteEntity(chebiId);
        System.out.println(name + " >>> ID: "+entity.getChebiId() +" --> " + entity.getChebiAsciiName()+
                " ["+lite.getSearchScore()+"] "+entity.getStatus() + " - " + entity.getEntityStar());

        OntologyDataItemList ont = client.getOntologyParents(chebiId);
        for (OntologyDataItem item : ont.getListElement()) {
          System.out.println();
          System.out.println(item.getType());
          System.out.println(item.getChebiId());
          System.out.println(item.getChebiName());
          System.out.println(item.getStatus());
        }

        list = client.getAllOntologyChildrenInPath(chebiId, RelationshipType.IS_CONJUGATE_ACID_OF, true);
        for (LiteEntity lite1 : list.getListElement()) {
          chebiId = lite1.getChebiId();
          // Recuperar y guardar si no existe
          entity = client.getCompleteEntity(chebiId);

          System.out.println("> RelationshipType.IS_CONJUGATE_ACID_OF");
          System.out.println("> ID: "+entity.getChebiId() +" --> " + entity.getChebiAsciiName()+
                  " ["+lite.getSearchScore()+"] "+entity.getStatus() + " - " + entity.getEntityStar());
        }

        list = client.getAllOntologyChildrenInPath(chebiId, RelationshipType.IS_CONJUGATE_BASE_OF, true);
        for (LiteEntity lite1 : list.getListElement()) {
          chebiId = lite1.getChebiId();
          // Recuperar y guardar si no existe
          entity = client.getCompleteEntity(chebiId);

          System.out.println("> RelationshipType.IS_CONJUGATE_BASE_OF");
          System.out.println("> ID: "+entity.getChebiId() +" --> " + entity.getChebiAsciiName()+
                  " ["+lite.getSearchScore()+"] "+entity.getStatus() + " - " + entity.getEntityStar());
        }
      }
    }
    catch(ChebiWebServiceFault_Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }

  }

}
