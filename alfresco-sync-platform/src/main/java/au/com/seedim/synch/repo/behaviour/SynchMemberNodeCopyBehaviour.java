package au.com.seedim.synch.repo.behaviour;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.alfresco.repo.copy.CopyBehaviourCallback;
import org.alfresco.repo.copy.CopyDetails;
import org.alfresco.repo.copy.CopyServicePolicies.OnCopyNodePolicy;
import org.alfresco.repo.copy.DefaultCopyBehaviourCallback;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.log4j.Logger;

import au.com.seedim.synch.model.SeedSynchModel;
import au.com.seedim.synch.repo.services.AlfrescoServices;
import au.com.seedim.synch.repo.services.SynchService;

public class SynchMemberNodeCopyBehaviour implements OnCopyNodePolicy{

  private static final Logger logger = Logger.getLogger(SynchMemberNodeCopyBehaviour.class);
  private PolicyComponent policyComponent;
  private NodeService nodeService;
  private SynchService synchService;
  
  public void init() {
    
    if(logger.isDebugEnabled()) logger.debug("Initialising OnCopyNode behaviour");
    this.policyComponent.bindClassBehaviour(
        OnCopyNodePolicy.QNAME,
        SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE,
        new JavaBehaviour(this, "getCopyCallback", NotificationFrequency.EVERY_EVENT));
    
//    this.policyComponent.bindClassBehaviour(
//        OnCopyNodePolicy.QNAME,
//        SeedSynchModel.ASPECT_SYNCH_SOURCE,
//        new JavaBehaviour(this, "getCopyCallback", NotificationFrequency.EVERY_EVENT));
    
    this.policyComponent.bindClassBehaviour(
        OnCopyNodePolicy.QNAME,
        SeedSynchModel.ASPECT_SYNCH_TARGET,
        new JavaBehaviour(this, "getCopyCallback", NotificationFrequency.EVERY_EVENT));
  }
    
  @Override
  public CopyBehaviourCallback getCopyCallback(QName classRef, CopyDetails copyDetails) {
    // TODO Auto-generated method stub
    if(logger.isDebugEnabled()) logger.debug("SynchMemberNodeCopyBehaviour CopyBehaviourCallback method entered");
    return SynchMemberNodeCopyBehaviourCallback.INSTANCE;
  }
  
  private static class SynchMemberNodeCopyBehaviourCallback extends DefaultCopyBehaviourCallback{
    
    private static final CopyBehaviourCallback INSTANCE = new SynchMemberNodeCopyBehaviourCallback();
    
    
    /**
     * Disallows copying of the synch:synchMemberNodeAspect, aspect.
     */
    @Override
    public boolean getMustCopy(QName classQName, CopyDetails copyDetails)
    {
        if (classQName.equals(SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE))
        {
            return false;
        }
        else if (classQName.equals(SeedSynchModel.ASPECT_SYNCH_SOURCE))
        {
            return false;
        }
        else if (classQName.equals(SeedSynchModel.ASPECT_SYNCH_TARGET))
        {
            return false;
        }
        else
        {
            return true;
        }
    }
    
    @Override
    public Map<QName, Serializable> getCopyProperties(QName classQName, CopyDetails copyDetails, Map<QName, Serializable> properties)
    {
      if (classQName.equals(SeedSynchModel.ASPECT_SYNCH_MEMBER_NODE))
      {
          return Collections.emptyMap();
      }
      else if (classQName.equals(SeedSynchModel.ASPECT_SYNCH_SOURCE))
      {
          return Collections.emptyMap();
      }
      else if (classQName.equals(SeedSynchModel.ASPECT_SYNCH_TARGET))
      {
          
        
            return Collections.emptyMap();
      }
      else
      {
          return properties;
      }
      
    }
    @Override
    public Pair<AssocCopySourceAction, AssocCopyTargetAction> getAssociationCopyAction(QName classQName,CopyDetails copyDetails,CopyAssociationDetails assocCopyDetails)
    {
        
      if (classQName.equals(SeedSynchModel.ASPECT_SYNCH_SOURCE))
      {
          return new Pair<AssocCopySourceAction, AssocCopyTargetAction>(
            AssocCopySourceAction.IGNORE,
            AssocCopyTargetAction.USE_COPIED_OTHERWISE_ORIGINAL_TARGET);
      }
      else if (classQName.equals(SeedSynchModel.ASPECT_SYNCH_TARGET))
      {
          return new Pair<AssocCopySourceAction, AssocCopyTargetAction>(
            AssocCopySourceAction.IGNORE,
            AssocCopyTargetAction.USE_COPIED_OTHERWISE_ORIGINAL_TARGET);
      }else{
          return new Pair<AssocCopySourceAction, AssocCopyTargetAction>(
            AssocCopySourceAction.COPY_REMOVE_EXISTING,
            AssocCopyTargetAction.USE_COPIED_OTHERWISE_ORIGINAL_TARGET);
      }
      
      
    }     
  }
  
  public void setAlfrescoServices(AlfrescoServices alfrescoServices) {
    
    policyComponent = alfrescoServices.getPolicyComponent();
    nodeService = alfrescoServices.getNodeService();

  }

  public SynchService getSynchService() {
    return synchService;
  }

  public void setSynchService(SynchService synchService) {
    this.synchService = synchService;
  }

}
