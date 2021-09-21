package au.com.seedim.synch.repo.behaviour;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies.BeforeDeleteNodePolicy;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.transaction.TransactionListener;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.log4j.Logger;

import au.com.seedim.synch.model.SeedSynchModel;
import au.com.seedim.synch.repo.services.AlfrescoServices;
import au.com.seedim.synch.repo.services.SynchService;

public class SynchMemberNodeDeleteBehaviour implements BeforeDeleteNodePolicy{
  
  private static final Logger logger = Logger.getLogger(SynchMemberNodeDeleteBehaviour.class);
  
  
//Key to identify resources associated to transaction
  private static final String KEY_RELATED_NODES = 
      SynchMemberNodeDeleteBehaviour.class.getName() + ".relatedNodes";
  
  
  private PolicyComponent policyComponent;
  private NodeService nodeService;
  private SynchService synchService;
  private TransactionService transactionService;
  private TransactionListener transactionListener;
  // Bind behaviour and initialize transaction listener
  public void init() {
       
    if(logger.isDebugEnabled()) logger.debug("Initialising beforeDeleteNode behaviour");
    //Synch Member Node Delete
    this.policyComponent.bindClassBehaviour(
        BeforeDeleteNodePolicy.QNAME, 
        SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE, 
        new JavaBehaviour(this, "beforeDeleteNode", NotificationFrequency.FIRST_EVENT));
//    
//    this.transactionListener = new RelatedNodesTransactionListener();
//    
    //Synch Target Node Delete
    this.policyComponent.bindClassBehaviour(
        BeforeDeleteNodePolicy.QNAME, 
        SeedSynchModel.ASPECT_SYNCH_TARGET, 
        new JavaBehaviour(this, "beforeDeleteNode", NotificationFrequency.FIRST_EVENT));
//       
  }


  
  @Override
  public void beforeDeleteNode(NodeRef sourceNodeRef) {
    
    // TODO Auto-generated method stub
    if(logger.isDebugEnabled()) logger.debug("Entered SynchMemberNodeDeleteBehaviour.beforeDeleteNode ");
    
    if(sourceNodeRef != null && nodeService.exists(sourceNodeRef)){
      //prevent Synch Source or Target Root Folder move
      if(nodeService.hasAspect(sourceNodeRef, SeedSynchModel.ASPECT_SYNCH_SOURCE) || nodeService.hasAspect(sourceNodeRef, SeedSynchModel.ASPECT_SYNCH_TARGET)){
        throw new AlfrescoRuntimeException("delete is not allowed for Synch Source or Target. Attempted to delete " + sourceNodeRef);
      }
    
      try{
        
        // get associated target ref
        String targetRefString = (String)nodeService.getProperty(sourceNodeRef, SeedSynchModel.PROP_SYNCED_TARGET_REF);
        NodeRef targetNodeRef = targetRefString != null? new NodeRef(targetRefString) : null;
      
        if(targetNodeRef != null && nodeService.exists(targetNodeRef)){
          //nodeService.deleteNode(targetNodeRef);
        	this.synchService.deleteNode(targetNodeRef);
        }
      
       }catch (Exception e){
          e.printStackTrace();  
          throw new AlfrescoRuntimeException(
              "SynchMemberNodeDeleteBehaviour.beforeDeleteNode exception: " + e);
        }
      }
    }

  
 /** @Override
  public void beforeDeleteNode(NodeRef nodeRef) {
    // TODO Auto-generated method stub
    if(logger.isDebugEnabled()) logger.debug("Entered SynchMemberNodeDeleteBehaviour.beforeDeleteNode ");
 // Bind listener to current transaction
    AlfrescoTransactionSupport.bindListener(transactionListener);

    // Get some related nodes to work with
    List<NodeRef> relatedNodes = new ArrayList<NodeRef>();
    
    if (nodeService.exists(nodeRef) && !nodeService.hasAspect(nodeRef, ContentModel.ASPECT_WORKING_COPY))
    {
      //Prevent delete of Synch Target Node
      if(nodeService.hasAspect(nodeRef, SeedSynchModel.ASPECT_SYNCH_TARGET)){
        if(logger.isDebugEnabled()) logger.debug("SynchMemberNodeDeleteBehaviour.beforeDeleteNode Synch Target Node cannot be deleted if still synched ");
        throw new AlfrescoRuntimeException(
            "message_CANNOT_DELETE_SYNCH_TARGET_NODE");
      }
      try {
          //Get SSID from parent
          Map<QName, Serializable> memberNodeProps = nodeService.getProperties(nodeRef);
          //String ssdId = (String) nodeService.getProperty(parentNodeRef, SeedSynchModel.PROP_SYNCH_DEF_NODE_ID);
          String ssdId = (String) memberNodeProps.get(SeedSynchModel.PROP_SYNCH_DEF_NODE_ID);
          List<ChildAssociationRef> parentAssocs = nodeService.getParentAssocs(nodeRef);
          //try parents
           if(!parentAssocs.isEmpty()){
              NodeRef parentNodeRef = parentAssocs.get(0).getParentRef();
              if(logger.isDebugEnabled()) logger.debug("SSID found " + ssdId); 
              //get TargetRootFolder based on ssdID 
              NodeRef targetSynchRootFolder = synchService.getTargetSynchRootFolderNode(ssdId);
              //Get Parent Name and SyncMemberNode Name
              String parentName = (String) nodeService.getProperty(parentNodeRef, ContentModel.PROP_NAME);;
              String synchMemberNodeName = (String) memberNodeProps.get(ContentModel.PROP_NAME);
              NodeRef synchTargetNode = null;
              //and then recursive try to find node to deleted 
              synchTargetNode = synchService.findSynchTargetNode(targetSynchRootFolder, parentName, synchMemberNodeName);
              
              if(synchTargetNode != null && nodeService.exists(synchTargetNode)){
                if(logger.isDebugEnabled()) logger.debug("SynchMemberNodeDeleteBehaviour.beforeDeleteNode adding node to delete: " + synchTargetNode);
                relatedNodes.add(synchTargetNode);
              }else{
                if(logger.isDebugEnabled()) logger.debug("SynchMemberNodeDeleteBehaviour.beforeDeleteNode synchTargetNode not found!!S");
              }
           }
      }catch (FileNotFoundException fe) {
        //e.printStackTrace();
        throw new AlfrescoRuntimeException(
            "SynchMemberNodeDeleteBehaviour.beforeDeleteNode: " + fe);
      }catch (Exception e){
        e.printStackTrace();  
        throw new AlfrescoRuntimeException(
            "SynchMemberNodeDeleteBehaviour.beforeDeleteNode exception: " + e);
      }
    }
 
    // Transactions involving several nodes need resource updating
    List<NodeRef> currentRelatedNodes = AlfrescoTransactionSupport.getResource(KEY_RELATED_NODES);
    if (currentRelatedNodes == null) {
        currentRelatedNodes = relatedNodes;
    } else {
        currentRelatedNodes.addAll(relatedNodes);
    }
     
    // Put resources to be used in transaction listener
    AlfrescoTransactionSupport.bindResource(KEY_RELATED_NODES, currentRelatedNodes);
    
    if(logger.isDebugEnabled()) logger.debug("Exited SynchMemberNodeDeleteBehaviour.beforeDeleteNode ");
  }
  
  //removed extends TransactionListenerAdapter since deprecated
//Listening "afterCommit" transaction event
  private class RelatedNodesTransactionListener implements TransactionListener {

      @Override
      public void afterCommit() {
        if(logger.isDebugEnabled()) logger.debug("Entered SynchMemberNodeDeleteBehaviour.RelatedNodesTransactionListener afterCommit method");
        ExecutorService executorService = Executors.newFixedThreadPool(5);
          @SuppressWarnings("unchecked")
          List<NodeRef> nodesToBeReviewed = 
              (List<NodeRef>) AlfrescoTransactionSupport.getResource(KEY_RELATED_NODES);
          if (nodesToBeReviewed != null) {
              for (NodeRef nodeToBeReviewed : nodesToBeReviewed) {
                  // Launch every node work in a different thread
                if(nodeToBeReviewed != null && nodeService.exists(nodeToBeReviewed)){
                  Runnable runnable = new RelatedNodeDeletion(nodeToBeReviewed);
                  if(logger.isDebugEnabled()) logger.debug("SynchMemberNodeDeleteBehaviour.RelatedNodesTransactionListener executing runnable " + nodeToBeReviewed);
                  //threadPoolExecutor.execute(runnable);
                  executorService.execute(runnable);
                }
              }
              executorService.shutdown();
          }else{
            if(logger.isDebugEnabled()) logger.debug("SynchMemberNodeDeleteBehaviour.RelatedNodesTransactionListener: No node to process");
          }
      }
       
      @Override
      public void flush() {
      }

      @Override
      public void afterRollback() {
        // TODO Auto-generated method stub
        
      }

      @Override
      public void beforeCommit(boolean arg0) {
        // TODO Auto-generated method stub
        
      }

      @Override
      public void beforeCompletion() {
        // TODO Auto-generated method stub
        
      }
       
  }
//Thread to work with an individual node
  private class RelatedNodeDeletion implements Runnable {
       
      private NodeRef nodeToBeReviewed;
       
      private RelatedNodeDeletion(NodeRef nodeToBeReviewed) {
          this.nodeToBeReviewed = nodeToBeReviewed;
      }

      @Override
      public void run() {
        if(logger.isDebugEnabled()) logger.debug("Entered SynchMemberNodeDeleteBehaviour.RelatedNodeDeletion run method");
          AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
               
              public Void doWork() throws Exception {
                   
                  RetryingTransactionCallback<Void> callback = new RetryingTransactionCallback<Void>() {
                       
                      @Override
                      public Void execute() throws Throwable {
                        if(logger.isDebugEnabled()) logger.debug("SynchMemberNodeDeleteBehaviour.RelatedNodeDeletion deleting node.... " + nodeToBeReviewed );
                          nodeService.deleteNode(nodeToBeReviewed);
                          return null;
                      }
                  };
                   
                  try {
                      RetryingTransactionHelper txnHelper = 
                          transactionService.getRetryingTransactionHelper();
                      txnHelper.doInTransaction(callback, false, true);
                  } catch (Throwable e) {
                      e.printStackTrace();
                  }
                   
                  return null;
                   
              }
          });
      }
       
  }**/
  
  public void setAlfrescoServices(AlfrescoServices alfrescoServices) {
    
    policyComponent = alfrescoServices.getPolicyComponent();
    nodeService = alfrescoServices.getNodeService();
    transactionService = alfrescoServices.getTransactionService();

  }

  public SynchService getSynchService() {
    return synchService;
  }

  public void setSynchService(SynchService synchService) {
    this.synchService = synchService;
  }

}
