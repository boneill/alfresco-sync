package au.com.seedim.synch.repo.behaviour;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.ContentServicePolicies.OnContentPropertyUpdatePolicy;
import org.alfresco.repo.node.NodeServicePolicies.BeforeMoveNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateChildAssociationPolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnUpdatePropertiesPolicy;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.version.VersionServicePolicies.AfterCreateVersionPolicy;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.PropertyMap;
import org.apache.log4j.Logger;

import au.com.seedim.synch.model.SeedSynchModel;
import au.com.seedim.synch.repo.services.AlfrescoServices;
import au.com.seedim.synch.repo.services.SynchService;

public class SynchChangeMonitor implements OnCreateChildAssociationPolicy,
                                           OnUpdatePropertiesPolicy,
                                           OnContentPropertyUpdatePolicy,
                                           BeforeMoveNodePolicy
                                           //AfterCreateVersionPolicy
                                           //OnCopyNodePolicy
                                           //BeforeDeleteChildAssociationPolicy,
                                           //BeforeDeleteNodePolicy
{
  
  private static final Logger logger = Logger.getLogger(SynchChangeMonitor.class);
  private PolicyComponent policyComponent;
  private NodeService nodeService;
  private FileFolderService fileFolderService;
  private SynchService synchService;
  public List<String> propertiesToTrack;
  private TransactionService transactionService;
  private Boolean deleteNodeStatus;
  
 
  public void init(){
    if(logger.isDebugEnabled()) logger.debug("Initialising onCreateChildAssociation behaviour");
    
    //child create
    this.policyComponent.bindAssociationBehaviour(
            OnCreateChildAssociationPolicy.QNAME,
            SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE,
            ContentModel.ASSOC_CONTAINS,
            new JavaBehaviour(this, "onCreateChildAssociation", NotificationFrequency.TRANSACTION_COMMIT));
    
    
    if(logger.isDebugEnabled()) logger.debug("Initialising onUpdateProperties behaviour");
    this.policyComponent.bindClassBehaviour(
            OnUpdatePropertiesPolicy.QNAME,
            SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE, 
            new JavaBehaviour(this, "onUpdateProperties", NotificationFrequency.EVERY_EVENT));
    
    if(logger.isDebugEnabled()) logger.debug("Initialising onContentPropertyUpdate behaviour");
    this.policyComponent.bindClassBehaviour(
            OnContentPropertyUpdatePolicy.QNAME,
            SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE, 
            new JavaBehaviour(this, "onContentPropertyUpdate", NotificationFrequency.EVERY_EVENT));

//    if(logger.isDebugEnabled()) logger.debug("Initialising BeforeMoveNode behaviour");
//    this.policyComponent.bindClassBehaviour(
//            BeforeMoveNodePolicy.QNAME,
//            SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE, 
//            new JavaBehaviour(this, "beforeMoveNode", NotificationFrequency.EVERY_EVENT));
    
    if(logger.isDebugEnabled()) logger.debug("Initialising BeforeMoveNode behaviour");
    this.policyComponent.bindClassBehaviour(
        BeforeMoveNodePolicy.QNAME,
        SeedSynchModel.ASPECT_SYNCH_TARGET, 
        new JavaBehaviour(this, "beforeMoveNode", NotificationFrequency.EVERY_EVENT));


    if(logger.isDebugEnabled()) logger.debug("Initialising BeforeMoveNode behaviour");
    this.policyComponent.bindClassBehaviour(
        BeforeMoveNodePolicy.QNAME,
        SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE, 
        new JavaBehaviour(this, "beforeMoveNode", NotificationFrequency.FIRST_EVENT));

  }

  
  @Override
  public void onCreateChildAssociation(ChildAssociationRef childAssoc, boolean isNewNode) {
    // TODO Auto-generated method stub
    
    if(logger.isDebugEnabled()) logger.debug("SynchChangeMonitor.onCreateChildAssociation method entered for node " + childAssoc.getChildRef() + " isNewNode: " + isNewNode);
    
    if (childAssoc.isPrimary() && isNewNode)
    {
        NodeRef childNodeRef = childAssoc.getChildRef();
//        NodeRef parentNodeRef = childAssocRef.getParentRef();
//        List<NodeRef> synchMemberNodes = new ArrayList<NodeRef>();
//        Boolean firstSynchJob = false;
//        String ssdId = null;
//        
        if (nodeService.exists(childNodeRef) && !nodeService.hasAspect(childNodeRef, ContentModel.ASSOC_WORKING_COPY_LINK))
        {
          
          synchService.addMemberDetailsProperties(childNodeRef, null, null);  // set the issynced to true because we are creating the
          
          if(fileFolderService.getFileInfo(childNodeRef).isFolder()){
            
            synchService.addMemberPropertiesRecursively(childNodeRef);
          }
          
          
          
          
          
          
          
//          synchMemberNodes.add(childNodeRef); 
//           
//           if(logger.isDebugEnabled()) logger.debug("Getting SSID from Parent:" + parentNodeRef);
//           
//           ssdId = synchService.getSynSetDefID(childNodeRef);
//      
//           if(ssdId != null ){
//               if(logger.isDebugEnabled()) logger.debug("SSID found from Parent's Parent " + ssdId);
//      
//               //set SyncMemberNode
//               synchService.addSynchSetMemberNodes(null, ssdId, synchMemberNodes, firstSynchJob);
//               
//  
//               
//               
//               
//             }
////           }else{
////             
//             // This must be the first time the sync has been run for this so 
//             
//             Boolean parentSynchStatus = (Boolean) nodeService.getProperty(parentNodeRef, SeedSynchModel.PROP_IS_SYNCHED);
//             
//             if(!parentSynchStatus){
//               firstSynchJob = true;
//             }
//             //set SyncMemberNode
//             synchService.addSynchSetMemberNodes(null, ssdId, synchMemberNodes, firstSynchJob);
//           }
           
        }
    }
    if(logger.isDebugEnabled()) logger.debug("SynchChangeMonitor.onCreateChildAssociation method exited");
  }

  
  
  /**@Override
  public void beforeDeleteChildAssociation(ChildAssociationRef childAssocRef) {
    // TODO Auto-generated method stub
    if(logger.isDebugEnabled()) logger.debug("SynchChangeMonitor.beforeDeleteChildAssociation method entered");
    
    if (childAssocRef.isPrimary())
    {
        NodeRef childNodeRef = childAssocRef.getChildRef();
        NodeRef parentNodeRef = childAssocRef.getParentRef();
        if (nodeService.exists(childNodeRef))
        {
          
          if(logger.isDebugEnabled()) logger.debug("Primary Assoc:" + childAssocRef.isPrimary() + "-" + childNodeRef);
           //Get SSID from parent
          Map<QName, Serializable> memberParentNodeProps = nodeService.getProperties(parentNodeRef);
           if(logger.isDebugEnabled()) logger.debug("Getting SSID from Parent:" + parentNodeRef);
           //String ssdId = (String) nodeService.getProperty(parentNodeRef, SeedSynchModel.PROP_SYNCH_DEF_NODE_ID);
           String ssdId = (String) memberParentNodeProps.get(SeedSynchModel.PROP_SYNCH_DEF_NODE_ID);
           //----
           if(ssdId.isEmpty()){
             if(logger.isDebugEnabled()) logger.debug("SSID NOT found Trying Parent's Parent ");
             
             List<ChildAssociationRef> parentAssocs = nodeService.getParentAssocs(parentNodeRef);
           //try parents parent
             if(!parentAssocs.isEmpty()){
               NodeRef parentParent = parentAssocs.get(0).getParentRef();
               ssdId = (String) nodeService.getProperty(parentParent, SeedSynchModel.PROP_SYNCH_DEF_NODE_ID);
               if(ssdId.isEmpty()){
                 //set status fail
               }
               if(logger.isDebugEnabled()) logger.debug("SSID found from Parent's Parent " + ssdId);
             
                //get Target
               
             }
           }else{
             if(logger.isDebugEnabled()) logger.debug("SSID found " + ssdId); 
             //get TargetRootFolder based on ssdID and then recursive try to find node to deleted
             try {
               
               NodeRef targetSynchRootFolder = synchService.getTargetSynchRootFolderNode(ssdId);
               //Get Parent Name and SyncMemberNode Name
               String parentName = (String) memberParentNodeProps.get(ContentModel.PROP_NAME);
               String synchMemberNodeName = (String) nodeService.getProperty(childNodeRef, ContentModel.PROP_NAME);
               NodeRef synchTargetNode = null;
               synchTargetNode = synchService.findSynchTargetNode(targetSynchRootFolder, parentName, synchMemberNodeName);
               //finally deleted node
               if(nodeService.exists(synchTargetNode)){
                 
                 nodeService.deleteNode(synchTargetNode);
               }
               
            } catch (FileNotFoundException e) {
              // TODO Auto-generated catch block
              //e.printStackTrace();
              throw new AlfrescoRuntimeException(
                  "SynchChangeMonitor.beforeDeleteChildAssociation: " + e);
            }catch (Exception e){
              throw new AlfrescoRuntimeException(
                  "SynchChangeMonitor.beforeDeleteChildAssociation exception: " + e);
            }
           }
           
        }
    }
    if(logger.isDebugEnabled()) logger.debug("SynchChangeMonitor.beforeDeleteChildAssociation method exited");
  }**/
  
  
  @Override
  public void onUpdateProperties(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after) {
    // TODO Auto-generated method stub
    if(logger.isDebugEnabled()) logger.debug("SynchChangeMonitor.onUpdateProperties method entered");
    
    if(nodeRef != null && nodeService.exists(nodeRef)){
      //get properties to monitor
      List<QName> trackablePropertyQNames = synchService.toPropertyNames(propertiesToTrack);
      if(logger.isDebugEnabled()) logger.debug("SynchChangeMonitor.onUpdateProperties node: " + nodeRef);
      
      Map<QName, Serializable> filteredBeforeProps = synchService.filterIrrelevantProperties(trackablePropertyQNames,before);
      Map<QName, Serializable> filteredAfterProps =  synchService.filterIrrelevantProperties(trackablePropertyQNames, after);
      HashSet<QName> changedProperties = new HashSet<QName>();
      // After filtering, perhaps none of the relevant properties have changed...
      if ( !filteredBeforeProps.equals(filteredAfterProps))
      {
          if(logger.isDebugEnabled()){logger.debug("a tracked property has changed filteredAfterProps: " + filteredAfterProps);}
          changedProperties = this.recordNonContentPropertiesUpdate(nodeRef, filteredBeforeProps, filteredAfterProps);
          if(!changedProperties.isEmpty()){
            //Check if Name has changed and if yes then set in synch:previousName
            synchService.disableSynchBehaviours();
//            if(changedProperties.contains(ContentModel.PROP_NAME)){
//              //String beforeName = (String) before.get(ContentModel.PROP_NAME);
//              String beforeName = (String) filteredBeforeProps.get(ContentModel.PROP_NAME);
//              if(logger.isDebugEnabled()) logger.debug("SynchChangeMonitor.onUpdateProperties previous node name : " + beforeName);
//              
//              nodeService.setProperty(nodeRef, SeedSynchModel.PROP_PREVIOUS_NAME, beforeName);
//            }
            
            nodeService.setProperty(nodeRef, SeedSynchModel.PROP_HAS_PROPERTIES_CHANGED, true);
            nodeService.setProperty(nodeRef, SeedSynchModel.PROP_IS_SYNCHED, false);
            
            synchService.enableSynchBehaviours();
          }
          
          
      }
    }
    if(logger.isDebugEnabled()) logger.debug("SynchChangeMonitor.onUpdateProperties method exited");
  }
  
  @Override
  public void onContentPropertyUpdate(NodeRef nodeRef, QName propertyQName, ContentData beforeValue, ContentData afterValue) {
    // TODO Auto-generated method stub
    if(logger.isDebugEnabled()) logger.debug("SynchChangeMonitor.onContentPropertyUpdate method entered");
    
    if(nodeRef != null && nodeService.exists(nodeRef)){
      if(!fileFolderService.getFileInfo(nodeRef).isFolder()){
        synchService.disableSynchBehaviours();
        nodeService.setProperty(nodeRef, SeedSynchModel.PROP_HAS_CONTENT_CHANGED, true);
        nodeService.setProperty(nodeRef, SeedSynchModel.PROP_IS_SYNCHED, false);
        synchService.enableSynchBehaviours();
      }
    }
    if(logger.isDebugEnabled()) logger.debug("SynchChangeMonitor.onContentPropertyUpdate method exited");
  }
  
  @Override
  public void beforeMoveNode(ChildAssociationRef oldChildAssocRef, NodeRef newParentRef) {

    if(logger.isDebugEnabled()) logger.debug("SynchChangeMonitor.beforeMoveNode entered");
    
    synchService.disableSynchBehaviours();
    try{
    
    
        
        final NodeRef sourceNodeRef = oldChildAssocRef.getChildRef();
        //final NodeRef synchMemberParenNodeRef = oldChildAssocRef.getParentRef();
        if(sourceNodeRef != null && nodeService.exists(sourceNodeRef)){
          //prevent Synch Source or Target Root Folder move
          if(nodeService.hasAspect(sourceNodeRef, SeedSynchModel.ASPECT_SYNCH_SOURCE) || nodeService.hasAspect(sourceNodeRef, SeedSynchModel.ASPECT_SYNCH_TARGET)){
            throw new AlfrescoRuntimeException("move is not allowed for Synch Source or Target foler. Attempted to move " + sourceNodeRef);
          }
        
          if(logger.isDebugEnabled()) logger.debug("SynchChangeMonitor.beforeMoveNodefor node " + 
              nodeService.getProperty(sourceNodeRef, ContentModel.PROP_NAME) + " [" + sourceNodeRef + "]"  );
          
          // get associated target ref
          String targetRefString = (String)nodeService.getProperty(sourceNodeRef, SeedSynchModel.PROP_SYNCED_TARGET_REF);
          NodeRef targetNodeRef = targetRefString != null? new NodeRef(targetRefString) : null;
          
          if(!nodeService.hasAspect(newParentRef, SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE)){
            
            // deal with case where moving file outside of target
            
              if(logger.isDebugEnabled()) logger.debug("SynchChangeMonitor.beforeMoveNode Node being moved out of synch folder ");
              
              if(targetNodeRef != null && nodeService.exists(targetNodeRef)){
                if(logger.isDebugEnabled()) logger.debug("beforeMoveNode delete target node " + targetNodeRef);
                nodeService.deleteNode(targetNodeRef);
              }
              
              synchService.sanitiseSynchedTargetNodesRecursively(sourceNodeRef);
            
          }else{
            
            if(logger.isDebugEnabled()) logger.debug("beforeMoveNode move within a synch folder");
            
            // deal with case where moving within the target
            
            // get the location where the target node should be moved ot.  This will be pointed at from the assoc target ref in the newParent
            String targetParentRefString = (String)nodeService.getProperty(newParentRef, SeedSynchModel.PROP_SYNCED_TARGET_REF);
            NodeRef targetParentNodeRef = targetParentRefString != null? new NodeRef(targetParentRefString) : null;
            
            
            if(logger.isDebugEnabled()) logger.debug("beforeMoveNode move target " + targetNodeRef + " to " + targetParentRefString );
            
            nodeService.moveNode(targetNodeRef, targetParentNodeRef, oldChildAssocRef.getTypeQName(), oldChildAssocRef.getQName()); 
            
          }
        }
    }
    catch(Exception e){
        
        logger.error(" Failed to move Node: "+ oldChildAssocRef.getChildRef());
        logger.error(" — Error message: " + e.getMessage());
        logger.error(" — Error cause: " + e.getCause());
        e.printStackTrace();
    }
    
    finally{
      synchService.enableSynchBehaviours();
      if(logger.isDebugEnabled()) logger.debug("SynchChangeMonitor.beforeMoveNode exited");
    }
    
   
  }

//  @Override
//  public void beforeMoveNode(ChildAssociationRef oldChildAssocRef, NodeRef newParentRef) {
//    if(logger.isDebugEnabled()) logger.debug("SynchChangeMonitor.beforeMoveNode method entered");
//    final NodeRef synchMemberNodeRef = oldChildAssocRef.getChildRef();
//    final NodeRef synchMemberParenNodeRef = oldChildAssocRef.getParentRef();
//    if(!newParentRef.toString().matches(synchMemberParenNodeRef.toString())){
//      if(synchMemberNodeRef != null && nodeService.exists(synchMemberNodeRef)){
//        //prevent Synch Source or Target Root Folder move
//        if(nodeService.hasAspect(synchMemberNodeRef, SeedSynchModel.ASPECT_SYNCH_SOURCE) || nodeService.hasAspect(synchMemberNodeRef, SeedSynchModel.ASPECT_SYNCH_TARGET)){
//          throw new AlfrescoRuntimeException("move is not allowed for Synch Source or Target foler. Attempted to move " + synchMemberNodeRef);
//        }else{
//          synchService.disableSynchBehaviours();
//          
//          /**if(fileFolderService.getFileInfo(synchMemberNodeRef).isFolder()){
//            synchService.deleteTargetNode(synchMemberNodeRef);
//            //Using sanitiseSynchedTargetNodesRecursively for SynchMemberNodes
//            synchService.sanitiseSynchedTargetNodesRecursively(synchMemberNodeRef);
//            }else{
//              synchService.deleteTargetNode(synchMemberNodeRef);
//              //Using sanitiseSynchedTargetNodesRecursively for SynchMemberNodes
//              synchService.sanitiseSynchedTargetNode(synchMemberNodeRef);
//            }**/
//          
//          AuthenticationUtil.setRunAsUserSystem();
//          setDeleteNodeStatus(false);
//          RetryingTransactionCallback<Void> callback = new RetryingTransactionCallback<Void>() {
//            @Override
//            public Void execute() throws Throwable {
//              synchService.deleteTargetNode(synchMemberNodeRef);
//              setDeleteNodeStatus(true);
//              return null;
//            }
//          };
//          try {
//            RetryingTransactionHelper txnHelper = transactionService.getRetryingTransactionHelper();
//            txnHelper.setMaxRetries(1);// retry one time
//            txnHelper.doInTransaction(callback, false, true);
//          }catch (Throwable e) {
//            logger.error(" Failed to delete Node: "
//                  + synchMemberNodeRef);
//            logger.error(" — Error message: " + e.getMessage());
//            logger.error(" — Error cause: " + e.getCause());
//            e.printStackTrace();
//          }
//          if(getDeleteNodeStatus()){
//            if(fileFolderService.getFileInfo(synchMemberNodeRef).isFolder()){
//              //synchService.deleteTargetNode(synchMemberNodeRef);
//              //Using sanitiseSynchedTargetNodesRecursively for SynchMemberNodes
//              synchService.sanitiseSynchedTargetNodesRecursively(synchMemberNodeRef);
//            }else{
//              //synchService.deleteTargetNode(synchMemberNodeRef);
//              //Using sanitiseSynchedTargetNodesRecursively for SynchMemberNodes
//              synchService.sanitiseSynchedTargetNode(synchMemberNodeRef);
//            }
//          }
//        //synchService.enableSynchBehaviours();
//       } 
//      }
//     }
//    if(logger.isDebugEnabled()) logger.debug("SynchChangeMonitor.beforeMoveNode method exited");
//  }

  
//  @Override
//  public void afterCreateVersion(NodeRef versionableNode, Version version) {
//    if(logger.isDebugEnabled()) logger.debug("SynchChangeMonitor.afterCreateVersion method entered");
//    //synchService.updateSynchMemberNodeAfterVersionCreate(versionableNode);
//    if(versionableNode != null && nodeService.exists(versionableNode)){
//      synchService.disableSynchBehaviours();
//      nodeService.setProperty(versionableNode, SeedSynchModel.PROP_HAS_CONTENT_CHANGED, true);
//      nodeService.setProperty(versionableNode, SeedSynchModel.PROP_HAS_PROPERTIES_CHANGED, false);
//      nodeService.setProperty(versionableNode, SeedSynchModel.PROP_IS_SYNCHED, false);
//      synchService.enableSynchBehaviours();
//    }
//    if(logger.isDebugEnabled()) logger.debug("SynchChangeMonitor.afterCreateVersion method exited");
//    
//    
//  }

  private HashSet<QName> recordNonContentPropertiesUpdate(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after)
  {
      // 'changed' here will mean added, removed or edited/changed.
      // We don't (yet) distinguish.
      HashSet<QName> changedProps = new HashSet<QName>();
      changedProps.addAll(PropertyMap.getAddedProperties(before, after).keySet());
      changedProps.addAll(PropertyMap.getRemovedProperties(before, after).keySet());
      changedProps.addAll(PropertyMap.getChangedProperties(before, after).keySet());
      
      //syncEventHandler.persistAuditChangesPropertiesChanged(nodeRef, changedProps);
      return changedProps;
  }
  

  public void setAlfrescoServices(AlfrescoServices alfrescoServices) {
    
    policyComponent = alfrescoServices.getPolicyComponent();
    nodeService = alfrescoServices.getNodeService();
    fileFolderService = alfrescoServices.getFileFolderService();
    transactionService = alfrescoServices.getTransactionService();

  }

  public SynchService getSynchService() {
    return synchService;
  }

  public void setSynchService(SynchService synchService) {
    this.synchService = synchService;
  }

  public List<String> getPropertiesToTrack() {
    return propertiesToTrack;
  }

  public void setPropertiesToTrack(List<String> propertiesToTrack) {
    this.propertiesToTrack = propertiesToTrack;
  }

  public Boolean getDeleteNodeStatus() {
    return deleteNodeStatus;
  }

  public void setDeleteNodeStatus(Boolean deleteNodeStatus) {
    this.deleteNodeStatus = deleteNodeStatus;
  }

 

 

 

  

 




 

 

}
