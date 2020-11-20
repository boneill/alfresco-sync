package au.org.amc.model;

import org.alfresco.service.namespace.QName;

/**
 * Constants for use with the AMC model
 *
 */
public interface AMCModel {
	static final String AMC_MODEL_1_0_URI = "http://www.amc.org.au/model/1.0";
	
	static final QName TYPE_AMC_SYSTEM_FOLDER = 
		QName.createQName(AMC_MODEL_1_0_URI, "systemFolder");
	
	static final QName TYPE_AMC_FILELINK = 
		QName.createQName(AMC_MODEL_1_0_URI, "filelink");
	
	static final QName ASPECT_AMC_CANDIDATECASEFOLDER =
		QName.createQName(AMC_MODEL_1_0_URI, "candidateCase");

	static final QName ASPECT_UPDATEPROPERTIESAUDITABLE = 
		QName.createQName(AMC_MODEL_1_0_URI, "updatePropertiesAuditable");
	
	static final QName ASPECT_AMC_CANDIDATESUBFOLDER =
		QName.createQName(AMC_MODEL_1_0_URI, "candidateSubFolder");
	
	static final QName ASPECT_AMC_CANDIDATEDOCUMENT =
		QName.createQName(AMC_MODEL_1_0_URI, "candidateDocument");
	
	static final QName ASPECT_AMC_IDENTIFIABLE =
		QName.createQName(AMC_MODEL_1_0_URI, "identifiable");
	
	static final QName ASPECT_AMC_CLASSIFIABLE =
		QName.createQName(AMC_MODEL_1_0_URI, "classifiable");
	
	static final QName ASPECT_AMC_RELEASED_MATERIAL =
		QName.createQName(AMC_MODEL_1_0_URI, "releasedMaterial");
	
	static final QName ASPECT_AMC_VERSION_SETTINGS =
			QName.createQName(AMC_MODEL_1_0_URI, "vesionSettings");
	
	static final QName ASPECT_AMC_REDIRECT_SETTINGS =
			QName.createQName(AMC_MODEL_1_0_URI, "redirectSettings");
	
	static final QName PROP_CASE_STATUS =
		QName.createQName(AMC_MODEL_1_0_URI, "status");

	static final QName PROP_CASE_ID =
		QName.createQName(AMC_MODEL_1_0_URI, "caseId");
	
	static final QName PROP_CONTENT_ID =
		QName.createQName(AMC_MODEL_1_0_URI, "amcContentID");
	
	static final QName PROP_CONTENT_LINK =
		QName.createQName(AMC_MODEL_1_0_URI, "amcContentLink");
	
	static final QName PROP_SECURITY_CLASSIFICATION =
		QName.createQName(AMC_MODEL_1_0_URI, "securityClassification");
	
	static final QName PROP_SECURITY_COMMENT =
		QName.createQName(AMC_MODEL_1_0_URI, "securityComment");

	static final QName PROP_RELEASE_DATE =
		QName.createQName(AMC_MODEL_1_0_URI, "releaseDate");

	static final QName PROP_RELEASE_VERSION =
		QName.createQName(AMC_MODEL_1_0_URI, "releaseVersion");

	static final QName PROP_RELEASE_PARTY =
		QName.createQName(AMC_MODEL_1_0_URI, "releaseParty");

	static final QName PROP_RELEASE_APPROVER =
		QName.createQName(AMC_MODEL_1_0_URI, "releaseApprover");

	static final QName PROP_RELEASE_NOTES =
		QName.createQName(AMC_MODEL_1_0_URI, "releaseNotes");

	static final QName PROP_CAND_DOC_CLASSIFICATION =
		QName.createQName(AMC_MODEL_1_0_URI, "candidateDocumentClassification");
	
	static final QName PROP_ASSIGNEE = 
		QName.createQName(AMC_MODEL_1_0_URI, "assignee");

	static final QName PROP_SYSTEM_FOLDER_INACTIVE = 
		QName.createQName(AMC_MODEL_1_0_URI, "systemFolderInactive");

	static final QName PROP_IS_SYSTEM_FOLDER = 
		QName.createQName(AMC_MODEL_1_0_URI, "isSystemFolder");

	static final QName PROP_CAN_CHILDREN_BE_ADDED = 
		QName.createQName(AMC_MODEL_1_0_URI, "canChildrenBeAdded");
	
	static final QName PROP_DEFALUT_TO_MAJOR_VERSION = 
			QName.createQName(AMC_MODEL_1_0_URI, "defaultToMajorVersion");
	
	static final QName PROP_REDIRECT_TO_DOCUMENT_LIBRARY = 
			QName.createQName(AMC_MODEL_1_0_URI, "redirectToDocumentLibrary");
}
