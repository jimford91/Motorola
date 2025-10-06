package ford.james.motorola.controllers;

import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(FileController.class.getName());
	private static final String FILENAME_REGEX = "[^a-zA-Z0-9._]+";

	private final FileService fileService;

	public FileController(FileService fileService) {
		this.fileService = fileService;
	}

	@GetMapping("download/{filename}")
	public ResponseEntity<Resource> getFile(@PathVariable String filename) throws IOException {

		LOGGER.debug("Downloading file [{}}]", filename);

		validateFilename(filename);

		Resource resource = fileService.getFile(filename);

		LOGGER.debug("Successfully downloaded file [{}}]", filename);

		return ResponseEntity.ok()
				.contentLength(resource.contentLength())
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(resource);
	}

	@GetMapping("list")
	public Set<String> listFiles() throws IOException {
		LOGGER.debug("Listing available files");
		return fileService.listFilenames();
	}

	@PostMapping("upload")
	public void uploadFile(MultipartFile file) throws IOException {
		LOGGER.debug("Uploading file [{}}]", file.getOriginalFilename());
		validateFilename(file.getOriginalFilename());
		fileService.saveFile(file);
		LOGGER.debug("Successfully uploaded file [{}}]", file.getOriginalFilename());
	}

	//TODO: TEst with no name provided
	@DeleteMapping("delete/{filename}")
	public void removeFile(@PathVariable String filename) throws IOException {
		LOGGER.debug("Deleting file [{}}]", filename);
		validateFilename(filename);
		fileService.deleteFile(filename);
		LOGGER.debug("Successfully deleted file [{}}]", filename);
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
