package au.org.amc.seedim.action;

import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;

import au.org.amc.model.AMCModel;


public class CreateSystemFolder extends ActionExecuterAbstractBase  {

	
	protected NodeService nodeService;
	private BehaviourFilter behaviourFilter;
	public static final QName TYPE_CMFOLDER = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "folder");
	private static Log logger = LogFactory.getLog(CreateSystemFolder.class);
	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		
		if (logger.isDebugEnabled()) logger.debug("Entered CreateSystemFolder executeImpl method");
		
		if((nodeService.getType(actionedUponNodeRef)).isMatch(TYPE_CMFOLDER)){		
			nodeService.setType(actionedUponNodeRef, AMCModel.TYPE_AMC_SYSTEM_FOLDER);
		}
		if((nodeService.getType(actionedUponNodeRef)).isMatch(AMCModel.TYPE_AMC_SYSTEM_FOLDER)){
			//Set Allow users to added subfolders to true
			nodeService.setProperty(actionedUponNodeRef, AMCModel.PROP_CAN_CHILDREN_BE_ADDED, true);
			if (logger.isDebugEnabled()) logger.debug("Can children be added: " + nodeService.getProperty(actionedUponNodeRef, AMCModel.PROP_CAN_CHILDREN_BE_ADDED));
			//Change folder ownership
			// Disable auditable aspect to allow change properties of cm:auditable aspect
			this.behaviourFilter.disableBehaviour(actionedUponNodeRef, ContentModel.ASPECT_AUDITABLE);

			// Update properties of cm:auditable aspect
			String systemUser = AuthenticationUtil.getSystemUserName();
			if (logger.isDebugEnabled()) logger.debug("System User: " + systemUser);
			nodeService.setProperty(actionedUponNodeRef, ContentModel.PROP_CREATOR,systemUser);
			nodeService.setProperty(actionedUponNodeRef, ContentModel.PROP_MODIFIER,systemUser);
			if (logger.isDebugEnabled()) logger.debug("Folder Creator: " + nodeService.getProperty(actionedUponNodeRef, ContentModel.PROP_CREATOR));
			// Enable auditable aspect
			this.behaviourFilter.enableBehaviour(actionedUponNodeRef, ContentModel.ASPECT_AUDITABLE);
		}
		if (logger.isDebugEnabled()) logger.debug("Exited CreateSystemFolder executeImpl method");
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		// TODO Auto-generated method stub

	}
	public NodeService getNodeService() {
		return nodeService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	public BehaviourFilter getBehaviourFilter() {
		return behaviourFilter;
	}

	public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
		this.behaviourFilter = behaviourFilter;
	}

}
