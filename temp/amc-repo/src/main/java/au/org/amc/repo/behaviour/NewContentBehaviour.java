package au.org.amc.repo.behaviour;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateNodePolicy;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;

import au.org.amc.model.AMCModel;

/**
 * Apply an ID to all documents created. 
 * 
 */
public class NewContentBehaviour implements OnCreateNodePolicy{

	private static final Logger logger = Logger
	.getLogger(NewContentBehaviour.class);
	
	private String repositoryId;

	private String padding;
	
	private String urlPrefix;
	
	private PolicyComponent policyComponent;

	private NodeService nodeService;
	
	public String getUrlPrefix() {
		return urlPrefix;
	}

	public void setUrlPrefix(String urlPrefix) {
		this.urlPrefix = urlPrefix;
	}

	public String getPadding() {
		return padding;
	}

	public void setPadding(String padding) {
		this.padding = padding;
	}

	public String getRepositoryId() {
		return repositoryId;
	}

	public void setRepositoryId(String repositoryId) {
		this.repositoryId = repositoryId;
	}

	public NodeService getNodeService() {
		return nodeService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public PolicyComponent getPolicyComponent() {
		return policyComponent;
	}

	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}

	public void init() {
		// Check if the repositoryId has been set.
		if (repositoryId != null && repositoryId.charAt(0) == '{') {
			// Assign it 1.
			repositoryId = String.valueOf(1);
		}
		
		// Register behaviour for ALL content in the repository
		/**this.policyComponent.bindClassBehaviour(
				OnCreateNodePolicy.QNAME, 
				ContentModel.TYPE_CONTENT, 
				new JavaBehaviour(this, "onCreateNode",
						NotificationFrequency.TRANSACTION_COMMIT) {			
		});**/
		//Modified due to modifier being changed when doing bulk import
		this.policyComponent.bindClassBehaviour(
        OnCreateNodePolicy.QNAME, 
        ContentModel.TYPE_CONTENT, 
        new JavaBehaviour(this, "onCreateNode",
            NotificationFrequency.FIRST_EVENT) {     
    });
	}
	
	@Override
	public void onCreateNode(ChildAssociationRef childAssocRef) {
		
		if(logger.isDebugEnabled()) {
			logger.debug("Running Policy NewContentBehaviour.onCreateNode");
		}
		
		// Check if the aspect has been applied, if not apply it with a new value. 
		NodeRef newNode = childAssocRef.getChildRef();
		// We ignore "working copies" and "thumbnails"
		if (nodeService.exists(newNode) && 
			!nodeService.hasAspect(newNode, ContentModel.ASPECT_WORKING_COPY) &&
			!nodeService.getType(newNode).equals(ContentModel.TYPE_THUMBNAIL)) {
		  if(logger.isDebugEnabled()) {
	      logger.debug("NodeRef: " + newNode);
	    }
			String sequence = String.valueOf(nodeService.getProperty(
					newNode, ContentModel.PROP_NODE_DBID));
			
			// parse the sequence to int and convert to hexadecimal value
			String hexIdentifier = Integer.toHexString(Integer.parseInt(sequence));
			
			// get current year
			int currentYear = Calendar.getInstance().get(Calendar.YEAR);
			
			// construct the AMC content Id
			String id = repositoryId + "-" + currentYear + "-" + hexIdentifier;
			
			if (!nodeService.hasAspect(
						newNode, AMCModel.ASPECT_AMC_IDENTIFIABLE)) {
				// Assign an id
				Map<QName, Serializable> props = new HashMap<QName, Serializable>(2);
				props.put(AMCModel.PROP_CONTENT_ID, id);				
				props.put(AMCModel.PROP_CONTENT_LINK, getUrlPrefix() + id);				
				nodeService.addAspect(newNode, AMCModel.ASPECT_AMC_IDENTIFIABLE, props);
			} else {
				// It has been created already with the aspect applied.
				nodeService.setProperty(newNode, AMCModel.PROP_CONTENT_ID, id);
				nodeService.setProperty(newNode, AMCModel.PROP_CONTENT_LINK, getUrlPrefix() + id);				
			}
			
			if (!nodeService.hasAspect(
						newNode, AMCModel.ASPECT_AMC_CLASSIFIABLE)) {
				// Assign classification
				Map<QName, Serializable> props = new HashMap<QName, Serializable>(0);
				nodeService.addAspect(newNode, AMCModel.ASPECT_AMC_CLASSIFIABLE, props);
			}
		}
	}
}
