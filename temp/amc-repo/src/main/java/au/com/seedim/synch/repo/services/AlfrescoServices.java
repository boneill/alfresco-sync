package au.com.seedim.synch.repo.services;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import org.alfresco.repo.lock.JobLockService;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.transaction.TransactionListener;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.beanutils.PropertyUtils;


public class AlfrescoServices {
  
  // ALFRESCO SERVICES
  private AuthenticationService authenticationService;
  private OwnableService ownableService;
  private ContentService contentService;
  private DictionaryService dictionaryService;
  private FileFolderService fileFolderService;
  private PermissionService permissionService;
  private WorkflowService workflowService;
  private SiteService siteService;
  private NodeService nodeService;
  private SearchService searchService;
  private PersonService personService;
  private NamespaceService namespaceService;
  private TransactionService transactionService;
  private ServiceRegistry serviceRegistry;
  private MimetypeService mimetypeService;
  private BehaviourFilter policyBehaviourFilter;
  private LockService lockService;
  private JobLockService jobLockService;
  private ActionService actionService;
  private Repository repositoryHelper;
  private PolicyComponent policyComponent;
  private VersionService versionService;
  private CopyService copyService;

  
  /**
   * Check if all the services are different of null
   * @throws ReflectiveOperationException
   */
  public void init() throws ReflectiveOperationException {
    Map<String, Object> getter = PropertyUtils.describe(this);
    
    for (String methodName : getter.keySet()) { 
      Object service = PropertyUtils.getProperty(this, methodName);
      if (service == null) {
        String msg = String.format("service %s=null", methodName);
        throw new NullPointerException(msg);
      }
    }
  }
  
  public AuthenticationService getAuthenticationService() {
    return authenticationService;
  }
  public void setAuthenticationService(
      AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }
  public ContentService getContentService() {
    return contentService;
  }
  public void setContentService(ContentService contentService) {
    this.contentService = contentService;
  }
  public DictionaryService getDictionaryService() {
    return dictionaryService;
  }
  public void setDictionaryService(DictionaryService dictionaryService) {
    this.dictionaryService = dictionaryService;
  }
  public FileFolderService getFileFolderService() {
    return fileFolderService;
  }
  public void setFileFolderService(FileFolderService fileFolderService) {
    this.fileFolderService = fileFolderService;
  }
  public PermissionService getPermissionService() {
    return permissionService;
  }
  public void setPermissionService(PermissionService permissionService) {
    this.permissionService = permissionService;
  }
  public WorkflowService getWorkflowService() {
    return workflowService;
  }
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }
  public SiteService getSiteService() {
    return siteService;
  }
  public void setSiteService(SiteService siteService) {
    this.siteService = siteService;
  }
  public NodeService getNodeService() {
    return nodeService;
  }
  public void setNodeService(NodeService nodeService) {
    this.nodeService = nodeService;
  }
  public SearchService getSearchService() {
    return searchService;
  }
  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }
  public PersonService getPersonService() {
    return personService;
  }
  public void setPersonService(PersonService personService) {
    this.personService = personService;
  }
  public NamespaceService getNamespaceService() {
    return namespaceService;
  }
  public void setNamespaceService(NamespaceService namespaceService) {
    this.namespaceService = namespaceService;
  }

  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  public TransactionService getTransactionService() {
    return transactionService;
  }

  public void setTransactionService(TransactionService transactionService) {
    this.transactionService = transactionService;
  }


  public MimetypeService getMimetypeService() {
    return mimetypeService;
  }

  public void setMimetypeService(MimetypeService mimetypeService) {
    this.mimetypeService = mimetypeService;
  }

  public OwnableService getOwnableService() {
    return ownableService;
  }

  public void setOwnableService(OwnableService ownableService) {
    this.ownableService = ownableService;
  }

  public BehaviourFilter getPolicyBehaviourFilter() {
    return policyBehaviourFilter;
  }

  public void setPolicyBehaviourFilter(BehaviourFilter policyBehaviourFilter) {
    this.policyBehaviourFilter = policyBehaviourFilter;
  }

  public LockService getLockService() {
    return lockService;
  }

  public void setLockService(LockService lockService) {
    this.lockService = lockService;
  }

  public JobLockService getJobLockService() {
    return jobLockService;
  }

  public void setJobLockService(JobLockService jobLockService) {
    this.jobLockService = jobLockService;
  }
  
  public ActionService getActionService() {
    return actionService;
  }

  public void setActionService(ActionService actionService) {
    this.actionService = actionService;
  }

  public Repository getRepositoryHelper() {
    return repositoryHelper;
  }

  public void setRepositoryHelper(Repository repositoryHelper) {
    this.repositoryHelper = repositoryHelper;
  }

  public PolicyComponent getPolicyComponent() {
    return policyComponent;
  }

  public void setPolicyComponent(PolicyComponent policyComponent) {
    this.policyComponent = policyComponent;
  }

  public VersionService getVersionService() {
    return versionService;
  }

  public void setVersionService(VersionService versionService) {
    this.versionService = versionService;
  }

  public CopyService getCopyService() {
    return copyService;
  }

  public void setCopyService(CopyService copyService) {
    this.copyService = copyService;
  }


}
