package au.com.seedim.synch.repo.webscripts;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import au.com.seedim.synch.model.SeedSynchModel;
import au.com.seedim.synch.repo.services.AlfrescoServices;
import au.com.seedim.synch.repo.services.SynchService;

public class UnSynch extends DeclarativeWebScript{
  
  static final Logger logger = Logger.getLogger(ConfigureSynch.class);
  private NodeService nodeService;
  private SynchService synchService;
  
  
  @Override
  protected Map<String, Object> executeImpl(WebScriptRequest request, Status status, Cache cache) {
  
    if(logger.isDebugEnabled()){logger.debug("Entered UnSynch executeImpl method");}
    Map<String, Object> model = new HashMap<String, Object>();
    
    final String actionedUponNodeRefString= request.getParameter("nodeRef");
    

    try{
    
        final NodeRef actionedUponNodeRef = new NodeRef(actionedUponNodeRefString);
        
        if(logger.isDebugEnabled()){logger.debug("Unsynch NodeRef: " + actionedUponNodeRef);}

        if(nodeService.exists(actionedUponNodeRef)){
          if(nodeService.hasAspect(actionedUponNodeRef, SeedSynchModel.ASPECT_SYNCH_SOURCE)){
            synchService.removeSynch(actionedUponNodeRef);
            model.put("status", "success");
          }
        }
    }catch(Exception e){
      throw e;
    }
    if(logger.isDebugEnabled()){logger.debug("Exited UnSynch executeImpl method");}
    return model;
  }

  public void setAlfrescoServices(AlfrescoServices alfrescoServices) {
    nodeService = alfrescoServices.getNodeService();

  }

  public SynchService getSynchService() {
    return synchService;
  }

  public void setSynchService(SynchService synchService) {
    this.synchService = synchService;
  }

}
