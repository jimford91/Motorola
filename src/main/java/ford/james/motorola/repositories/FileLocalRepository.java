package ford.james.motorola.repositories;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import ford.james.motorola.FileStorageProperties;

@Repository
public class FileLocalRepository implements FileRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(FileLocalRepository.class.getName());

	private final FileStorageProperties fileStorageProperties;

	public FileLocalRepository(FileStorageProperties fileStorageProperties) {
		this.fileStorageProperties = fileStorageProperties;
	}

	@Override
	public Set<String> listFilenames() throws IOException {
		try (Stream<Path> stream = Files.list(Paths.get(fileStorageProperties.getBaseLocation()))) {
			return stream
					.filter(file -> !Files.isDirectory(file))
					.map(Path::getFileName)
					.map(Path::toString)
					.collect(Collectors.toSet());
		} catch (IOException e) {
			LOGGER.error("Unexpected error while reading filenames in {}", fileStorageProperties.getBaseLocation(), e);
			throw e;
		}
	}

	@Override
	public boolean saveFileToStorage(MultipartFile file) throws IOException {
		Path path = buildFilePath(file);
		try {
			file.transferTo(path);
			return true;
		} catch (IOException ex) {
			LOGGER.error("Unable to save file in location [{}]", path, ex);
			throw ex;
		}
	}

	@Override
	public boolean deleteFileFromStorage(String filename) throws IOException {

		Path filePath = buildFilePath(filename);

		try {
			Files.delete(filePath);
			return true;
		} catch (IOException e) {
			LOGGER.error("Unable to delete file in location [{}]", filePath, e);
			throw e;
		}
	}

	@Override
	public ByteArrayResource getFileFromStorage(String filename) throws IOException {

		Path path = buildFilePath(filename);

		return new ByteArrayResource(Files.readAllBytes(path));
	}

	@Override
	public boolean fileExists(String filename) {
		return Files.exists(buildFilePath(filename));
	}

	private Path buildFilePath(MultipartFile file) {
		return buildFilePath(file.getOriginalFilename());
	}

	private Path buildFilePath(String filename) {
		return Path.of(fileStorageProperties.getBaseLocation() + "/" + filename);
	}
}
