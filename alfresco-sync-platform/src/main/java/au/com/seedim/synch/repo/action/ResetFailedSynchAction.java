package au.com.seedim.synch.repo.action;

import java.util.List;

import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.log4j.Logger;

import au.com.seedim.synch.model.SeedSynchModel;
import au.com.seedim.synch.repo.services.AlfrescoServices;

public class ResetFailedSynchAction extends ActionExecuterAbstractBase{
  
  private static final Logger logger = Logger.getLogger(ResetFailedSynchAction.class);
  private NodeService nodeService;
  //public static final String PARAM_TARGET_FOLDER = "synchTarget";

  @Override
  protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
    // TODO Auto-generated method stub
   
    //Removed synchFailed Aspect and set JobStatus to blank
    if(nodeService.exists(actionedUponNodeRef)){
      if(nodeService.hasAspect(actionedUponNodeRef, SeedSynchModel.ASPECT_SYNCH_FAILED)){
        nodeService.removeAspect(actionedUponNodeRef, SeedSynchModel.ASPECT_SYNCH_FAILED);
        
        //Get SyncDefNode and update synchJobStatus
        
      }
    }
  }

  @Override
  protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
    // TODO Auto-generated method stub
    //paramList.add(new ParameterDefinitionImpl(PARAM_TARGET_FOLDER, DataTypeDefinition.TEXT, false, getParamDisplayLabel(PARAM_TARGET_FOLDER), false));
  }
  public void setAlfrescoServices(AlfrescoServices alfrescoServices) {
    nodeService = alfrescoServices.getNodeService();

  }

}
