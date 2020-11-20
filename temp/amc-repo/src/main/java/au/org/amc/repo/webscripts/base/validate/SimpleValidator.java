package au.org.amc.repo.webscripts.base.validate;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import au.org.amc.repo.webscripts.base.ValidationException;
import au.org.amc.repo.webscripts.base.Validator;

public class SimpleValidator implements Validator {
  private static final Logger logger = Logger.getLogger(SimpleValidator.class);
  private Pattern pattern;
  private Boolean mandatory;
  private String fieldName;
   
  public void validate(Map<String, String> map) throws ValidationException {
    boolean contain = map.containsKey(fieldName);
    
    // If a non mandatory field is not present then return that it validates okay
    if(!mandatory && !contain){
      return;
    }
    
    if (mandatory && !contain) {
      String msg = String.format("Parameter %s is not present", fieldName);
      throw new ValidationException(msg);
    }
    
    String value = map.get(fieldName);
    if (value == null) {
      String msg = String.format("Parameter %s is not present", fieldName);
      throw new ValidationException(msg);
    }
    if (pattern != null) {
      Matcher matcher = pattern.matcher(value);
      if (!matcher.matches()) {
        String msg = String.format("Parameter '%s' doesn't follow pattern", fieldName);
        throw new ValidationException(msg);
      }
    }
    return;
  }
  
  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }
  
  public void setRegex(String regex) {
    this.pattern = Pattern.compile(regex);
  }

  public void setMandatory(Boolean mandatory) {
    this.mandatory = mandatory;
  }
}
