package ford.james.motorola.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import ford.james.motorola.FileStorageProperties;
import ford.james.motorola.exceptions.LockTimeoutException;
import ford.james.motorola.repositories.FileRepository;
import ford.james.motorola.services.FileServiceImpl;

public class FileServiceImplTest {

	private static final long REQUEST_TIMEOUT = 1L;

	@Mock
	private FileRepository fileRepository;
	@Mock
	private FileStorageProperties  fileStorageProperties;
	@Mock
	private Resource resource;
	@InjectMocks
	private FileServiceImpl fileServiceImpl;

	private final ExecutorService executorService = Executors.newFixedThreadPool(2);
	private AutoCloseable closeable;

	@BeforeEach
	void before() {
		closeable = MockitoAnnotations.openMocks(this);
		when(fileStorageProperties.getRequestTimeout()).thenReturn(REQUEST_TIMEOUT);
	}

	@AfterEach
	void after() throws Exception {
		closeable.close();
	}

	@Test
	void testListFilenames() throws IOException {
		fileServiceImpl.listFilenames();
		verify(fileRepository).listFilenames();
	}

	@Test
	void testGetFileSuccess() throws Exception {
		when(fileRepository.fileExists("filename")).thenReturn(true);
		when(fileRepository.getFileFromStorage("filename")).thenReturn(resource);

		assertEquals(resource, fileServiceImpl.getFile("filename"));

		verify(fileRepository).fileExists("filename");
		verify(fileRepository).getFileFromStorage("filename");
	}

	@Test
	void testGetFileDoesNotExist() throws Exception {
		when(fileRepository.fileExists("filename")).thenReturn(false);

		assertThrows(FileNotFoundException.class, () -> fileServiceImpl.getFile("filename"));

		verify(fileRepository).fileExists("filename");
		verify(fileRepository, never()).getFileFromStorage("filename");
	}

	@Test
	void testGetFileExceptionWhileGettingFile() throws Exception {
		when(fileRepository.fileExists("filename")).thenReturn(true);
		when(fileRepository.getFileFromStorage("filename")).thenThrow(IOException.class);

		assertThrows(IOException.class, () -> fileServiceImpl.getFile("filename"));

		verify(fileRepository).fileExists("filename");
		verify(fileRepository).getFileFromStorage("filename");
	}

	@Test
	void testDeleteFileSuccess() throws Exception {
		when(fileRepository.fileExists("filename")).thenReturn(true);
		when(fileRepository.deleteFileFromStorage("filename")).thenReturn(true);

		fileServiceImpl.deleteFile("filename");

		verify(fileRepository).fileExists("filename");
		verify(fileRepository).deleteFileFromStorage("filename");
	}

	@Test
	void testDeleteFileDoesNotExist() throws Exception {
		when(fileRepository.fileExists("filename")).thenReturn(false);

		assertThrows(FileNotFoundException.class, () -> fileServiceImpl.deleteFile("filename"));

		verify(fileRepository).fileExists("filename");
		verify(fileRepository, never()).deleteFileFromStorage("filename");
	}

	@Test
	void testDeleteFileExceptionWhileDeletingFile() throws Exception {
		when(fileRepository.fileExists("filename")).thenReturn(true);
		when(fileRepository.deleteFileFromStorage("filename")).thenThrow(IOException.class);

		assertThrows(IOException.class, () -> fileServiceImpl.deleteFile("filename"));

		verify(fileRepository).fileExists("filename");
		verify(fileRepository).deleteFileFromStorage("filename");
	}

	@Test
	void testSaveFileSuccess() throws Exception {

		MockMultipartFile mockMultipartFile = new MockMultipartFile("name", "originalName", null, "A".getBytes());

		when(fileRepository.fileExists("originalName")).thenReturn(false);
		when(fileRepository.saveFileToStorage(mockMultipartFile)).thenReturn(true);

		fileServiceImpl.saveFile(mockMultipartFile);

		verify(fileRepository).fileExists("originalName");
		verify(fileRepository).saveFileToStorage(mockMultipartFile);
	}

	@Test
	void testSaveFileAlreadyExist() throws Exception {
		MockMultipartFile mockMultipartFile = new MockMultipartFile("name", "originalName", null, "A".getBytes());

		when(fileRepository.fileExists("originalName")).thenReturn(true);

		assertThrows(FileAlreadyExistsException.class, () -> fileServiceImpl.saveFile(mockMultipartFile));

		verify(fileRepository).fileExists("originalName");
		verify(fileRepository, never()).saveFileToStorage(mockMultipartFile);
	}

	@Test
	void testSaveFileExceptionWhileSavingFile() throws Exception {
		MockMultipartFile mockMultipartFile = new MockMultipartFile("name", "originalName", null, "A".getBytes());

		when(fileRepository.fileExists("originalName")).thenReturn(false);
		when(fileRepository.saveFileToStorage(mockMultipartFile)).thenThrow(IOException.class);

		assertThrows(IOException.class, () -> fileServiceImpl.saveFile(mockMultipartFile));

		verify(fileRepository).fileExists("originalName");
		verify(fileRepository).saveFileToStorage(mockMultipartFile);
	}

	@Test
	void testGetFileLockExpiresWhileDeletingSameFile() throws Exception {

		when(fileRepository.fileExists("filename")).thenReturn(true);
		when(fileRepository.deleteFileFromStorage("filename")).then(invocation -> {
			Thread.sleep(2000);
			return true;
		});

		executorService.submit(() -> {
			fileServiceImpl.deleteFile("filename");
			return null;
		});
		executorService.submit(() -> assertThrows(LockTimeoutException.class, () -> fileServiceImpl.getFile("filename")));
		executorService.shutdown();
		executorService.awaitTermination(20, TimeUnit.SECONDS);
		verify(fileRepository).fileExists("filename");
		verify(fileRepository).deleteFileFromStorage("filename");
		verify(fileRepository, never()).getFileFromStorage("filename");
	}

	@Test
	void testGetFileDoesNotLockWhenDeletingOtherFile() throws Exception {

		when(fileRepository.fileExists("filename")).thenReturn(true);
		when(fileRepository.fileExists("other")).thenReturn(true);
		when(fileRepository.getFileFromStorage("filename")).thenReturn(resource);
		when(fileRepository.deleteFileFromStorage("other")).then(invocation -> {
			Thread.sleep(2000);
			return true;
		});

		executorService.submit(() -> {
			fileServiceImpl.deleteFile("other");
			return null;
		});
		executorService.submit(() -> {
			try {
				assertEquals(resource, fileServiceImpl.getFile("filename"));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		executorService.shutdown();
		executorService.awaitTermination(20, TimeUnit.SECONDS);

		verify(fileRepository).fileExists("filename");
		verify(fileRepository).fileExists("other");
		verify(fileRepository).deleteFileFromStorage("other");
		verify(fileRepository).getFileFromStorage("filename");
	}

	@Test
	void testGetFileFindsNoFileWhenDeleteCompletesInTime() throws Exception {

		when(fileRepository.fileExists("filename")).thenReturn(true).thenReturn(false);
		when(fileRepository.getFileFromStorage("filename")).thenReturn(resource);
		when(fileRepository.deleteFileFromStorage("filename")).then(invocation -> {
			Thread.sleep(500);
			return true;
		});

		executorService.submit(() -> {
			fileServiceImpl.deleteFile("filename");
			return null;
		});
		executorService.submit(() -> assertThrows(FileNotFoundException.class, () -> fileServiceImpl.getFile("filename")));
		executorService.shutdown();
		executorService.awaitTermination(20, TimeUnit.SECONDS);

		verify(fileRepository, times(2)).fileExists("filename");
		verify(fileRepository).deleteFileFromStorage("filename");
		verify(fileRepository, never()).getFileFromStorage("filename");
	}

	@Test
	void testGetFileDoesNotBlockOtherGetFile() throws Exception {

		when(fileRepository.fileExists("filename")).thenReturn(true);
		when(fileRepository.getFileFromStorage("filename")).then(invocation -> {
			Thread.sleep(2000);
			return resource;
		}).thenReturn(resource);

		executorService.submit(() -> {
			assertEquals(resource, fileServiceImpl.getFile("filename"));
			return null;
		});
		executorService.submit(() -> {
			try {
				assertEquals(resource, fileServiceImpl.getFile("filename"));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		executorService.shutdown();
		executorService.awaitTermination(20, TimeUnit.SECONDS);

		verify(fileRepository, times(2)).fileExists("filename");
		verify(fileRepository, times(2)).getFileFromStorage("filename");
	}

	@Test
	void testDeleteFileLockExpiresWhileGettingSameFile() throws Exception {

		when(fileRepository.fileExists("filename")).thenReturn(true);
		when(fileRepository.getFileFromStorage("filename")).then(invocation -> {
			Thread.sleep(2000);
			return true;
		});

		executorService.submit(() -> {
			fileServiceImpl.getFile("filename");
			return null;
		});
		executorService.submit(() ->assertThrows(LockTimeoutException.class, () -> fileServiceImpl.deleteFile("filename")));
		executorService.shutdown();
		executorService.awaitTermination(20, TimeUnit.SECONDS);
		verify(fileRepository).fileExists("filename");
		verify(fileRepository).getFileFromStorage("filename");
		verify(fileRepository, never()).deleteFileFromStorage("filename");
	}

	@Test
	void testDeleteFileDoesNotTimeoutWhileGettingSameFileCompletesInTime() throws Exception {

		when(fileRepository.fileExists("filename")).thenReturn(true);
		when(fileRepository.getFileFromStorage("filename")).then(invocation -> {
			Thread.sleep(500);
			return true;
		});

		executorService.submit(() -> {
			fileServiceImpl.getFile("filename");
			return null;
		});
		executorService.submit(() -> {
			try {
				fileServiceImpl.deleteFile("filename");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		executorService.shutdown();
		executorService.awaitTermination(20, TimeUnit.SECONDS);
		verify(fileRepository, times(2)).fileExists("filename");
		verify(fileRepository).getFileFromStorage("filename");
		verify(fileRepository).deleteFileFromStorage("filename");
	}
}
