package ford.james.motorola.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ford.james.motorola.FileStorageProperties;

@SpringBootTest
@AutoConfigureMockMvc
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileControllerE2ETest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	public void testListFilesSuccess() throws Exception {
		mockMvc.perform(get("/files/list"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	public void testDeleteFileSuccess() throws Exception {

		Path filePath = Path.of("src/test/resources/uploadFiles/fileDelete.txt");

		Files.createFile(filePath);
		assertTrue(Files.exists(filePath));

		mockMvc.perform(delete("/files/delete/fileDelete.txt"))
				.andExpect(status().isOk());

		assertFalse(Files.exists(filePath));
	}

	@Test
	public void testDeleteFileInvalidName() throws Exception {

		Path filePath = Path.of("src/test/resources/uploadFiles/fileDelete.txt");

		Files.createFile(filePath);
		assertTrue(Files.exists(filePath));

		mockMvc.perform(delete("/files/delete/fileDelete&&&.txt"))
				.andExpect(status().isBadRequest());

		assertTrue(Files.exists(filePath));

		Files.delete(filePath);
	}

	@Test
	public void testDeleteFileDoesNotExist() throws Exception {

		Path filePath = Path.of("src/test/resources/uploadFiles/fileDelete.txt");

		assertFalse(Files.exists(filePath));

		mockMvc.perform(delete("/files/delete/fileDelet.txt"))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testUploadFileSuccessSmallTextFile() throws Exception {

		MockMultipartFile multipartFile = new MockMultipartFile("file", "simpleFile.txt", MediaType.TEXT_PLAIN.getType(),
				"Just a file".getBytes());

		mockMvc.perform(multipart("/files/upload")
						.file(multipartFile))
				.andExpect(status().isOk());

		Path filePath = Path.of("src/test/resources/uploadFiles/simpleFile.txt");
		assertTrue(Files.exists(filePath));
		Files.delete(filePath);
	}

	@Test
	public void testUploadFileSuccessImageFile() throws Exception {

		byte[] fileBytes = Files.readAllBytes(Path.of("src/test/resources/testFiles/testImage.jpg"));

		MockMultipartFile multipartFile = new MockMultipartFile("file", "testImage.jpg", MediaType.IMAGE_JPEG.getType(),
				fileBytes);

		mockMvc.perform(multipart("/files/upload")
						.file(multipartFile))
				.andExpect(status().isOk());

		Path filePath = Path.of("src/test/resources/uploadFiles/testImage.jpg");
		assertEquals(fileBytes.length, Files.readAllBytes(filePath).length);
		Files.delete(filePath);
	}

	@Test
	public void testUploadFileInvalidFileName() throws Exception {

		MockMultipartFile multipartFile = new MockMultipartFile("file", "simple&&&File.txt", MediaType.TEXT_PLAIN.getType(),
				"Just a file".getBytes());

		mockMvc.perform(multipart("/files/upload")
						.file(multipartFile))
				.andExpect(status().isBadRequest());

		Path filePath = Path.of("src/test/resources/uploadFiles/simpleFile.txt");
		assertFalse(Files.exists(filePath));
	}

	@Test
	public void testDownloadFileSuccessTextFile() throws Exception {

		MvcResult response = mockMvc.perform(get("/files/download/fileA.txt"))
				.andExpect(status().isOk())
				.andReturn();

		assertEquals("Just a random small text file", response.getResponse().getContentAsString());
	}

	@Test
	public void testDownloadFileSuccessImageFile() throws Exception {

		byte[] fileBytes = Files.readAllBytes(Path.of("src/test/resources/uploadFiles/imageUploaded.jpg"));

		MvcResult response = mockMvc.perform(get("/files/download/imageUploaded.jpg"))
				.andExpect(status().isOk())
				.andReturn();

		assertEquals(fileBytes.length, response.getResponse().getContentLength());
	}

	@Test
	public void testDownloadFileNoFileExists() throws Exception {
		mockMvc.perform(get("/files/download/imageNotThere.jpg"))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testDownloadFileInvalidName() throws Exception {
		mockMvc.perform(get("/files/download/imageNotThere$$$.jpg"))
				.andExpect(status().isBadRequest());
	}

}
