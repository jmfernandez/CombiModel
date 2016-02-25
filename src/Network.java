

import java.util.ArrayList;
import java.util.List;

import es.csic.cnb.data.NetCompound;
import es.csic.cnb.data.NetReaction;

public class Network {
  private String modelName;
  private String modelId;
  
  private List<NetCompound> compList;
  private List<NetReaction> reactionList;
  
  public Network(String modelName, String modelId) {
    this.modelName = modelName;
    this.modelId = modelId;
    
    this.compList = new ArrayList<NetCompound>();
    this.reactionList = new ArrayList<NetReaction>();
  }
  
  public void addCompound(NetCompound comp) {
    compList.add(comp);
  }
  
  public void addReaction(NetReaction reaction) {
    reactionList.add(reaction);
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(modelId).append(":").append(modelName);
    sb.append(" >> ").append(compList);
    return sb.toString();
  }
}
