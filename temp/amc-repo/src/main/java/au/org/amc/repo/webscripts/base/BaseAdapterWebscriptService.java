package au.org.amc.repo.webscripts.base;

import java.util.Map;

public abstract class BaseAdapterWebscriptService implements BaseWebscriptService {

  @Override
  public void init() {

  }

  @Override
  public void before(String originalUser, Map<String, String> parameters)
      throws BaseWebscriptException, ValidationException {
  }

  @Override
  public void after(String originalUser, Map<String, String> parameters)
      throws BaseWebscriptException, ValidationException {
  }

}
