package ford.james.motorola.controllers;


import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import ford.james.motorola.exceptions.LockTimeoutException;

/**
 * Exception handler for all Controllers to return correct responses and status codes to the caller.
 */
@RestControllerAdvice
public class ControllerExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ControllerExceptionHandler.class);

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<String> handleIllegalArgumentException(WebRequest webRequest, IllegalArgumentException e) {
		return generateResponseAndLog(HttpStatus.BAD_REQUEST, e, webRequest);
	}

	@ExceptionHandler(NoSuchFileException.class)
	public ResponseEntity<String> handleNoSuchFileException(WebRequest webRequest, NoSuchFileException e) {
		return generateResponseAndLog(HttpStatus.NOT_FOUND, e, webRequest);
	}

	@ExceptionHandler(LockTimeoutException.class)
	public ResponseEntity<String> handleLockTimeoutException(WebRequest webRequest, LockTimeoutException e) {
		return generateResponseAndLog(HttpStatus.REQUEST_TIMEOUT, e, webRequest);
	}

	@ExceptionHandler(FileAlreadyExistsException.class)
	public ResponseEntity<String> handleFileAlreadyExistsException(WebRequest webRequest, FileAlreadyExistsException e) {
		return generateResponseAndLog(HttpStatus.CONFLICT, e, webRequest);
	}

	@ExceptionHandler(Throwable.class)
	public ResponseEntity<String> handleExceptions(WebRequest webRequest, Exception e) {
		return generateResponseAndLog(HttpStatus.INTERNAL_SERVER_ERROR, e, webRequest);
	}

	private ResponseEntity<String> generateResponseAndLog(HttpStatus status, Exception e, WebRequest webRequest) {
		String message = String.format("Error handling request [%s]. Error message: [%s]",
				webRequest.getDescription(false), e.getMessage());

		LOGGER.error(message, e);

		return ResponseEntity.status(status)
				.contentType(MediaType.TEXT_PLAIN)
				.body(message);
	}

}
