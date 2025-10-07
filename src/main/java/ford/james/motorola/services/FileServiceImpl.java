package ford.james.motorola.services;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ford.james.motorola.FileStorageProperties;
import ford.james.motorola.exceptions.LockTimeoutException;
import ford.james.motorola.repositories.FileRepository;
import ford.james.motorola.functions.LockFunction;
import ford.james.motorola.utils.LockUtils;

@Service
public class FileServiceImpl implements FileService {

	private static final Logger LOGGER = LoggerFactory.getLogger(FileServiceImpl.class);

	private final Map<String, ReadWriteLock> filenameLock;

	private final FileRepository fileRepository;
	private final FileStorageProperties fileStorageProperties;

	public FileServiceImpl(FileRepository fileRepository, FileStorageProperties fileStorageProperties) {
		this.fileRepository = fileRepository;
		this.fileStorageProperties = fileStorageProperties;
		filenameLock = new ConcurrentHashMap<>();
	}

	@Override
	public Set<String> listFilenames() throws IOException {
		return fileRepository.listFilenames();
	}

	@Override
	public void saveFile(MultipartFile file) throws Exception {

		executeWithLock(file.getOriginalFilename(), true, () -> {

			if (fileRepository.fileExists(file.getOriginalFilename()) ) {
				LOGGER.error("Cannot save file [{}] as it already exists ", file.getOriginalFilename());
				throw new FileAlreadyExistsException("The file with name [" + file.getOriginalFilename() + "] already exists");
			}
			return fileRepository.saveFileToStorage(file);
		});
	}

	@Override
	public Resource getFile(String filename) throws Exception {
		LockFunction<Resource> lockFunction = () -> {

			if (!fileRepository.fileExists(filename) ) {
				LOGGER.error("Cannot delete file [{}] as it does not exist ", filename);
				throw new FileNotFoundException("The file with name [" + filename + "] does not exist");
			}

			return fileRepository.getFileFromStorage(filename);
		};

		return executeWithLock(filename, false, lockFunction);
	}

	@Override
	public void deleteFile(String filename) throws Exception {

		LockFunction<Boolean> lockFunction = () -> {

			if (!fileRepository.fileExists(filename) ) {
				LOGGER.error("Cannot delete file [{}] as it does not exist ", filename);
				throw new FileNotFoundException("The file with name [" + filename + "] does not exist");
			}
			return fileRepository.deleteFileFromStorage(filename);
		};

		executeWithLock(filename, true, lockFunction);
	}

	private <R> R executeWithLock(String filename, boolean isWrite, LockFunction<R> function) throws Exception {
		ReadWriteLock lock = filenameLock.get(filename);
		Lock specificLock;

		if (lock != null) {
			LOGGER.debug("Awaiting for lock on file [{}] to be released",  filename);

			specificLock = LockUtils.getLock(lock, isWrite);

			if (!specificLock.tryLock(fileStorageProperties.getRequestTimeout(), TimeUnit.SECONDS)) {
				LOGGER.warn("Lock for file [{}] was not released within the window of [{}] seconds", filename, fileStorageProperties.getRequestTimeout());

				String message = isWrite ? String.format("Cannot modify the file [%s] as it is in use", filename)
						: String.format("Cannot get the file [%s] as it is being modified", filename);

				throw new LockTimeoutException(message);
			}
			LOGGER.debug("Obtained lock while deleting file [{}]", filename);
		} else {
			LOGGER.debug("Creating a lock as none existed for file [{}]", filename);
			lock = new  ReentrantReadWriteLock(true);
			specificLock = LockUtils.getLock(lock, isWrite);
		}

		specificLock.lock();
		filenameLock.putIfAbsent(filename, lock);

		try {
			return function.apply();
		} finally {
			LOGGER.debug("Releasing lock on file [{}", filename);
			try {
				specificLock.unlock();
			} catch (IllegalMonitorStateException e) {
				LOGGER.warn("Error while releasing lock for [filename: {}, isWrite: {}]", filename, isWrite, e);
			}
			filenameLock.remove(filename);
		}
	}
}
