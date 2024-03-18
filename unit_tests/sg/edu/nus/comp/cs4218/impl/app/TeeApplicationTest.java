package sg.edu.nus.comp.cs4218.impl.app;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static sg.edu.nus.comp.cs4218.impl.util.StringUtils.STRING_NEWLINE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import sg.edu.nus.comp.cs4218.impl.util.IOUtils;
import sg.edu.nus.comp.cs4218.impl.util.StringUtils;

class TeeApplicationTest {

    private static final String FILE_A = "A.txt";
    private static final String FILE_B = "B.txt";
    private static final String EMPTY_FILE = "empty.txt";
    private static final String CONTENT_FILE_A = "A" + STRING_NEWLINE + "B" + STRING_NEWLINE + "C" + STRING_NEWLINE +
            "D" + STRING_NEWLINE + "E" + STRING_NEWLINE;
    private static final String CONTENT_FILE_B = "1" + STRING_NEWLINE + "2" + STRING_NEWLINE + "3" +
            STRING_NEWLINE + "4" + STRING_NEWLINE + "5" + STRING_NEWLINE;
    private TeeApplication app;
    private InputStream inputStream;
    @TempDir
    private Path tempDir;
    private Path fileBPath;
    private Path emptyFilePath;
    private String fileA;
    private String fileB;
    private String emptyFile;

    @BeforeEach
    void setUp() throws IOException {
        this.app = new TeeApplication();

        Path fileAPath = tempDir.resolve(FILE_A);
        fileBPath = tempDir.resolve(FILE_B);
        emptyFilePath = tempDir.resolve(EMPTY_FILE);

        fileA = fileAPath.toString();
        fileB = fileBPath.toString();
        emptyFile = emptyFilePath.toString();

        Files.write(fileAPath, CONTENT_FILE_A.getBytes(StandardCharsets.UTF_8));
        Files.write(fileBPath, CONTENT_FILE_B.getBytes(StandardCharsets.UTF_8));
        Files.createFile(emptyFilePath);

        inputStream = assertDoesNotThrow(() -> IOUtils.openInputStream(fileA));
    }

    @AfterEach
    void tearDown() throws IOException {
        inputStream.close();
    }

    @Test
    void teeFromStdin_OnlyStdin_ReturnsCorrectString() {
        String expected = CONTENT_FILE_A;
        String[] filenames = {};
        String outputStdOut = assertDoesNotThrow(() -> app.teeFromStdin(false, inputStream, filenames));
        assertEquals(expected, outputStdOut);
    }

    @Test
    void teeFromStdin_OneFile_WritesToFile() {
        String outputStdOut = assertDoesNotThrow(() -> app.teeFromStdin(false, inputStream, emptyFile));
        String outputFile = assertDoesNotThrow(() -> Files.readString(emptyFilePath));
        String expected = CONTENT_FILE_A;
        assertEquals(expected, outputStdOut);
        assertEquals(expected, outputFile);
    }

    @Test
    void teeFromStdin_OneFileAndValidFlag_AppendsToFile() {
        String outputStdOut = assertDoesNotThrow(() -> app.teeFromStdin(true, inputStream, fileB));
        String outputFile = assertDoesNotThrow(() -> Files.readString(fileBPath));
        String expected = CONTENT_FILE_B + CONTENT_FILE_A;
        assertEquals(CONTENT_FILE_A, outputStdOut);
        assertEquals(expected, outputFile);
    }
}
