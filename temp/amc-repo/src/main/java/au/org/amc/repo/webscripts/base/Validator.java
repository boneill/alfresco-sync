package au.org.amc.repo.webscripts.base;

import java.util.Map;

public interface Validator {
  void validate(Map<String, String> map) throws ValidationException;
}
