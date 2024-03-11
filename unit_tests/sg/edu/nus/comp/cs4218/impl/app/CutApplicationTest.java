package sg.edu.nus.comp.cs4218.impl.app;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static sg.edu.nus.comp.cs4218.impl.util.StringUtils.STRING_NEWLINE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import sg.edu.nus.comp.cs4218.exception.CutException;

class CutApplicationTest {

    private static final List<int[]> ONE_TO_FIVE_RANGE = List.of(new int[]{1, 5});
    private static final String FILE_ONE = "file1.txt";
    private static final String FILE_TWO = "file2.txt";
    private static final String FILE_ONE_CONTENT = "1234567890";
    private static final String FILE_TWO_CONTENT = "0987654321";
    private static final String ONE_TO_FIVE = "12345";
    private static final String ZERO_TO_SIX = "09876";
    private static final String NON_EXIST_FILE = "cut: 'nonExistFile.txt': No such file or directory";

    @TempDir
    private Path tempDir;
    private Path fileOnePath;
    private String fileOne;
    private String fileTwo;
    private String nonExistFile;
    private CutApplication app;

    @BeforeEach
    void setUp() throws IOException {
        app = new CutApplication();

        // Create temporary file, automatically deletes after test execution
        fileOnePath = tempDir.resolve(FILE_ONE);
        Path fileTwoPath = tempDir.resolve(FILE_TWO);

        fileOne = fileOnePath.toString();
        fileTwo = fileTwoPath.toString();
        nonExistFile = tempDir.resolve("nonExistFile.txt").toString();

        Files.createFile(fileOnePath);
        Files.createFile(fileTwoPath);

        // Writes content to temporary file
        Files.write(fileOnePath, FILE_ONE_CONTENT.getBytes());
        Files.write(fileTwoPath, FILE_TWO_CONTENT.getBytes());
    }

    // The tests do not cover scenarios where no flag is provided, more than one flag is given,
    // or the invalidity of the range, as exceptions are expected to be thrown before reaching the cutFromFiles method.

    @Test
    void cutFromFiles_EmptyFile_ThrowsCutException() {
        CutException result = assertThrowsExactly(CutException.class, () ->
                app.cutFromFiles(false, false, null, new String[0])
        );
        String expected = "cut: Null arguments";
        assertEquals(expected, result.getMessage());
    }

    @Test
    void cutFromFiles_FileDoNotExist_PrintsErrorMessage() {
        String result = assertDoesNotThrow(() -> app.cutFromFiles(true, false, null, nonExistFile));
        assertEquals(NON_EXIST_FILE, result);
    }

    @Test
    void cutFromFiles_FileGivenAsDirectory_PrintsErrorMessage() {
        Path subDir = tempDir.resolve("subdirectory");
        assertDoesNotThrow(() -> Files.createDirectories(subDir));
        String result = assertDoesNotThrow(() -> app.cutFromFiles(true, false, ONE_TO_FIVE_RANGE, subDir.toString()));
        String expected = "cut: 'subdirectory': This is a directory";
        assertEquals(expected, result);
    }

    @Test
    @DisabledOnOs(value = OS.WINDOWS)
    void cutFromFiles_FileNoPermissionToRead_PrintsErrorMessage() {
        boolean isSetReadable = fileOnePath.toFile().setReadable(false);
        if (isSetReadable) {
            fail("Failed to set read permission to false for test");
        }

        String result = assertDoesNotThrow(() -> app.cutFromFiles(true, false, ONE_TO_FIVE_RANGE, fileOne));
        String expected = String.format("cut: '%s': Permission denied", fileOne);
        assertEquals(expected, result);
    }

    @Test
    void cutFromFiles_CutByChar_ReturnsCutString() {
        String result = assertDoesNotThrow(() -> app.cutFromFiles(true, false, ONE_TO_FIVE_RANGE, fileOne));
        assertEquals(ONE_TO_FIVE, result);
    }

    @Test
    void cutFromFiles_CutByByte_ReturnsCutString() {
        String result = assertDoesNotThrow(() -> app.cutFromFiles(false, true, ONE_TO_FIVE_RANGE, fileOne));
        assertEquals(ONE_TO_FIVE, result);
    }

    @Test
    void cutFromFiles_FileNoContent_ReturnsEmptyString() {
        // Given: overwrites the file content with an empty string
        assertDoesNotThrow(() -> Files.write(fileOnePath, "".getBytes()));
        String expected = assertDoesNotThrow(() -> String.join("", Files.readAllLines(fileOnePath)));
        assertTrue(expected.isEmpty());

        String result = assertDoesNotThrow(() -> app.cutFromFiles(true, false, ONE_TO_FIVE_RANGE, fileOne));
        assertEquals(expected, result);
    }

    @Test
    void cutFromFiles_MultipleFiles_ReturnsCutString() {
        String result = assertDoesNotThrow(() -> app.cutFromFiles(false, true, ONE_TO_FIVE_RANGE, fileOne, fileTwo));
        String expected = ONE_TO_FIVE + STRING_NEWLINE + ZERO_TO_SIX;
        assertEquals(expected, result);
    }

    @Test
    void cutFromFiles_SomeFilesAtTheStartDoNotExist_ReturnsCutStringAndErrorMessage() {
        String result = assertDoesNotThrow(() -> app.cutFromFiles(false, true, ONE_TO_FIVE_RANGE, nonExistFile, fileOne, fileTwo));
        String expected = ONE_TO_FIVE + STRING_NEWLINE + ZERO_TO_SIX + STRING_NEWLINE + NON_EXIST_FILE;
        assertEquals(expected, result);
    }

    @Test
    void cutFromFiles_SomeFilesInTheMiddleDoNotExist_ReturnsCutStringAndErrorMessage() {
        String result = assertDoesNotThrow(() -> app.cutFromFiles(false, true, ONE_TO_FIVE_RANGE, fileOne, nonExistFile, fileTwo));
        String expected = ONE_TO_FIVE + STRING_NEWLINE + ZERO_TO_SIX + STRING_NEWLINE + NON_EXIST_FILE;
        assertEquals(expected, result);
    }

    @Test
    void cutFromFiles_SomeFilesAtTheEndDoNotExist_ReturnsCutStringAndErrorMessage() {
        String result = assertDoesNotThrow(() -> app.cutFromFiles(false, true, ONE_TO_FIVE_RANGE, fileOne, fileTwo, nonExistFile));
        String expected = ONE_TO_FIVE + STRING_NEWLINE + ZERO_TO_SIX + STRING_NEWLINE + NON_EXIST_FILE;
        assertEquals(expected, result);
    }

    @Test
    void cutFromStdin_NullStdin_ThrowsCutException() {
        CutException result = assertThrowsExactly(CutException.class, () -> app.cutFromStdin(false, false, null, null));
        String expected = "cut: Null Pointer Exception";
        assertEquals(expected, result.getMessage());
    }

    @Test
    void cutFromStdin_CutByChar_ReturnsCutString() {
        InputStream stdin = new ByteArrayInputStream(FILE_ONE_CONTENT.getBytes());
        String result = assertDoesNotThrow(() -> app.cutFromStdin(true, false, ONE_TO_FIVE_RANGE, stdin));
        assertEquals(ONE_TO_FIVE, result);
    }

    @Test
    void cutFromStdin_CutByByte_ReturnsCutString() {
        InputStream stdin = new ByteArrayInputStream(FILE_ONE_CONTENT.getBytes());
        String result = assertDoesNotThrow(() -> app.cutFromStdin(true, false, ONE_TO_FIVE_RANGE, stdin));
        assertEquals(ONE_TO_FIVE, result);
    }

    @Test
    void cutFromStdin_StdinNoContent_ReturnsEmptyString() {
        InputStream stdin = new ByteArrayInputStream("".getBytes());
        String result = assertDoesNotThrow(() -> app.cutFromStdin(true, false, ONE_TO_FIVE_RANGE, stdin));
        String expected = "";
        assertEquals(expected, result);
    }
}
