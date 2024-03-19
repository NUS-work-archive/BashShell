package sg.edu.nus.comp.cs4218.impl.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static sg.edu.nus.comp.cs4218.impl.util.ErrorConstants.ERR_SYNTAX;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sg.edu.nus.comp.cs4218.exception.AbstractApplicationException;
import sg.edu.nus.comp.cs4218.exception.ShellException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.File;
import java.util.List;

@SuppressWarnings("PMD.ClassNamingConventions")
public class IORedirectionHandlerIT {
    private ArgumentResolver argResolver;
    private InputStream inputStream;
    private OutputStream outputStream;
    private final String currentDirectory = System.getProperty("user.dir");
    private static final String TEMP1 = "temp1";
    private static final String TEMP2 = "temp2";
    private static final String TXT = ".txt";

    private String[] splitArgs(String args) {
        return args.split("\\s+");
    }

    @BeforeEach
    public void setUp() throws Exception {
        argResolver = new ArgumentResolver();
        inputStream = new ByteArrayInputStream("origInputStream".getBytes(StandardCharsets.UTF_8));
        outputStream = new ByteArrayOutputStream();
    }

    @AfterEach
    void tearDown() throws IOException {
        inputStream.close();
        outputStream.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"< < input.txt", "> > output.txt", "input.txt < > output.txt", "output.txt > < input.txt"})
    void extractRedirOptions_InvalidSyntax_ThrowsShellException(String args) {
        List<String> invalidSyntax = List.of(splitArgs(args));
        IORedirectionHandler ioRedirHandler = new IORedirectionHandler(invalidSyntax, inputStream, outputStream, argResolver);
        ShellException shellException = assertThrows(ShellException.class, ioRedirHandler::extractRedirOptions);
        assertEquals("shell: " + ERR_SYNTAX, shellException.getMessage());
    }

    @Test
    void extractRedirOptions_AmbiguousRedirect_ThrowsShellException() {
        try {
            File tempFileOne = File.createTempFile(TEMP1, TXT, new File(currentDirectory));
            File tempFileTwo = File.createTempFile(TEMP2, TXT, new File(currentDirectory));
            List<String> ambiguousRedirect = List.of(">", "temp*.txt");
            IORedirectionHandler ioRedirHandler = new IORedirectionHandler(ambiguousRedirect, inputStream, outputStream, argResolver);
            ShellException shellException = assertThrows(ShellException.class, ioRedirHandler::extractRedirOptions);
            assertEquals("shell: " + ERR_SYNTAX, shellException.getMessage());
            tempFileOne.delete();
            tempFileTwo.delete();
        } catch (IOException  e) {
            fail(e.getMessage());
        }
    }

    @Test
    void extractRedirOptions_IORedirectionOnly_GetNoRedirArgsListIsEmpty() {
        try{
            File tempFileOne = File.createTempFile(TEMP1, TXT, new File(currentDirectory));
            File tempFileTwo = File.createTempFile(TEMP2, TXT, new File(currentDirectory));
            List<String> ioRedirectsOnly = List.of("<", tempFileOne.getAbsolutePath(), ">", tempFileTwo.getAbsolutePath());
            IORedirectionHandler ioRedirHandler = new IORedirectionHandler(ioRedirectsOnly, inputStream, outputStream, argResolver);
            assertDoesNotThrow(() -> ioRedirHandler.extractRedirOptions());
            assertTrue(ioRedirHandler.getNoRedirArgsList().isEmpty());
            ioRedirHandler.getInputStream().close();
            ioRedirHandler.getOutputStream().close();
            tempFileOne.delete();
            tempFileTwo.delete();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void extractRedirOptions_NoRedirection_GetNoRedirArgsListIsNotEmpty() {
        try {
            File tempFileOne = File.createTempFile(TEMP1, TXT, new File(currentDirectory));
            File tempFileTwo = File.createTempFile(TEMP2, TXT, new File(currentDirectory));
            List<String> noRedirection = List.of(tempFileOne.getAbsolutePath(), tempFileTwo.getAbsolutePath());
            IORedirectionHandler ioRedirHandler = new IORedirectionHandler(noRedirection, inputStream, outputStream, argResolver);
            assertDoesNotThrow(() -> ioRedirHandler.extractRedirOptions());
            assertFalse(ioRedirHandler.getNoRedirArgsList().isEmpty());
            assertSame(inputStream, ioRedirHandler.getInputStream());
            assertSame(outputStream, ioRedirHandler.getOutputStream());
            tempFileOne.delete();
            tempFileTwo.delete();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void extractRedirOptions_OneInputRedirection_InputStreamChangesFromOriginal() {
        try {
            File tempFileOne = File.createTempFile(TEMP1, TXT, new File(currentDirectory));
            File tempFileTwo = File.createTempFile(TEMP2, TXT, new File(currentDirectory));
            List<String> inputs = List.of(tempFileOne.getAbsolutePath(), "<", tempFileTwo.getAbsolutePath());
            IORedirectionHandler ioRedirHandler = new IORedirectionHandler(inputs, inputStream, outputStream, argResolver);
            assertDoesNotThrow(() -> ioRedirHandler.extractRedirOptions());
            String expected = new String(inputStream.readAllBytes());
            String result = new String(ioRedirHandler.getInputStream().readAllBytes());
            assertNotEquals(expected, result);
            assertNotSame(inputStream, ioRedirHandler.getInputStream());
            ioRedirHandler.getInputStream().close();
            tempFileOne.delete();
            tempFileTwo.delete();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void extractRedirOptions_MultipleInputRedirections_TakeLatestInputStream() {
        try {
            File tempFileOne = File.createTempFile(TEMP1, TXT, new File(currentDirectory));
            File tempFileTwo = File.createTempFile(TEMP2, TXT, new File(currentDirectory));
            Files.write(tempFileTwo.toPath(), "This is a temp file".getBytes());
            List<String> inputs = List.of("<", tempFileOne.getAbsolutePath(), "<", tempFileTwo.getAbsolutePath());
            IORedirectionHandler ioRedirHandler = new IORedirectionHandler(inputs, inputStream, outputStream, argResolver);
            assertDoesNotThrow(() -> ioRedirHandler.extractRedirOptions());
            String expected = "This is a temp file";
            String result = new String(ioRedirHandler.getInputStream().readAllBytes());
            assertEquals(expected, result);
            ioRedirHandler.getInputStream().close();
            tempFileOne.delete();
            tempFileTwo.delete();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void extractRedirOptions_OneOutputRedirection_OutputStreamChangesFromOriginal() {
        try {
            File tempFileOne = File.createTempFile(TEMP1, TXT, new File(currentDirectory));
            File tempFileTwo = File.createTempFile(TEMP2, TXT, new File(currentDirectory));
            List<String> inputs = List.of(tempFileOne.getAbsolutePath(), ">", tempFileTwo.getAbsolutePath());
            IORedirectionHandler ioRedirHandler = new IORedirectionHandler(inputs, inputStream, outputStream, argResolver);
            assertDoesNotThrow(() -> ioRedirHandler.extractRedirOptions());
            assertNotSame(outputStream, ioRedirHandler.getOutputStream());
            ioRedirHandler.getOutputStream().close();
            tempFileOne.delete();
            tempFileTwo.delete();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void extractRedirOptions_MultipleOutputRedirections_TakeLatestOutputStream() {
        try {
            File tempFileOne = File.createTempFile(TEMP1, TXT, new File(currentDirectory));
            File tempFileTwo = File.createTempFile(TEMP2, TXT, new File(currentDirectory));
            List<String> inputs = List.of("<", tempFileOne.getAbsolutePath(), ">", tempFileTwo.getAbsolutePath(), ">", tempFileOne.getAbsolutePath());
            IORedirectionHandler ioRedirHandler = new IORedirectionHandler(inputs, inputStream, outputStream, argResolver);
            assertDoesNotThrow(() -> ioRedirHandler.extractRedirOptions());
            assertEquals(0, new String(ioRedirHandler.getInputStream().readAllBytes()).length());
            String str = "Not Empty";
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            ioRedirHandler.getOutputStream().write(bytes);
            assertTrue(new String(ioRedirHandler.getInputStream().readAllBytes()).length() > 0);
            ioRedirHandler.getInputStream().close();
            ioRedirHandler.getOutputStream().close();
            tempFileOne.delete();
            tempFileTwo.delete();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
