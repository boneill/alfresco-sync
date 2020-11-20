package au.org.amc.seedim.action;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class DeleteOldMinorVersions extends ActionExecuterAbstractBase {
	
	private static Log logger = LogFactory.getLog(DeleteOldMinorVersions.class);
	private String versionAge;
	
	public final static String NAME = "delete-old-minor-versions";
	protected VersionService versionService;
	protected NodeService nodeService;

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		// TODO Auto-generated method stub
		if (logger.isDebugEnabled()) logger.debug("Entered DeleteOldMinorVersions executeImpl method");
		
		Version currentVersion = this.versionService.getCurrentVersion(actionedUponNodeRef);
		if (logger.isDebugEnabled()) logger.debug("Current Version label: " + currentVersion.getVersionLabel());
		
		VersionHistory versionHistory = this.versionService.getVersionHistory(actionedUponNodeRef);
		if(versionHistory != null){
			Version rootVersion = versionHistory.getRootVersion();
			
			Iterator<Version> itr = (versionHistory.getAllVersions()).iterator();
			while(itr.hasNext()) {
		         Version version = itr.next();
		         //if (logger.isDebugEnabled()) logger.debug("Versions: " + version.getVersionLabel() + "-" + version.getFrozenModifiedDate() + "-" + version.getVersionType());
		         
		         String verType = "unknown";
		         if(version.getVersionType() != null){
		        	 verType = version.getVersionType().toString();
		         }else{
		     		
		        	 if (logger.isDebugEnabled()) logger.debug("This has a version type of " + version.getVersionType());
		        	 String verLabel = version.getVersionLabel();
		        	 String[] verLabelParts = verLabel.split("\\.");
		        	 //String verLabelPart1 = verLabelParts[0];
		        	 String verLabelPart2 = verLabelParts[1];
		        	 int intVerlabelPart2 = Integer.parseInt(verLabelPart2);
		        	 if (intVerlabelPart2 > 0){
		        		 verType = "minor";
		        	 }
		        	 if (logger.isDebugEnabled()) logger.debug("The version label is " + verLabel + " and version type has been set to: " + verType);
		         }
		         
		         if ((verType.equalsIgnoreCase("minor"))) {
		        	 //Check if version being checked is not the root and current version
		        	if(!(version.getVersionLabel().equalsIgnoreCase(currentVersion.getVersionLabel())) && !(version.getVersionLabel().equalsIgnoreCase(rootVersion.getVersionLabel()))){
		        		if (logger.isDebugEnabled()) logger.debug("Minor Versions: " + version.getVersionLabel() + "<-->" + version.getFrozenModifiedDate());
		         		if(processDate(version.getFrozenModifiedDate())){
		         			if (logger.isDebugEnabled()) logger.debug("This Minor Version has been deleted " + version.getVersionLabel() + " -> " + version.getFrozenModifiedDate());	
		         			versionService.deleteVersion(actionedUponNodeRef, version);
		         		
		         		}
		       		}
		         }
		         
		      }
		}
		if (logger.isDebugEnabled()) logger.debug("Exiting DeleteOldMinorVersions executeImpl method");
	}

	@Override
	//Dummy parameter had to be added or else the action is not called by the scheduled job
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		paramList.add(new ParameterDefinitionImpl("a-parameter", DataTypeDefinition.TEXT, false, getParamDisplayLabel("a-parameter")));		
	}

	public VersionService getVersionService() {
		return versionService;
	}

	public void setVersionService(VersionService versionService) {
		this.versionService = versionService;
	}

	public NodeService getNodeService() {
		return nodeService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	
	 private boolean processDate(Date d){
		 Calendar calendarNow = Calendar.getInstance();
		 long timeNowMilli = calendarNow.getTimeInMillis();
		 //long day = timeNowMilli/(24 * 60 * 60 * 1000);
		 Calendar calendarVer = new GregorianCalendar();
		 calendarVer.setTime(d);
		 long timeVerMilli = calendarVer.getTimeInMillis();
		 //long verDay = timeVerMilli/(24 * 60 * 60 * 1000);
		 //if (logger.isDebugEnabled()) logger.debug("versionDay -> " + verDay + " Today -> " + day);
		 long verAge =  (timeNowMilli -timeVerMilli)/(24 * 60 * 60 * 1000);
		 if (logger.isDebugEnabled()) logger.debug("Actual verAge is " + verAge);
		 
		 if(versionAge != null){
			 if (logger.isDebugEnabled()) logger.debug("versionAge set to -> " + versionAge);
			 int vAge = Integer.parseInt(versionAge);
			 
			 if(verAge > vAge){
				 return true; 
			 }else{
				 if (logger.isDebugEnabled()) logger.debug("versionAge not reached");
				 return false;
			 } 
		 }else{
			 if (logger.isDebugEnabled()) logger.debug("versionAge not Set");
			 return false;
		 }
	 }

	public String getVersionAge() {
		return versionAge;
	}

	public void setVersionAge(String versionAge) {
		this.versionAge = versionAge;
	}
}
