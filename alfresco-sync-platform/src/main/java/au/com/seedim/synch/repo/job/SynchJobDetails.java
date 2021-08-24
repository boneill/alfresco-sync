package au.com.seedim.synch.repo.job;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.schedule.AbstractScheduledLockedJob;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

public class SynchJobDetails extends AbstractScheduledLockedJob implements StatefulJob{
  
  
	private static Log logger = LogFactory.getLog(SynchJobDetails.class);
	
  @Override
  public void executeJob(JobExecutionContext context)
      throws JobExecutionException {
    // TODO Auto-generated method stub
    
	  
	  
    JobDataMap jobData = context.getJobDetail().getJobDataMap();
    
    String runAsUser = (String)jobData.get("runAsUserName");
    logger.debug("runAsUser:" + runAsUser);
    
    
    // Extract the Job executer to use
    Object executerObj = jobData.get("jobExecuter");
    if (executerObj == null || !(executerObj instanceof SynchJobExecutor)) {
        throw new AlfrescoRuntimeException(
                "ScheduledJob data must contain valid 'Executer' reference");
    }

    final SynchJobExecutor jobExecuter = (SynchJobExecutor) executerObj;

    // Execute the scheduled job as a specific user
    AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Object>() {
        public Object doWork() throws Exception {
            jobExecuter.execute();
            return null;
        }
    }, runAsUser);
  }


}
