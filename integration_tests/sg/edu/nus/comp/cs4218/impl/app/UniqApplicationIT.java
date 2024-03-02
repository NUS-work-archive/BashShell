package sg.edu.nus.comp.cs4218.impl.app;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static sg.edu.nus.comp.cs4218.impl.util.StringUtils.STRING_NEWLINE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import sg.edu.nus.comp.cs4218.exception.UniqException;

@SuppressWarnings("PMD.ClassNamingConventions")
public class UniqApplicationIT {
    private static final String TEST_RESOURCES = "resources/uniq/";
    private static final String TEST_INPUT_FILE = TEST_RESOURCES + "input.txt";
    private static final String STR_HELLO_WORLD = "Hello World";
    private static final String STR_ALICE = "Alice";
    private static final String STR_BOB = "Bob";

    private UniqApplication app;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() {
        this.app = new UniqApplication();
        this.outputStream = new ByteArrayOutputStream();
    }

    @AfterEach
    void tearDown() throws IOException {
        this.outputStream.close();
    }

    @Test
    void run_NoFlags_OnlyUniqueAdjacentLines() {
        //Given
        final String[] args = {TEST_INPUT_FILE};
        final String expected = STR_HELLO_WORLD + STRING_NEWLINE +
                STR_ALICE + STRING_NEWLINE +
                STR_BOB + STRING_NEWLINE +
                STR_ALICE + STRING_NEWLINE +
                STR_BOB + STRING_NEWLINE;

        assertDoesNotThrow(() -> this.app.run(args, null, this.outputStream)); // When
        assertEquals(expected, this.outputStream.toString()); // Then
    }

    @Test
    void run_CountFlag_CorrectOccurrenceCounts() {
        //Given
        final String[] args = {"-c", TEST_INPUT_FILE};
        final String expected = "2 Hello World" + STRING_NEWLINE +
                "2 Alice" + STRING_NEWLINE +
                "1 Bob" + STRING_NEWLINE +
                "1 Alice" + STRING_NEWLINE +
                "1 Bob" + STRING_NEWLINE;

        assertDoesNotThrow(() -> this.app.run(args, null, this.outputStream)); // When
        assertEquals(expected, this.outputStream.toString()); // Then
    }

    @Test
    void run_GroupDuplicatesFlag_OnlyOnePerDuplicateGroup() {
        //Given
        final String[] args = {"-d", TEST_INPUT_FILE};
        final String expected = STR_HELLO_WORLD + STRING_NEWLINE +
                STR_ALICE + STRING_NEWLINE;

        assertDoesNotThrow(() -> this.app.run(args, null, this.outputStream)); // When
        assertEquals(expected, this.outputStream.toString()); // Then
    }

    @Test
    void run_AllDuplicatesFlag_AllDuplicateLines() {
        //Given
        final String[] args = {"-D", TEST_INPUT_FILE};
        final String expected = STR_HELLO_WORLD + STRING_NEWLINE +
                STR_HELLO_WORLD + STRING_NEWLINE +
                STR_ALICE + STRING_NEWLINE +
                STR_ALICE + STRING_NEWLINE;

        assertDoesNotThrow(() -> this.app.run(args, null, this.outputStream)); // When
        assertEquals(expected, this.outputStream.toString()); // Then
    }

    @Test
    @DisplayName("Test -D flag's precedence over -d flag")
    void run_GroupDuplicatesAndAllDuplicatesFlags_AllDuplicateLines() {
        //Given
        final String[] args = {"-dD", TEST_INPUT_FILE};
        final String expected = STR_HELLO_WORLD + STRING_NEWLINE +
                STR_HELLO_WORLD + STRING_NEWLINE +
                STR_ALICE + STRING_NEWLINE +
                STR_ALICE + STRING_NEWLINE;

        assertDoesNotThrow(() -> this.app.run(args, null, this.outputStream)); // When
        assertEquals(expected, this.outputStream.toString()); // Then
    }

    @Test
    void run_CountAndGroupDuplicateFlags_CorrectCountAndLines() {
        //Given
        final String[] args = {"-cd", TEST_INPUT_FILE};
        final String expected = "2 Hello World" + STRING_NEWLINE +
                "2 Alice" + STRING_NEWLINE;

        assertDoesNotThrow(() -> this.app.run(args, null, this.outputStream)); // When
        assertEquals(expected, this.outputStream.toString()); // Then
    }

    @Test
    void run_CountAndAllDuplicatesFlags_ThrowsUniqException() {
        //Given
        final String[] args = {"-cD", TEST_INPUT_FILE};
        final String expectedMessage = "uniq: printing all duplicated lines and repeat counts is meaningless";

        Throwable thrown = assertThrows(UniqException.class, () -> this.app.run(args, null, null)); // When
        assertEquals(expectedMessage, thrown.getMessage()); // Then
    }
}
