package au.org.amc.repo.webscripts.base;

import org.apache.commons.httpclient.HttpStatus;

public class BaseWebscriptException extends RuntimeException {

  private static final long serialVersionUID = 1L;

	protected int code = HttpStatus.SC_INTERNAL_SERVER_ERROR;
	
	public BaseWebscriptException() {

	}
	public BaseWebscriptException(String str) {
		super(str);
	}
	
	public BaseWebscriptException(String str, int httpStatus) {
		super(str);
		this.code = httpStatus;
	}
	
	public BaseWebscriptException(int httpStatus) {
		super();
		this.code = httpStatus;
		
	}
	public BaseWebscriptException(String str, Throwable e) {
		super(str, e);
	}
	public BaseWebscriptException(Throwable e) {
		super(e);
	}
	
	public void setHttpStatus(int code){
		this.code = code;
	}
	
	public int getHttpStatus(){
		return code;
	}
}
