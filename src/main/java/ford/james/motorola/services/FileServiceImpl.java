package ford.james.motorola.services;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ford.james.motorola.repositories.FileRepository;

@Service
public class FileServiceImpl implements FileService {

	private static final Logger LOGGER = Logger.getLogger(FileServiceImpl.class.getName());

	private final FileRepository fileRepository;

	public FileServiceImpl(FileRepository fileRepository) {
		this.fileRepository = fileRepository;
	}

	@Override
	public Set<String> listFilenames() throws IOException {
		return fileRepository.listFilenames();
	}

	@Override
	public void saveFile(MultipartFile file) throws IOException {
		LOGGER.info(String.format("Saving file [%s] to storage", file.getName()));
		fileRepository.saveFileToStorage(file);
		LOGGER.info(String.format("Successfully saved file [%s] to storage", file.getName()));
	}

	@Override
	public Resource getFile(String filename) throws IOException {
		return fileRepository.getFileFromStorage(filename);
	}

	@Override
	public void deleteFile(String filename) throws IOException {
		fileRepository.deleteFileFromStorage(filename);
	}
}
