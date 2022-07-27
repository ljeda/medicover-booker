package ljeda.medicover.base;

public class BotException extends Exception {

	private static final long serialVersionUID = -4776058440697594130L;
	private ErrorCodes errorCode;
	
	public enum ErrorCodes {
		OK,
		DOM_CHANGED,
		TIMEOUT,
		UNEXPECTED_SITUATION
	}
	
	public BotException(String message, ErrorCodes errorCode) {
		super(message);
		this.errorCode = errorCode;
	}
	
	public ErrorCodes getErrorCode() {
		return errorCode;
	}
}
