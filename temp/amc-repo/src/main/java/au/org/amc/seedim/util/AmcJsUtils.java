package au.org.amc.seedim.util;

import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.apache.log4j.Logger;
import au.org.amc.repo.services.AllServices;

public class AmcJsUtils extends BaseScopableProcessorExtension{
  private static final Logger logger = Logger.getLogger(AmcJsUtils.class);
  private BehaviourFilter policyBehaviourFilter;
  


  public void disableAllBehaviours() {
    RunAsWork<Void> work = new RunAsWork<Void>() {
      public Void doWork() throws Exception {
        policyBehaviourFilter.disableBehaviour();
        return null;
      }
    };
    String user = AuthenticationUtil.getSystemUserName();
    AuthenticationUtil.runAs(work, user);
  }
  
  public void enableAllBehaviours() {
    RunAsWork<Void> work = new RunAsWork<Void>() {
      public Void doWork() throws Exception {
        policyBehaviourFilter.enableBehaviour();
        return null;
      }
    };
    String user = AuthenticationUtil.getSystemUserName();
    AuthenticationUtil.runAs(work, user);
  }
  public void setAllServices(AllServices allServices) {
    this.policyBehaviourFilter = allServices.getPolicyBehaviourFilter();
  }
  public BehaviourFilter getPolicyBehaviourFilter() {
    return policyBehaviourFilter;
  }

  public void setPolicyBehaviourFilter(BehaviourFilter policyBehaviourFilter) {
    this.policyBehaviourFilter = policyBehaviourFilter;
  }
}
