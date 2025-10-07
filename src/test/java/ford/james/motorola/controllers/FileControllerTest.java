package ford.james.motorola.controllers;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import ford.james.motorola.exceptions.LockTimeoutException;
import ford.james.motorola.services.FileService;

public class FileControllerTest {

	@Mock
	private FileService fileService;
	@InjectMocks
	private FileController fileController;

	private AutoCloseable closeable;

	@BeforeEach
	void before() {
		closeable = MockitoAnnotations.openMocks(this);
	}

	@AfterEach
	void after() throws Exception {
		closeable.close();
	}

	@Test
	void testListFiles() throws IOException {

		when(fileService.listFilenames()).thenReturn(Set.of("File1", "Second File"));

		Set<String> filenames = fileController.listFiles();

		verify(fileService).listFilenames();
		assertEquals(2, filenames.size());
	}

	@Test
	void testRemoveFileSuccess() throws Exception {
		fileController.removeFile("AcceptedName1.csv");
		verify(fileService).deleteFile("AcceptedName1.csv");
	}

	@Test
	void testRemoveFileInvalidName() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> fileController.removeFile("InvalidN&M£.csv"));
		verify(fileService, never()).deleteFile(any());
	}

	@Test
	void testUploadFileSuccess() throws Exception {

		MultipartFile file = new MockMultipartFile("val1dFil3nam3", "val1dFil3nam3", null, "file".getBytes());

		fileController.uploadFile(file);
		verify(fileService).saveFile(file);
	}

	@Test
	void testUploadFileInvalidName() throws Exception {

		MultipartFile file = new MockMultipartFile("inval1dFil3nam3.t/t", "inval1dFil3nam3.t/t", null, "file".getBytes());

		assertThrows(IllegalArgumentException.class, () -> fileController.uploadFile(file));
		verify(fileService, never()).saveFile(any());
	}

	@Test
	void testGetFileSuccess() throws Exception {

		Resource resource = new ByteArrayResource("file".getBytes());
		when(fileService.getFile(any())).thenReturn(resource);

		ResponseEntity<Resource> response = fileController.getFile("testFile.jpeg");

		assertAll(() -> {
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
			assertEquals(4, response.getHeaders().getContentLength());
			assertEquals(resource, response.getBody());
		});

		verify(fileService).getFile("testFile.jpeg");
	}

	@Test
	void testGetFileInvalidName() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> fileController.getFile("InvalidN&M£.csv"));
		verify(fileService, never()).getFile(any());
	}
}
