package ford.james.motorola.services;

import java.io.IOException;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ford.james.motorola.exceptions.LockTimeoutException;

public interface FileService {

	Set<String> listFilenames() throws IOException;

	void saveFile(MultipartFile file) throws Exception;

	Resource getFile(String filename) throws Exception;

	void deleteFile(String filename) throws Exception;
}
