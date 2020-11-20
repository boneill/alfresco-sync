package au.org.amc.repo.webscripts.base;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;


public abstract class StandardWebscript extends AbstractWebScript {
  private Logger logger = Logger.getLogger(StandardWebscript.class); 
  protected AuthenticationService authenticationService;
  private List<Validator> validatorList;
  private PersonService personService;
  private Set<String> groupAllowedRunAsList;
  private Set<String> groupAllowedExecuteList;
  private AuthorityService authorityService;
  
  public abstract void exec(WebScriptRequest request, final WebScriptResponse response) throws BaseWebscriptException, ValidationException;
  
  public void before(String originalUser, Map<String, String> parameters)
      throws BaseWebscriptException, ValidationException {
  }

  public void after(String originalUser, Map<String, String> parameters)
      throws BaseWebscriptException, ValidationException {
  }
  
  public interface Task{
    void run() throws Exception;
  }
  
  private boolean isAllowed(final String user, Set<String> groupAllowed) {
    if (groupAllowed == null || groupAllowed.size() == 0) {
      return true;
    }
    
    Set<String> groupsUser = AuthenticationUtil.runAs(new RunAsWork<Set<String>>(){
      @Override
      public Set<String> doWork() throws Exception {
        return authorityService.getAuthoritiesForUser(user);
      }
    }, AuthenticationUtil.getAdminUserName());
    return !Collections.disjoint(groupAllowed, groupsUser);
  }
  
  public void run(final Task task, Map<String, String> map) throws ValidationException {
    String currentUserName = authenticationService.getCurrentUserName();
  
    this.before(currentUserName, map);
    
    if (!isAllowed(currentUserName, groupAllowedExecuteList)) {
      String msg = String.format("user %s is not allowed to run the webscript", currentUserName);
      throw new BaseWebscriptException(msg, HttpStatus.SC_UNAUTHORIZED);
    }
    
    if(validatorList != null){
      for (Validator validator : validatorList) {
        validator.validate(map);
      }
    }
    
    String userId = currentUserName;
    
    if (map.containsKey("user")) {
      userId = map.get("user");
      if (!personService.personExists(userId)) {
        String msg = String.format("user %s doesn't exist", userId);
        throw new ValidationException(msg);
      }
      
      if (groupAllowedRunAsList == null) {
        String msg = "this webscript doesn't allow 'run as' another user";
        throw new ValidationException(msg);
      }
      
      if (!isAllowed(userId, groupAllowedRunAsList)) {
        String msg = String.format("user %s doesn't belong to a allowed 'runas' group", userId);
        throw new BaseWebscriptException(msg, HttpStatus.SC_UNAUTHORIZED);
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("User ID Set to " + userId);
    }
    
    runAs(userId, task);
    
    this.after(currentUserName, map);
  }
  
  private void runAs(final String userId, final Task task) {
    try {
      AuthenticationUtil.runAs(new RunAsWork<Void>() {
        @Override
        public Void doWork() throws Exception {
          AuthenticationUtil.setFullyAuthenticatedUser(userId);
          task.run();
          return null;
        }
      }, userId);
    } catch (BaseWebscriptException e) {
      throw e;
    } catch (ValidationException e) {
      throw e;
    } catch (Exception e) {
      throw new BaseWebscriptException(e.getMessage(), e);
    }
  }
  
  @Override 
  final public void execute(WebScriptRequest request, final WebScriptResponse response)
      throws IOException {
    final OutputStream outputStream = response.getOutputStream();
    try {
      response.setStatus(HttpStatus.SC_OK);
      response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
      exec(request, response);
      
    } catch (ValidationException e) {
      response.setStatus(HttpStatus.SC_BAD_REQUEST);
      errorOutput(outputStream, HttpStatus.SC_BAD_REQUEST, e.getMessage());
    } catch (BaseWebscriptException e) {
      response.setStatus(e.getHttpStatus());
      errorOutput(outputStream, e.getHttpStatus(), e.getMessage());
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
      errorOutput(outputStream, HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
    
  }
  
  protected Map<String,String> getParameters(WebScriptRequest request) throws ValidationException {
    final Map<String,String> map = new HashMap<String, String>();
    String[] parameterNames = request.getParameterNames();
    for (String key: parameterNames) {
      String value = request.getParameter(key);
      map.put(key, value);
    }
    return map;
  }
  
  protected void errorOutput(OutputStream outputStream, int httpStatus, String msg) throws IOException {
    JsonFactory jasonFactory = new JsonFactory();
    JsonGenerator jsonGenerator = jasonFactory.createJsonGenerator(outputStream, JsonEncoding.UTF8);
    jsonGenerator.writeStartObject();
    jsonGenerator.writeNumberField("status", httpStatus);
    jsonGenerator.writeStringField("msg", msg);
    jsonGenerator.writeEndObject();
    jsonGenerator.close();
  }

  public void setAuthenticationService(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }
  
  public void setPersonService(PersonService personService) {
    this.personService = personService;
  }
  
  public void setAuthorityService(AuthorityService authorityService) {
    this.authorityService = authorityService;
  }
  
  public void setValidatorList(List<Validator> validatorList) {
    this.validatorList = validatorList;
  }

  public void setGroupAllowedRunAsList(Set<String> groupAllowedRunAsList) {
    this.groupAllowedRunAsList = groupAllowedRunAsList;
  }

  public void setGroupAllowedExecuteList(Set<String> groupAllowedExecuteList) {
    this.groupAllowedExecuteList = groupAllowedExecuteList;
  }
}
