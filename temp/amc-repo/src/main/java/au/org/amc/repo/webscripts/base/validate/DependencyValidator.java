package au.org.amc.repo.webscripts.base.validate;

import java.util.Map;

import au.org.amc.repo.webscripts.base.ValidationException;
import au.org.amc.repo.webscripts.base.Validator;

public class DependencyValidator implements Validator {
  private String fieldName;
  private String fieldNameDependency;
  
  public void validate(Map<String, String> map) throws ValidationException {
    boolean contain = map.containsKey(fieldName);
    boolean containDependency = map.containsKey(fieldNameDependency);
    if (!contain) {
      return;
    }
    
    if (containDependency) {
      String dependencyValue = map.get(fieldNameDependency);
      if (dependencyValue != null) {
        return;
      }
    }
    String msg = String.format("Parameter '%s' depends on parameter '%s' and it is not present", fieldName, fieldNameDependency);
    throw new ValidationException(msg);
  }
  
  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }
  
  public void setFieldNameDependency(String fieldNameDependency) {
    this.fieldNameDependency = fieldNameDependency;
  }
}
