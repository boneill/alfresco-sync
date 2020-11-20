package au.org.amc.repo.webscripts.base;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class BaseWebscript extends StandardWebscript {
  private Logger logger = Logger.getLogger(BaseWebscript.class);
  private BaseWebscriptService baseWebscriptService;
  
  public void run(final OutputStream outputStream, final Map<String, String> map) throws ValidationException, BaseWebscriptException {
    if (logger.isDebugEnabled()) {
      StringBuilder builder = new StringBuilder("values request BaseWebscript: ");
      for (String key : map.keySet()) {
        String value = map.get(key);
        String msg = String.format("{%s:%s},", key,value);
        builder.append(msg);
      }
      logger.debug(builder.toString());
    }
    this.run(new Task(){
      @Override
      public void run() throws Exception {
        baseWebscriptService.run(outputStream, map);
      }}, map);
  }

  @Override
  public void before(String originalUser, Map<String, String> parameters)
      throws BaseWebscriptException, ValidationException {
    baseWebscriptService.before(originalUser, parameters);
  }

  @Override
  public void after(String originalUser, Map<String, String> parameters)
      throws BaseWebscriptException, ValidationException {
    baseWebscriptService.after(originalUser, parameters);
  }
  
  @Override
  public void exec(WebScriptRequest request, WebScriptResponse response) throws ValidationException, BaseWebscriptException {
    try {
      OutputStream outputStream = response.getOutputStream();
      Map<String, String> map = getParameters(request);
      run(outputStream, map);
    }
    catch (IOException e) {
      throw new BaseWebscriptException(e.getMessage(), e);
    }
  }

  public void setBaseWebscriptService(BaseWebscriptService baseWebscriptService) {
    this.baseWebscriptService = baseWebscriptService;
  }
}
