package sg.edu.nus.comp.cs4218.impl.app;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import sg.edu.nus.comp.cs4218.exception.RmException;
import sg.edu.nus.comp.cs4218.impl.util.StringUtils;

public class RmApplicationTest {
    private static final String TEST_RESOURCES = "resources/rm/";
    private static final String EMPTY_DIRECTORY = "empty_directory";
    private static final String TEST_DIRECTORY = "test_folder";
    private static final String NON_EXIST_FILE = "does-not-exist.txt";
    private static final String TEST_FILE_ONE = "test-file-1.txt";

    @TempDir
    private Path testingDirectory;

    private RmApplication app;

    @BeforeEach
    void setUp(@TempDir(cleanup = CleanupMode.ALWAYS) Path tempDir) throws IOException {

        final String resourceDirectory = StringUtils.removeTrailing(TEST_RESOURCES, "/");
        this.testingDirectory = tempDir;
        this.app = new RmApplication();

        try (Stream<Path> stream = Files.walk(Paths.get(resourceDirectory))) {
            stream.forEach(source -> {
                Path destination = Paths.get(this.testingDirectory.toString(),
                        source.toString().substring(resourceDirectory.length()));

                try {
                    Files.copy(source, destination, REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to copy test resources to temp directory.", e);
                }
            });

            Files.createDirectory(this.testingDirectory.resolve(EMPTY_DIRECTORY));
        }
    }

    @Test
    void remove_ExistingFile_SuccessfullyRemoveFile() {
        assertDoesNotThrow(() -> this.app.remove(false, false, TEST_FILE_ONE));
        assertTrue(Files.notExists(Paths.get(this.testingDirectory.toString(), TEST_FILE_ONE)));
    }

    @Test
    void remove_NonExistentFile_ThrowsRmException() {
        final String expectedMsg = String.format("rm: cannot remove '%s': No such file or directory", NON_EXIST_FILE);
        RmException exception = assertThrowsExactly(RmException.class, () ->
                this.app.remove(false, false, NON_EXIST_FILE)
        ); // When
        assertEquals(expectedMsg, exception.getMessage()); // Then
    }

    @Test
    void remove_DirectoryWithoutFlag_ThrowsRmException() {
        final String expectedMsg = String.format("rm: cannot remove '%s': Is a directory", TEST_DIRECTORY);
        RmException exception = assertThrowsExactly(RmException.class, () ->
                this.app.remove(false, false, TEST_DIRECTORY)
        ); // When
        assertEquals(expectedMsg, exception.getMessage()); // Then
    }

    @Test
    void remove_EmptyDirectory_SuccessfullyRemoveDirectory() {
        assertDoesNotThrow(() -> this.app.remove(true, false, EMPTY_DIRECTORY));
        assertTrue(Files.notExists(Paths.get(this.testingDirectory.toString(), EMPTY_DIRECTORY)));
    }

    @Test
    void remove_NonEmptyDirectory_ThrowsRmException() {
        final String expectedMsg = String.format("rm: cannot remove '%s': Directory not empty", TEST_DIRECTORY);
        RmException exception = assertThrowsExactly(RmException.class, () ->
                this.app.remove(true, false, TEST_DIRECTORY)
        ); // When
        assertEquals(expectedMsg, exception.getMessage()); // Then
    }

    @Test
    void remove_SingleDirectoryRecursively_SuccessfullyRemoveAllFilesAndDirectory() {
        assertDoesNotThrow(() -> this.app.remove(false, true, TEST_DIRECTORY));
        assertTrue(Files.exists(Paths.get(this.testingDirectory.toString(), TEST_FILE_ONE))); // should exist
        assertTrue(Files.exists(Paths.get(this.testingDirectory.toString(), EMPTY_DIRECTORY))); // should exist
        assertTrue(Files.notExists(Paths.get(this.testingDirectory.toString(), TEST_DIRECTORY)));
    }

}
