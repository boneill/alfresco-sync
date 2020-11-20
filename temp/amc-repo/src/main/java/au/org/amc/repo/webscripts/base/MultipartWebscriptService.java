package au.org.amc.repo.webscripts.base;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public interface MultipartWebscriptService {
  void run(OutputStream outputStream, String filename, InputStream content, 
      Map<String, String> properties) throws BaseWebscriptException, ValidationException;
}
