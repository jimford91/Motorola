package ford.james.motorola.repositories;

import java.io.IOException;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

public interface FileRepository {

	Set<String> listFilenames() throws IOException;

	void saveFileToStorage(MultipartFile file) throws IOException;

	void deleteFileFromStorage(String filename) throws IOException;

	Resource getFileFromStorage(String filename) throws IOException;
}
