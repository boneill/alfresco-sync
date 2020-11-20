package au.com.seedim.synch.repo.webscripts;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import au.com.seedim.synch.model.SeedSynchModel;
import au.com.seedim.synch.repo.services.AlfrescoServices;

public class ConfigureSynch extends DeclarativeWebScript{
  static final Logger logger = Logger.getLogger(ConfigureSynch.class);
  private NodeService nodeService;
  
  @Override
  protected Map<String, Object> executeImpl(WebScriptRequest request, Status status, Cache cache) {
  
    if(logger.isDebugEnabled()){logger.debug("Entered ConfigureSynch executeImpl method");}
    Map<String, Object> model = new HashMap<String, Object>();
    
    final String actionedUponNodeRefString= request.getParameter("nodeRef");
    
    //this.setPublicAccessRemoved(false);
    try{
    
        final NodeRef actionedUponNodeRef = new NodeRef(actionedUponNodeRefString);
        
        if(logger.isDebugEnabled()){logger.debug("NodeRef: " + actionedUponNodeRef);}
        //set syncCreator Owner
        final String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
        // Create Map for properties
        Map<QName, Serializable> myProps = new HashMap<QName, Serializable>();
        myProps.put(SeedSynchModel.PROP_CREATOR_USERNAME, currentUser);
        if(nodeService.exists(actionedUponNodeRef)){
          if(!nodeService.hasAspect(actionedUponNodeRef, SeedSynchModel.ASPECT_SYNCH_SOURCE)){
            nodeService.addAspect(actionedUponNodeRef, SeedSynchModel.ASPECT_SYNCH_SOURCE, myProps);
            model.put("status", "success");
          }
        }
    }catch(Exception e){
      throw e;
    }
    if(logger.isDebugEnabled()){logger.debug("Exited ConfigureSynch executeImpl method");}
    return model;
  }

  public void setAlfrescoServices(AlfrescoServices alfrescoServices) {
    nodeService = alfrescoServices.getNodeService();

  }
  

}
