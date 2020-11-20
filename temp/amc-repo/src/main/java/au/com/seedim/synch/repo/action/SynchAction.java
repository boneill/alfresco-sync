package au.com.seedim.synch.repo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.log4j.Logger;

import au.com.seedim.synch.model.SeedSynchModel;
import au.com.seedim.synch.repo.services.AlfrescoServices;
import au.com.seedim.synch.repo.services.SynchService;

public class SynchAction extends ActionExecuterAbstractBase{
  
  private static final Logger logger = Logger.getLogger(SynchAction.class);
  private NodeService nodeService;
  private SynchService synchService;
  private TransactionService transactionService;
  private NodeRef ssdNodeRef = null;
 
  @Override
  protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
    // TODO Auto-generated method stub
    if(logger.isDebugEnabled()){logger.debug("Entered SynchAction executeImpl method");}
    final NodeRef synchSourceNodeRef = actionedUponNodeRef;
        
    AuthenticationUtil.setRunAsUserSystem();
    RetryingTransactionHelper.RetryingTransactionCallback<Object> callback = new RetryingTransactionHelper.RetryingTransactionCallback<Object>() {
       @Override
       public Object execute() throws Throwable {
         processSynchNode(synchSourceNodeRef);
          return null;
       }
    };
    
 // run transaction
    try {
       RetryingTransactionHelper txnHelper = transactionService.getRetryingTransactionHelper();
       //txnHelper.setMaxRetries(1);// retry one time
       txnHelper.doInTransaction(callback, false, true);
    } catch (Throwable e) {
       
       if(logger.isDebugEnabled()){logger.debug("Exception: " + e.getMessage());}
       
       synchService.applySynchFailedMetadata(this.getSsdNodeRef(), synchSourceNodeRef, e.getMessage());
       logger.error(" Failed to process node: "+ actionedUponNodeRef);
       logger.error(" — Error message: " + e.getMessage());
       logger.error(" — Error cause: " + e.getCause());
       e.printStackTrace();
    } 
    if(logger.isDebugEnabled()){logger.debug("Exited SynchAction executeImpl method");}
  }

  @Override
  protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
    // TODO Auto-generated method stub
    paramList.add(new ParameterDefinitionImpl("a-parameter", DataTypeDefinition.TEXT, false, getParamDisplayLabel("a-parameter"))); 
  }
  
  public void setAlfrescoServices(AlfrescoServices alfrescoServices) {
    nodeService = alfrescoServices.getNodeService();
    transactionService = alfrescoServices.getTransactionService();

  }
  
  
  /**
   * Behaves in two ways
   * 
   * If first time Sync:  Create target root node, add a sync source member aspect to each node in source tree and set IS_SYNCED to true, Set sync complete to true
   * 
   * If not first time:  
   * 
   * 
   * @param synchSourceNodeRef
   * @throws FileNotFoundException
   */
  public void processSynchNode(NodeRef synchSourceNodeRef) throws FileNotFoundException{
  
    
      boolean firstSync = false;
      String ssdId = null;
      
      try {
            if(logger.isDebugEnabled()){logger.debug("****************************************");}
            if(logger.isDebugEnabled()){logger.debug("processSynchNode entered ...");}
            if(logger.isDebugEnabled()){logger.debug("****************************************");}
          
            // only process source nodes if they are sync source  and are not marked as failed and have a target association set to the root target folder
            if(nodeService.exists(synchSourceNodeRef) && 
                  nodeService.hasAspect(synchSourceNodeRef, SeedSynchModel.ASPECT_SYNCH_SOURCE) && 
                  !nodeService.hasAspect(synchSourceNodeRef, SeedSynchModel.ASPECT_SYNCH_FAILED) &&
                  !nodeService.getTargetAssocs(synchSourceNodeRef,  SeedSynchModel.ASSOC_TARGET_ROOT_FOLDER).isEmpty()){
            
                
                Map<QName, Serializable> ssdProps = new HashMap<QName, Serializable>();
                
                //Check if Node has an Associated Sync Def Node and if not create node
                List<AssociationRef> synchSourceAssocs = nodeService.getTargetAssocs(synchSourceNodeRef, SeedSynchModel.ASSOC_SYNCH_SET_DEF_NODE);
                if(synchSourceAssocs.isEmpty()){
                  
                  
                    if(logger.isDebugEnabled()){logger.debug("SynchSource does not have associated SynchSetDefinitionNode");}
                    firstSync = true;
                    
                    this.ssdNodeRef = synchService.createSynchSetDefinitionNode(synchSourceNodeRef);
                         
                }
                else
                {
                  // get the existing ssdNode
                  this.ssdNodeRef = synchSourceAssocs.get(0).getTargetRef();
                }
                  
                
                ssdProps = nodeService.getProperties(this.ssdNodeRef);
                ssdId = (String) ssdProps.get(SeedSynchModel.PROP_SYNCH_SET_ID);
                if(logger.isDebugEnabled()){logger.debug("SyncSetJobDefinition ID for Synch is " + ssdId);}
                if(logger.isDebugEnabled()){logger.debug("ssd nodeRef: " + this.ssdNodeRef);}
                
                // set job status
                String SynchJobStatus = (String) ssdProps.get(SeedSynchModel.PROP_SYNCH_SET_JOB_STATUS);
                if(logger.isDebugEnabled()){logger.debug("Sync Job Status; " + SynchJobStatus);}
            
                
                if(SynchJobStatus.equalsIgnoreCase("In Progress")){
                  // do not proceed any further as another job is working with it
                  return;
                }else{
                  this.nodeService.setProperty(this.ssdNodeRef, SeedSynchModel.PROP_SYNCH_SET_JOB_STATUS, "In Progress");
                }
                
                
                //Process a full iniitial sync if its the first time that we are synching
                if(firstSync){
                    
                    if(logger.isDebugEnabled()){logger.debug("New Sync");}
                    
                    runInitialSync(synchSourceNodeRef, ssdProps);
                }
                else
                {
                    // There have been changes withing the source folder tree.  Therefore, determine these changes and sync changed nodes.  Nodes can change
                    // by either being New, updated to a property or change of content.  Each change will be managed seperately.
                    
                    // proces partial sync
                                    
                    ResultSet dirtyNodesRS = synchService.getDirtyNodes(ssdId);
                    
                    if(logger.isDebugEnabled()){logger.debug("SynchAction.processSynchNode dirtyNodes size" + dirtyNodesRS.length());}
                    
                    if(dirtyNodesRS != null &&  dirtyNodesRS.length() > 0){
                      
                      if(logger.isDebugEnabled()){logger.debug("Sync Update");}
                      // run Sync Update
                      runSyncUpdate(dirtyNodesRS);
                    }
                 
                }
                
                if(this.ssdNodeRef != null){
                  this.nodeService.setProperty(this.ssdNodeRef, SeedSynchModel.PROP_SYNCH_SET_JOB_STATUS, "Complete");
                }
              
            }else{
              if(logger.isDebugEnabled()){logger.debug("Synch node does not exist or has failed synch aspect ");}
            }
          
          }catch (FileExistsException fe) {
            if(logger.isDebugEnabled()){logger.debug("FileExistsException: " + fe.getMessage());}
            throw fe;
          } catch (FileNotFoundException fn) {
            if(logger.isDebugEnabled()){logger.debug("FileNotFoundException: " + fn.getMessage());}
            throw fn;
          } catch (AlfrescoRuntimeException are){
            if(logger.isDebugEnabled()){logger.debug("AlfrescoRuntimeException: " + are.getMessage());}
            throw are;
          }catch (Exception e){
            if(logger.isDebugEnabled()){logger.debug("Exception: " + e.getMessage());}
            throw e;
          }finally{
            
            if(logger.isDebugEnabled()){logger.debug("****************************************");}
            if(logger.isDebugEnabled()){logger.debug("processSynchNode exited ...");}
            if(logger.isDebugEnabled()){logger.debug("****************************************");}
            
            
          }

          
          
    }

  
      private void runSyncUpdate(ResultSet dirtyNodes) throws FileNotFoundException {
        
        if(logger.isDebugEnabled()){logger.debug("Entered runSyncUpdate ...");}
        
        try 
        {
            // Process new Nodes first
            if(dirtyNodes != null){
              if(logger.isDebugEnabled()){logger.debug("Number of nodes requiring sync " + dirtyNodes.length());}
              //synchService.mergeUnSynchedNewMemberNodes(newNodes, synchSourceNodeRef, targetRootFolderNodeString);
              synchService.syncNodes(dirtyNodes);
            }
            
            
          //update Job Status and isSynch
          nodeService.setProperty(ssdNodeRef, SeedSynchModel.PROP_SYNCH_SET_JOB_STATUS, "Complete");
        } catch (FileExistsException fe) {
          if(logger.isDebugEnabled()){logger.debug("FileExistsException: " + fe.getMessage());}
          throw fe;
        } catch (FileNotFoundException fn) {
          if(logger.isDebugEnabled()){logger.debug("FileNotFoundException: " + fn.getMessage());}
          throw fn;
        } catch (AlfrescoRuntimeException are){
          if(logger.isDebugEnabled()){logger.debug("AlfrescoRuntimeException: " + are.getMessage());}
          throw are;
        }catch (Exception e){
          if(logger.isDebugEnabled()){logger.debug("Exception: " + e.getMessage());}
          throw e;
        }
        
        
        if(logger.isDebugEnabled()){logger.debug("Exited runSyncUpdate ...");}
        
    // TODO Auto-generated method stub
    
  }

      /**
       * Run the initial sync, setting up the target root foler and synching all content into it.
       * @param sourceNodeRef
       * @param ssdProps
       * @throws FileNotFoundException 
       * @throws FileExistsException 
       */
      private void runInitialSync(NodeRef sourceNodeRef,  Map<QName, Serializable> ssdProps) throws FileExistsException, FileNotFoundException {
        
          try{
          
            this.synchService.disableBehaviour(sourceNodeRef, ContentModel.ASPECT_AUDITABLE);
            this.synchService.disableSynchBehaviours();
          
        
            String ssdId = (String) ssdProps.get(SeedSynchModel.PROP_SYNCH_SET_ID);
            
            if(logger.isDebugEnabled()){logger.debug("runInitialSync: ssdId value for SourceRootNode: " + ssdId);}
            
          //Create a root target folder and mark this sync as Inprogress
            nodeService.setProperty(ssdNodeRef, SeedSynchModel.PROP_SYNCH_SET_JOB_STATUS, "In Progress");
            NodeRef targetRootNodeRef = synchService.createTargetRootFolder(sourceNodeRef, this.ssdNodeRef);
            
            synchService.addMemberDetailsProperties(sourceNodeRef, targetRootNodeRef, ssdId);  // set the issynced to true because we are creating the node
            
            //this.nodeService.setProperty(sourceNodeRef, SeedSynchModel.PROP_SYNCH_SET_ID, ssdId);
            
            this.nodeService.setProperty(sourceNodeRef, SeedSynchModel.PROP_SYNCH_DEF_NODE_ID, ssdId);
          
            
            
  //               
            // process each member node
            Boolean includeSubFolders = (Boolean) ssdProps.get(SeedSynchModel.PROP_SYNCH_SET_INCLUDE_SUBFOLDERS);
            synchService.copyTreeToTarget(sourceNodeRef, targetRootNodeRef, includeSubFolders, ssdProps);
            
            nodeService.setProperty(ssdNodeRef, SeedSynchModel.PROP_SYNCH_SET_JOB_STATUS, "Complete");
            
            
          }finally{
            
            this.synchService.enableSynchBehaviours();
            this.synchService.enableBehaviour(sourceNodeRef, ContentModel.ASPECT_AUDITABLE);
          }
        }
      
  
  
  public SynchService getSynchService() {
    return synchService;
  }

  public void setSynchService(SynchService synchService) {
    this.synchService = synchService;
  }

  public NodeRef getSsdNodeRef() {
    return ssdNodeRef;
  }

  public void setSsdNodeRef(NodeRef ssdNodeRef) {
    this.ssdNodeRef = ssdNodeRef;
  }


}
