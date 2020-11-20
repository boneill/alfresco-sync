package au.org.amc.repo.webscripts.base.validate;

import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.util.ISO8601DateFormat;

import au.org.amc.repo.webscripts.base.ValidationException;
import au.org.amc.repo.webscripts.base.Validator;

public class DateValidator implements Validator {
  private String fieldName;
  
  public void validate(Map<String, String> map) throws ValidationException {
    boolean contain = map.containsKey(fieldName);
    if (!contain) {
      return ;
    }
    try {
      String str = map.get(fieldName);
      ISO8601DateFormat.parse(str);
      return;
    } catch (AlfrescoRuntimeException e){
      String msg = String.format("Parameter '%s' isn't a valid date", fieldName);
      throw new ValidationException(msg);
    }
  }
  
  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }
}
