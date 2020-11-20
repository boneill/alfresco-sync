package au.org.amc.repo.webscripts.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.extensions.webscripts.WrappingWebScriptRequest;
import org.springframework.extensions.webscripts.servlet.FormData.FormField;
import org.springframework.extensions.webscripts.servlet.WebScriptServletRequest;

public class MultipartWebscript extends StandardWebscript{
  private Logger logger = Logger.getLogger(BaseWebscript.class); 

  private MultipartWebscriptService multipartWebscriptService;
  
  private WebScriptServletRequest makeServletRequest(WebScriptRequest request) throws ValidationException {
    WebScriptServletRequest webScriptServletRequest = null;
    WebScriptRequest current = request;
    do {
        if (current instanceof WebScriptServletRequest) {
            webScriptServletRequest = (WebScriptServletRequest) current;
            current = null;
        } else if (current instanceof WrappingWebScriptRequest) {
            current = ((WrappingWebScriptRequest) request).getNext();
        } else {
            current = null;
        }
    } while (current != null);
    if (webScriptServletRequest == null) {
        logger.debug("no webScriptServletRequest");
        throw new ValidationException("bad request");
    }
    return webScriptServletRequest;
  }
  
  @Override
  public void exec(WebScriptRequest request, WebScriptResponse response) throws ValidationException {
    WebScriptServletRequest webScriptServletRequest = makeServletRequest(request);
    
    String contentType = webScriptServletRequest.getContentType();
    
    if (!WebScriptServletRequest.MULTIPART_FORM_DATA.equals(contentType)){ 
      throw new ValidationException("no multipart request"); 
    }
    
    FormField formField = webScriptServletRequest.getFileField("file");
    if(formField== null || !formField.getIsFile()) {
      throw new ValidationException("field 'file' has to be a file"); 
    }
    
    final String filename = formField.getFilename();
    final InputStream inputStream = formField.getInputStream();
    if (filename == null) {
      throw new ValidationException("field 'file' is mandatory"); 
    }
    
    try {
      final OutputStream outputStream = response.getOutputStream();
      final Map<String, String> map = getParameters(request);
      
      if (logger.isDebugEnabled()) {
        StringBuilder builder = new StringBuilder("values request multipartWebscriptService: ");
        String msg = String.format("filename : %s},", filename);
        builder.append(msg);
        for (String key : map.keySet()) { 
          String value = map.get(key);
          msg = String.format("{%s:%s},", key,value);
          builder.append(msg);
        }
        logger.debug(builder.toString());
      }
      this.run(new Task(){
        @Override
        public void run() throws Exception {
          multipartWebscriptService.run(outputStream, filename, inputStream, map);
        }}, map);
    } catch (IOException e) {
      throw new BaseWebscriptException(e.getMessage(), e);
    }
  }
  
  public void setMultipartWebscriptService(MultipartWebscriptService multipartWebscriptService) {
    this.multipartWebscriptService = multipartWebscriptService;
  }

}
