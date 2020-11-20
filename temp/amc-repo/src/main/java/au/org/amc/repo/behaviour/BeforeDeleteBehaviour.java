package au.org.amc.repo.behaviour;

import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteService;
import org.apache.log4j.Logger;

public class BeforeDeleteBehaviour implements NodeServicePolicies.BeforeDeleteNodePolicy {

  
  private static final Logger logger = Logger.getLogger(BeforeDeleteBehaviour.class);

  private PolicyComponent policyComponent;
  protected PersonService personService;
  protected AuthorityService authorityService;
  protected NodeService nodeService;
  protected SiteService siteService;
  protected OwnableService ownableService;
  
  
  public void init() {
    // append the deletion check to any document or folder 
 
    JavaBehaviour javaBehaviour = new JavaBehaviour(this, "beforeDeleteNode",
        Behaviour.NotificationFrequency.FIRST_EVENT);
    
    policyComponent.bindClassBehaviour(
        NodeServicePolicies.BeforeDeleteNodePolicy.QNAME,
        ContentModel.TYPE_FOLDER,
        javaBehaviour);
    policyComponent.bindClassBehaviour(
        NodeServicePolicies.BeforeDeleteNodePolicy.QNAME,
        ContentModel.TYPE_CONTENT,
        javaBehaviour);
    logger.debug("Initialised : BeforeDeleteBehaviour");
  }
  
  @SuppressWarnings("unused")
  @Override
  public void beforeDeleteNode(NodeRef nodeRef) {

    logger.debug("BeforeDeleteBehaviour entered");

    try {
      final String userId = AuthenticationUtil.getFullyAuthenticatedUser();
      final String systemUser = AuthenticationUtil.getSystemUserName();
      if(userId.matches(systemUser)){
        logger.debug("User is system, Allow deletion");
        return;
      }
      // allow deletion for users if they are canceling editing of a working copy
      if(nodeService.hasAspect(nodeRef, ContentModel.ASPECT_WORKING_COPY)){
        logger.debug("Node is working copy");
        return;
      }
      Set<String> authorities = authorityService.getAuthoritiesForUser(userId);
      if (logger.isDebugEnabled()) {
        String msg = String.format("Authorities for user %s", authorities.toString());
        logger.debug(msg);
      }
      String siteShortname = siteService.getSiteShortName(nodeRef);
      String sitePowerUserGroup = "GROUP_site_" + siteShortname + "_SitePowerUser";
      String siteManagerGroup = "GROUP_site_" + siteShortname + "_SiteManager";
      
      boolean isPowerUser = authorities.contains(sitePowerUserGroup);
      logger.debug("isPowerUser:" + isPowerUser);
      boolean isAdmin = authorities.contains("GROUP_ALFRESCO_ADMINISTRATORS");
      logger.debug("isAdmin:" + isAdmin );
      boolean isSiteAdmin = authorities.contains("GROUP_SITE_ADMINISTRATORS");
      logger.debug("isSiteAdmin:" + isSiteAdmin );
      boolean isSiteManager = authorities.contains(siteManagerGroup);
      logger.debug("isSiteManager:" + isSiteManager );
      boolean isOwner = false;
      
      if(ownableService.hasOwner(nodeRef)){
      
        if(userId.matches(ownableService.getOwner(nodeRef))){
           isOwner = true;
           logger.debug("isOwner:" + isOwner);
        }
      }
      if (authorities != null && !(isAdmin || isSiteManager || isOwner || isSiteAdmin)) {
        logger.debug("Prevent Deletion");
        String msg = String.format("User %s is not allowed to delete node:" + nodeRef, userId);
        throw new AlfrescoRuntimeException(msg);
      }else{
        logger.debug("Allow Deletion");
      }
      
    }finally{
      logger.debug("BeforeDeleteBehaviour exited");
    }
    
  }

  public PolicyComponent getPolicyComponent() {
    return policyComponent;
  }

  public void setPolicyComponent(PolicyComponent policyComponent) {
    this.policyComponent = policyComponent;
  }

  public PersonService getPersonService() {
    return personService;
  }

  public void setPersonService(PersonService personService) {
    this.personService = personService;
  }

  public AuthorityService getAuthorityService() {
    return authorityService;
  }

  public void setAuthorityService(AuthorityService authorityService) {
    this.authorityService = authorityService;
  }

  public NodeService getNodeService() {
    return nodeService;
  }

  public void setNodeService(NodeService nodeService) {
    this.nodeService = nodeService;
  }

  public SiteService getSiteService() {
    return siteService;
  }

  public void setSiteService(SiteService siteService) {
    this.siteService = siteService;
  }

  public OwnableService getOwnableService() {
    return ownableService;
  }

  public void setOwnableService(OwnableService ownableService) {
    this.ownableService = ownableService;
  }

}
