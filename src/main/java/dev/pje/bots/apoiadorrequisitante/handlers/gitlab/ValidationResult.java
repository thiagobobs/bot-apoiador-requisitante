package dev.pje.bots.apoiadorrequisitante.handlers.gitlab;

public class ValidationResult {

	private final boolean isValid;
	private final String errorMsg;

	public ValidationResult(boolean isValid, String errorMsg) {
		super();
		this.isValid = isValid;
		this.errorMsg = errorMsg;
	}

	public static ValidationResult valid() {
		return new ValidationResult(true, null);
	}

	public static ValidationResult invalid(String errorMsg) {
		return new ValidationResult(false, errorMsg);
	}

	public boolean isValid() {
		return isValid;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

}
