package au.org.amc.repo.webscripts.base;

public class ValidationException extends RuntimeException {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  public ValidationException() {
    
  }
  public ValidationException(String str) {
    super(str);
  }
  public ValidationException(String str, Throwable e) {
    super(str, e);
  }
  public ValidationException(Throwable e) {
    super(e);
  }
}
