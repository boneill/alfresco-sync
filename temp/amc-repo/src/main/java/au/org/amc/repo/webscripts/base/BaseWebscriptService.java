package au.org.amc.repo.webscripts.base;

import java.io.OutputStream;
import java.util.Map;

public interface BaseWebscriptService {
  void init();
  void run(OutputStream outputStream, Map<String, String> parameters) throws BaseWebscriptException;
  void before(String originalUser, Map<String, String> parameters) throws BaseWebscriptException, ValidationException;
  void after(String originalUser, Map<String, String> parameters) throws BaseWebscriptException, ValidationException;
}
