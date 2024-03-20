package sg.edu.nus.comp.cs4218.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.io.CleanupMode.ALWAYS;
import static sg.edu.nus.comp.cs4218.test.FileUtils.createNewDirectory;
import static sg.edu.nus.comp.cs4218.test.FileUtils.createNewFileInDir;
import static sg.edu.nus.comp.cs4218.test.FileUtils.deleteFileOrDirectory;
import static sg.edu.nus.comp.cs4218.testutils.TestStringUtils.CHAR_FILE_SEP;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SetUpSystemTest extends AbstractSystemTest {
    private final static String FILE_CONTENT = "line1\nline2\nline3\nabc\nline4\nabc\nline5\n";
    private final static String DIR_NAME = "testDir";

    private static Path file;
    private static Path dir;
    private static Path nestedFile;
    private static String fileName = "";
    private static String nestedFileName = "";

    @Override
    @BeforeEach
    void beforeEach(@TempDir(cleanup = ALWAYS) Path tempDir) {
        super.beforeEach(tempDir);
        file = createNewFileInDir(tempDir, "testFile.txt", FILE_CONTENT);
        fileName = file.toFile().getName();
        dir = createNewDirectory(tempDir, DIR_NAME);
        nestedFile = createNewFileInDir(dir, "nestedFile.txt", FILE_CONTENT);
        nestedFileName = nestedFile.toFile().getName();
    }

    @AfterEach
    void afterEach() {
        deleteFileOrDirectory(nestedFile);
        deleteFileOrDirectory(dir);
        deleteFileOrDirectory(file);
    }

    @Test
    void main_RmFile_RmSuccessfully() {
        SystemTestResults actual = testMainWith(
                LS_APP,
                EXIT_APP
        );
        assertTrue(actual.out.contains(fileName));

        actual = testMainWith(
                RM_APP + " " + fileName,
                LS_APP,
                EXIT_APP
        );
        assertFalse(actual.out.contains(fileName));
    }

    @Test
    void main_RmDirWithContents_RmSuccessfully() {
        SystemTestResults actual = testMainWith(
                LS_APP,
                EXIT_APP
        );
        assertTrue(actual.out.contains(DIR_NAME));

        actual = testMainWith(
                RM_APP + " " + DIR_NAME,
                LS_APP,
                EXIT_APP
        );
        assertTrue(actual.out.contains(DIR_NAME));

        actual = testMainWith(
                RM_APP + " -d " + DIR_NAME,
                LS_APP,
                EXIT_APP
        );
        assertTrue(actual.out.contains(DIR_NAME));

        actual = testMainWith(
                RM_APP + " -r " + DIR_NAME,
                LS_APP,
                EXIT_APP
        );
        assertFalse(actual.out.contains(DIR_NAME));
    }

    @Test
    void main_SortFile_PrintsSortedFileContents() {
        SystemTestResults actual = testMainWith(
                SORT_APP + " " + fileName,
                EXIT_APP
        );
        String expected = actual.rootDirectory + "$ " + "abc\nabc\nline1\nline2\nline3\nline4\nline5";
        assertEquals(expected, actual.out);
    }

    @Test
    void main_MvAllFilesInChildDirToParentDirConnectedUsingSemicolon_MvSuccessfully() {
        SystemTestResults actual = testMainWith(
                CD_APP + " " + DIR_NAME + "; " + MV_APP + " * ..",
                LS_APP + "; " + CD_APP + " ..; " + LS_APP,
                EXIT_APP
        );
        String expected = String.format("%s$ %s%c%s$ \n%s\n%s\n%s", actual.rootDirectory, actual.rootDirectory,
                CHAR_FILE_SEP, DIR_NAME, nestedFileName, DIR_NAME, fileName);
        assertEquals(expected, actual.out);
    }

    @Test
    void main_PasteFileAndPipeToCut_PrintsCutContents() {
        SystemTestResults actual = testMainWith(
                PASTE_APP + " " + fileName + " | " + CUT_APP + " -c 1-3",
                EXIT_APP
        );
        String expected = actual.rootDirectory + "$ " + "lin\nlin\nlin\nabc\nlin\nabc\nlin\n";
        assertEquals(expected, actual.out);
    }

    @Test
    void main_GrepFileAndPipeToWc_PrintsCorrectCount() {
        SystemTestResults actual = testMainWith(
                GREP_APP + " abc " + fileName + " | " + WC_APP,
                EXIT_APP
        );
        String expected = actual.rootDirectory + "$ " + String.format(" %7d %7d %7d", 2, 2, 10);
        assertEquals(expected, actual.out);
    }
}
