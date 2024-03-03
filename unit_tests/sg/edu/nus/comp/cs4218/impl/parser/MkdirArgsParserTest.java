package sg.edu.nus.comp.cs4218.impl.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sg.edu.nus.comp.cs4218.impl.parser.ArgsParser.ILLEGAL_FLAG_MSG;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import sg.edu.nus.comp.cs4218.exception.InvalidArgsException;

class MkdirArgsParserTest {

    private final static String FLAG_CR_PARENT = "-p";
    private final static String FILE_ONE = "file1";
    private final static String FILE_TWO = "file2";
    private final static String FILE_THREE = "file3";
    private final Set<Character> VALID_FLAGS = Set.of('p');
    private MkdirArgsParser mkdirArgsParser;

    private static Stream<Arguments> validSyntax() {
        return Stream.of(
                Arguments.of((Object) new String[]{}),
                Arguments.of((Object) new String[]{FLAG_CR_PARENT}),
                Arguments.of((Object) new String[]{FILE_ONE}),
                Arguments.of((Object) new String[]{FLAG_CR_PARENT, FILE_ONE}),
                Arguments.of((Object) new String[]{FILE_ONE, FLAG_CR_PARENT}),
                Arguments.of((Object) new String[]{FILE_ONE, FILE_TWO}),
                Arguments.of((Object) new String[]{FLAG_CR_PARENT, FILE_ONE, FILE_TWO}),
                Arguments.of((Object) new String[]{FILE_ONE, FILE_TWO, FLAG_CR_PARENT}),
                Arguments.of((Object) new String[]{FLAG_CR_PARENT, FILE_ONE, FILE_TWO, FILE_THREE}),
                Arguments.of((Object) new String[]{FILE_ONE, FILE_TWO, FILE_THREE, FLAG_CR_PARENT})
        );
    }

    private static Stream<Arguments> invalidSyntax() {
        return Stream.of(
                Arguments.of((Object) new String[]{"-P"}),
                Arguments.of((Object) new String[]{"--"})
        );
    }

    @BeforeEach
    void setUp() {
        mkdirArgsParser = new MkdirArgsParser();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\n"})
    void parse_EmptyString_ReturnsEmptyFlagsAndNonFlagArgsContainsInput(String args)
            throws InvalidArgsException {
        mkdirArgsParser.parse(args);
        assertTrue(mkdirArgsParser.flags.isEmpty());
        assertTrue(mkdirArgsParser.nonFlagArgs.contains(args));
    }

    @Test
    void parse_ValidFlag_ReturnsGivenMatchingFlag() {
        assertDoesNotThrow(() -> mkdirArgsParser.parse(FLAG_CR_PARENT));
        assertEquals(VALID_FLAGS, mkdirArgsParser.flags, "Flags do not match");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-a", "-1", "-!", "-P", "--"})
    void parse_InvalidFlag_ThrowsInvalidArgsException(String args) {
        Throwable result = assertThrows(InvalidArgsException.class, () -> {
            mkdirArgsParser.parse(args);
        });
        assertEquals(ILLEGAL_FLAG_MSG + args.charAt(1), result.getMessage());
    }

    @ParameterizedTest
    @MethodSource("validSyntax")
    void parse_validSyntax_DoNotThrowException(String... args) {
        assertDoesNotThrow(() -> mkdirArgsParser.parse(args));
    }

    @ParameterizedTest
    @MethodSource("invalidSyntax")
    void parse_invalidSyntax_ThrowsInvalidArgsException(String... args) {
        assertThrows(InvalidArgsException.class, () -> mkdirArgsParser.parse(args));
    }

    @Test
    void isCreateParent_NoFlag_ReturnsFalse() {
        assertDoesNotThrow(() -> mkdirArgsParser.parse());
        assertFalse(mkdirArgsParser.isCreateParent());
    }

    @Test
    void isCreateParent_ValidFlag_ReturnsTrue() {
        assertDoesNotThrow(() -> mkdirArgsParser.parse(FLAG_CR_PARENT));
        assertTrue(mkdirArgsParser.isCreateParent());
    }

    @Test
    void isCreateParent_ValidFlagAndNonFlagArg_ReturnsTrue() {
        assertDoesNotThrow(() -> mkdirArgsParser.parse(FLAG_CR_PARENT, FILE_ONE));
        assertTrue(mkdirArgsParser.isCreateParent());
    }

    @Test
    void isCreateParent_OnlyNonFlagArg_ReturnsFalse() {
        assertDoesNotThrow(() -> mkdirArgsParser.parse(FILE_ONE));
        assertFalse(mkdirArgsParser.isCreateParent());
    }

    @Test
    void getDirectories_NoArg_ReturnsEmpty() {
        assertDoesNotThrow(() -> mkdirArgsParser.parse());
        List<String> result = mkdirArgsParser.getDirectories();
        assertTrue(result.isEmpty());
    }

    @Test
    void getDirectories_OneNonFlagArg_ReturnsOneFolder() {
        assertDoesNotThrow(() -> mkdirArgsParser.parse(FILE_ONE));
        List<String> expected = List.of(FILE_ONE);
        List<String> result = mkdirArgsParser.getDirectories();
        assertEquals(expected, result);
    }

    @Test
    void getDirectories_MultipleNonFlagArg_ReturnsMultipleFolder() {
        assertDoesNotThrow(() -> mkdirArgsParser.parse(FILE_ONE, FILE_TWO, FILE_THREE));
        List<String> expected = List.of(FILE_ONE, FILE_TWO, FILE_THREE);
        List<String> result = mkdirArgsParser.getDirectories();
        assertEquals(expected, result);
    }

    @Test
    void getDirectories_ValidFlagAndOneNonFlagArg_ReturnsOneFolder() {
        assertDoesNotThrow(() -> mkdirArgsParser.parse(FLAG_CR_PARENT, FILE_ONE));
        List<String> expected = List.of(FILE_ONE);
        List<String> result = mkdirArgsParser.getDirectories();
        assertEquals(expected, result);
    }
}
