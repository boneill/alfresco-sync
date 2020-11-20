package au.org.amc.repo.webscripts.base.validate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.security.AuthorityService;
import org.apache.log4j.Logger;

import au.org.amc.repo.webscripts.base.ValidationException;
import au.org.amc.repo.webscripts.base.Validator;

public class GroupValidator implements Validator {
  private static final Logger logger = Logger.getLogger(GroupValidator.class);
  private List<String> groupList;
  protected AuthorityService authorityService;
  String errorMsg;

  
  @Override
  public void validate(Map<String, String> map) throws ValidationException {

    final String userId = map.get("user");
     
    if(userId == null){
      if(logger.isDebugEnabled()){logger.debug("User not passed");}
      return;
    }
    if (userId != null){
      Set<String> authorities = authorityService.getAuthoritiesForUser(userId);
      if (logger.isDebugEnabled()) {
          String msg = String.format("Authorities for user %s", authorities.toString());
          logger.debug(msg);
      }
      for (String group : groupList){
        if((authorities.contains(group))){
          if(logger.isDebugEnabled()){logger.debug(userId +" in Group " + group + "ALLOWED");}
          return;
        }
      }
    } 
    errorMsg = userId + " not part of allowed groups to execute webscript";
    logger.debug(errorMsg);
    throw new ValidationException(errorMsg);
  }
  
  public List<String> getGroupList() {
    return groupList;
  }
  
  public void setGroupList(List<String> groupList) {
    this.groupList = groupList;
  }
  
  public AuthorityService getAuthorityService() {
    return authorityService;
  }
  
  public void setAuthorityService(AuthorityService authorityService) {
    this.authorityService = authorityService;
  }
}