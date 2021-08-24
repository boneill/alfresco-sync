package au.com.seedim.synch.repo.job;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.schedule.AbstractScheduledLockedJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

public class SynchJobDetails extends AbstractScheduledLockedJob implements StatefulJob{
  
  
  @Override
  public void executeJob(JobExecutionContext context)
      throws JobExecutionException {
    // TODO Auto-generated method stub
    
    
    JobDataMap jobData = context.getJobDetail().getJobDataMap();

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
    }, "admin");
  }


}
