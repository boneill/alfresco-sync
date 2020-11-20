package au.com.seedim.synch.repo.job;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;

import au.com.seedim.synch.repo.services.SynchService;


public class SynchJobExecutor {
  
  private static final Logger logger = Logger.getLogger(SynchJobExecutor.class);
  private SynchService synchService;
  private ActionService actionService;
  /**
   * Public API access
   */
  private ServiceRegistry serviceRegistry;

  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
      this.serviceRegistry = serviceRegistry;
  }
  
  public void execute() {

    if (logger.isDebugEnabled()){logger.debug("SynchJobExecutor.execute() entered");}
    
    try{
    //get all nodes with SyncSource Aspect and call Synch Action on each Node
    List<NodeRef> synchSourceRootFolderNodes = synchService.getAllSynchSourceRootFolders();
    if (logger.isDebugEnabled()){logger.debug("Number of Synch Source Root Nodes: " + synchSourceRootFolderNodes.size());}
    processNodes(synchSourceRootFolderNodes);
    
    }finally{
      if (logger.isDebugEnabled()){logger.debug("SynchJobExecutor.execute() exited");}
    }
    
  }
  
  /**private void processNodes(List<NodeRef> nodes)
  {
    for (int i = 0; i < nodes.size(); i++)
    {
      // for each node call the action to process it as a pair
      if(nodes.get(i) != null){
        if (logger.isDebugEnabled()){logger.debug("Processing Sync Source Node: " + nodes.get(i));}
        callStartSynchAction(nodes.get(i));
      }
    }
  }**/
  private void processNodes(List<NodeRef> nodes)
  {
    for(NodeRef node : nodes){
      if (logger.isDebugEnabled()){logger.debug("Processing Synch Source Node: " + node);}
      callStartSynchAction(node);
    }
  }
  
  public void callStartSynchAction(NodeRef nodeRef) {
    if (logger.isDebugEnabled()){logger.debug("Excecuting callStartSynchAction callback method");}
    boolean executeAsync = false;
    Map<String, Serializable> aParams = new HashMap<String, Serializable>();
    Action action = actionService.createAction("start-synch", aParams);
    if (action != null) {
       actionService.executeAction(action, nodeRef, true, executeAsync);
    } else {
       throw new RuntimeException("Could not create start-synch action");
    }
}                  

  public SynchService getSynchService() {
    return synchService;
  }

  public void setSynchService(SynchService synchService) {
    this.synchService = synchService;
  }

  public ActionService getActionService() {
    return actionService;
  }

  public void setActionService(ActionService actionService) {
    this.actionService = actionService;
  }

}
