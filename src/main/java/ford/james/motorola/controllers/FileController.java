package ford.james.motorola.controllers;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Set;
import java.util.logging.Logger;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ford.james.motorola.services.FileService;

@RestController
@RequestMapping("files")
public class FileController {

	private static final Logger logger = Logger.getLogger(FileController.class.getName());
	private static final String FILENAME_REGEX = "[^a-zA-Z0-9._]+";

	private final FileService fileService;

	public FileController(FileService fileService) {
		this.fileService = fileService;
	}

	//TODO: ControllerAdvice
	@GetMapping("download/{filename}")
	public ResponseEntity<Resource> getFile(@PathVariable String filename) throws IOException {

		validateFilename(filename);

		Resource resource = fileService.getFile(filename);

		return ResponseEntity.ok()
				.contentLength(resource.contentLength())
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(resource);
	}

	@GetMapping("list")
	public Set<String> listFiles() throws IOException {
		logger.info("Listing available files");
		return fileService.listFilenames();
	}

	@PostMapping("upload")
	public void uploadFile(MultipartFile file) throws IOException {
		validateFilename(file.getOriginalFilename());
		logger.info("Uploading file");
		fileService.saveFile(file);
	}

	@DeleteMapping("delete/{filename}")
	public void removeFile(@PathVariable String filename) throws IOException {
		validateFilename(filename);
		fileService.deleteFile(filename);
	}

	/**
	 * Checks that the filename contains only alphanumeric characters and if it does not then throws an
	 * {@code IllegalArgumentException}
	 *
	 * @param filename the name of the file to validate
	 */
	private void validateFilename(String filename) {
		String newFilename = filename.replaceAll(FILENAME_REGEX, "");
		if (!filename.equals(newFilename)) {
			throw new IllegalArgumentException(String.format("Filename of %s was not a valid name", filename));
		}
	}


}
