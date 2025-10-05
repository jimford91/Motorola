package ford.james.motorola.services;

import java.io.IOException;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

	Set<String> listFilenames() throws IOException;

	void saveFile(MultipartFile file) throws IOException;

	Resource getFile(String filename) throws IOException;

	void deleteFile(String filename) throws IOException;
}
