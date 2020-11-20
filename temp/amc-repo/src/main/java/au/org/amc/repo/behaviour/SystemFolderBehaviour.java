package au.org.amc.repo.behaviour;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateChildAssociationPolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnUpdatePropertiesPolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;

import au.org.amc.model.AMCModel;

/**
 * Content policy to react to a new System Folder being created.
 * In this behaviour, we will set the permission rules and de-specialise 
 * the node to a cm:folder if it wasn't marked as a System Folder. 
 * 
 */
public class SystemFolderBehaviour implements OnCreateNodePolicy, OnUpdatePropertiesPolicy, OnCreateChildAssociationPolicy {

	private static final Logger logger = Logger.getLogger(SystemFolderBehaviour.class);
	
	private DictionaryService dictionaryService;
	
	private PermissionService permissionService;
	
	private PolicyComponent policyComponent;

	private NodeService nodeService;
	
	private PersonService personService;

	private String systemFolderManagerAuthorityName;

	private String systemFolderPermission = "SiteConsumer";
	
	public String getSystemFolderPermission() {
		return systemFolderPermission;
	}

	public void setSystemFolderPermission(String systemFolderPermission) {
		this.systemFolderPermission = systemFolderPermission;
	}

	public DictionaryService getDictionaryService() {
		return dictionaryService;
	}

	public void setDictionaryService(DictionaryService dictionaryService) {
		this.dictionaryService = dictionaryService;
	}

	public String getSystemFolderManagerAuthorityName() {
		return systemFolderManagerAuthorityName;
	}

	public void setSystemFolderManagerAuthorityName(
			String systemFolderManagerAuthorityName) {
		this.systemFolderManagerAuthorityName = systemFolderManagerAuthorityName;
	}

	public PolicyComponent getPolicyComponent() {
		return policyComponent;
	}

	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}

	public NodeService getNodeService() {
		return nodeService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public PersonService getPersonService() {
		return personService;
	}

	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}
	
	public PermissionService getPermissionService() {
		return permissionService;
	}

	public void setPermissionService(PermissionService permissionService) {
		this.permissionService = permissionService;
	}

	public void init() {
		
		if(logger.isDebugEnabled()) logger.debug("Running Policy SystemFolderBehaviour.init()");
		
		// Bind to the event of creating new System Folders
		this.policyComponent.bindClassBehaviour(
				OnCreateNodePolicy.QNAME,
				AMCModel.TYPE_AMC_SYSTEM_FOLDER,
				new JavaBehaviour(
						this, 
						"onCreateNode", 
						NotificationFrequency.TRANSACTION_COMMIT)
		);
		
		// Bind to the event of updating System Folders
		this.policyComponent.bindClassBehaviour(
				OnUpdatePropertiesPolicy.QNAME,
				AMCModel.TYPE_AMC_SYSTEM_FOLDER,
				new JavaBehaviour(
						this, 
						"onUpdateProperties", 
						NotificationFrequency.TRANSACTION_COMMIT)
		);
		
		// Bind to the event of creating new children in System Folders
		this.policyComponent.bindAssociationBehaviour(
				OnCreateChildAssociationPolicy.QNAME, 
				AMCModel.TYPE_AMC_SYSTEM_FOLDER, 
				ContentModel.ASSOC_CONTAINS, 
				new JavaBehaviour(
						this, 
						"onCreateChildAssociation", 
						NotificationFrequency.TRANSACTION_COMMIT));
	}
	
	@Override
	public void onCreateNode(ChildAssociationRef childAssocRef) {
		
		if(logger.isDebugEnabled()) logger.debug("Entered Policy SystemFolderBehaviour.onCreateNode");
		
		try{
			
			NodeRef createdNode = childAssocRef.getChildRef();
			setSystemFolderPermissions(createdNode);			
		}
		finally{
			if(logger.isDebugEnabled()) logger.debug("Exited Policy SystemFolderBehaviour.onCreateNode");
		}
	}

	/**
	 * Set the appropriate permissions for this System Folder.
	 * @param nodeRef The {@link NodeRef} of the System Folder.
	 */
	private void setSystemFolderPermissions(NodeRef nodeRef) {
		
		if(logger.isDebugEnabled()) logger.debug("Running Policy SystemFolderBehaviour.setSystemFolderPermissions");
		
		// 1. System Folder Managers should have coordinator access.
		permissionService.setPermission(
				nodeRef, 
				systemFolderManagerAuthorityName, 
				PermissionService.COORDINATOR, true);
		
		// 2. All other roles should only have Consumer Access.
		// NOTE: Also allow "ADD_CHILDREN" if the property has been set.
		Set<AccessPermission> permissions = 
			permissionService.getAllSetPermissions(nodeRef);
		Boolean addChildren = (Boolean) nodeService.getProperty(
			nodeRef, AMCModel.PROP_CAN_CHILDREN_BE_ADDED);
		
		for (AccessPermission permission : permissions) {
			
			if(logger.isDebugEnabled()) logger.debug("SystemFolderBehaviour.setSystemFolderPermissions: " + permission.getAuthority());
			
			// BON leave the permission as it should be if its the default site consumer Group or the systemFolderManager group or Everyone
			if (permission.getAuthority().equals(systemFolderManagerAuthorityName)) {
				
				if(logger.isDebugEnabled()) logger.debug("SystemFolderBehaviour.setSystemFolderPermissions: Continue");
				continue;
			}
			
			String authority = permission.getAuthority();
			// Remove existing permission.
			permissionService.clearPermission(nodeRef, authority);
			
			// Add the configured access permission (SiteConsumer).
			permissionService.setPermission(
					nodeRef, 
					authority, 
					systemFolderPermission, true);
						
			// Check if add children has been set as a property.  Also don't set it for the consumer group
			if (addChildren)
			{
				if(!(authority.contains("_SiteConsumer") || authority.equals("GROUP_EVERYONE"))) 
				{	
			
	//				permissionService.setPermission(
	//					nodeRef, 
	//					authority, 
	//					PermissionService.ADD_CHILDREN, 
	//					true);

					permissionService.setPermission(
							nodeRef, 
							authority, 
							PermissionService.CREATE_CHILDREN, 
							true);
				
	//				permissionService.setPermission(
	//					nodeRef, 
	//					authority, 
	//					PermissionService.READ_PERMISSIONS, 
	//					true);
				}
			}
		}
		// 3. Break inheritance
		permissionService.setInheritParentPermissions(nodeRef, false);
	}
	
	@Override
	public void onUpdateProperties(final NodeRef nodeRef,
			Map<QName, Serializable> before, Map<QName, Serializable> after) {
		
		if(logger.isDebugEnabled()) logger.debug("Running Policy SystemFolderBehaviour.onUpdateProperties");
		
		// Ignore if it is not a system folder.
		Boolean isSystemFolder = (Boolean) after.get(AMCModel.PROP_IS_SYSTEM_FOLDER);
		
		if (isSystemFolder != null && isSystemFolder) {
			if(logger.isDebugEnabled()) logger.debug("Running Policy SystemFolderBehaviour.onUpdateProperties It is a systemFolder");
			// Check if we need to reset the permissions again.
			Boolean beforeChildrenVal = (Boolean) before.get(AMCModel.PROP_CAN_CHILDREN_BE_ADDED);
			Boolean afterChildrenVal = (Boolean) after.get(AMCModel.PROP_CAN_CHILDREN_BE_ADDED);
			
			Boolean beforeIsSystemFolderVal = (Boolean) before.get(AMCModel.PROP_IS_SYSTEM_FOLDER);
			Boolean afterIsSystemFolderVal = (Boolean) after.get(AMCModel.PROP_IS_SYSTEM_FOLDER);
			
			if(logger.isDebugEnabled()) logger.debug("Running Policy SystemFolderBehaviour.onUpdateProperties beforeVal = " + beforeChildrenVal + "afterVal = " + afterChildrenVal);
			
			if ((beforeChildrenVal == null && afterChildrenVal != null) 
					|| !(beforeChildrenVal.equals(afterChildrenVal)) 
					|| (beforeIsSystemFolderVal == null && afterIsSystemFolderVal != null) 
					|| !(beforeIsSystemFolderVal.equals(afterIsSystemFolderVal)) ) {
				//Added if user is not admin but System Folder Manager
				AuthenticationUtil.runAs(new RunAsWork<Object>() {
					@Override
					public Object doWork() throws Exception {
						setSystemFolderPermissions(nodeRef);
						return null;
					}
					
				}, AuthenticationUtil.getSystemUserName());
				
				
			}			
		}
	}

	@Override
	public void onCreateChildAssociation(final ChildAssociationRef childAssocRef,
			boolean isNewNode) {
		final NodeRef newChild = childAssocRef.getChildRef();
		
		if(logger.isDebugEnabled()) logger.debug("Running Policy SystemFolderBehaviour.onCreateChildAssociation");
		
		QName type = nodeService.getType(newChild);
		if (dictionaryService.isSubClass(type, ContentModel.TYPE_CONTENT)) {
			throw new IllegalStateException("Unable to create content nodes in a System Folder");
		}

		// Reset the permissions of the child node to be that of the parent 
		// of the System Folder.
		AuthenticationUtil.runAs(new RunAsWork<Object>() {
			@Override
			public Object doWork() throws Exception {
				permissionService.setInheritParentPermissions(newChild, false);

				NodeRef parent = childAssocRef.getParentRef();
				NodeRef toInheritFrom = nodeService.getPrimaryParent(parent).getParentRef();
				
				// Check the type of node we are inheriting from.
				while (nodeService.getType(toInheritFrom).equals(AMCModel.TYPE_AMC_SYSTEM_FOLDER)) {
					toInheritFrom = nodeService.getPrimaryParent(toInheritFrom).getParentRef();
				}
				
				// Get the permissions from the grandParent.
				Set<AccessPermission> permissions = 
					permissionService.getAllSetPermissions(toInheritFrom);
				
				for (AccessPermission grandParentPermission : permissions) {
					
					String authority = grandParentPermission.getAuthority();
					String permission = grandParentPermission.getPermission();
					boolean allow = grandParentPermission.getAccessStatus() == AccessStatus.ALLOWED;
					permissionService.clearPermission(newChild, authority);
					permissionService.setPermission(newChild, authority, permission, allow);
				}
				return null;
			}
			
		}, AuthenticationUtil.getSystemUserName());
		
		
		// TODO:
	}
	
	
}
