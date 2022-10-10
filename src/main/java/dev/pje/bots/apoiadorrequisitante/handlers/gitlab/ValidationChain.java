package dev.pje.bots.apoiadorrequisitante.handlers.gitlab;

public abstract class ValidationChain<T> {

	private ValidationChain<T> next;

	public ValidationChain<T> next(ValidationChain<T> next) {
		this.next = next;
		return this;
	}

	protected ValidationResult verifyNext(T object) {
		if (next == null) {
			return ValidationResult.valid();
		}

		return next.verify(object);
	}

	public abstract ValidationResult verify(T object);
}
