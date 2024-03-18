package sg.edu.nus.comp.cs4218.impl.app;

import static sg.edu.nus.comp.cs4218.impl.util.CollectionsUtils.listToArray;
import static sg.edu.nus.comp.cs4218.impl.util.ErrorConstants.ERR_FILE_NOT_FOUND;
import static sg.edu.nus.comp.cs4218.impl.util.ErrorConstants.ERR_IO_EXCEPTION;
import static sg.edu.nus.comp.cs4218.impl.util.ErrorConstants.ERR_IS_DIR;
import static sg.edu.nus.comp.cs4218.impl.util.ErrorConstants.ERR_NO_ISTREAM;
import static sg.edu.nus.comp.cs4218.impl.util.ErrorConstants.ERR_NULL_ARGS;
import static sg.edu.nus.comp.cs4218.impl.util.ErrorConstants.ERR_NULL_STREAMS;
import static sg.edu.nus.comp.cs4218.impl.util.ErrorConstants.ERR_READING_FILE;
import static sg.edu.nus.comp.cs4218.impl.util.ErrorConstants.ERR_WRITE_STREAM;
import static sg.edu.nus.comp.cs4218.impl.util.StringUtils.STRING_NEWLINE;
import static sg.edu.nus.comp.cs4218.impl.util.StringUtils.STRING_TAB;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import sg.edu.nus.comp.cs4218.app.PasteInterface;
import sg.edu.nus.comp.cs4218.exception.InvalidArgsException;
import sg.edu.nus.comp.cs4218.exception.PasteException;
import sg.edu.nus.comp.cs4218.exception.ShellException;
import sg.edu.nus.comp.cs4218.impl.parser.PasteArgsParser;
import sg.edu.nus.comp.cs4218.impl.util.IOUtils;

/**
 * The paste command merge lines of files, write to standard output lines consisting of
 * sequentially corresponding lines of each given file, separated by a TAB character.
 *
 * <p>
 * <b>Command format:</b> <code>paste [Option] [FILES]...</code>
 * </p>
 */
public class PasteApplication implements PasteInterface {

    /**
     * Runs the paste application with the specified arguments.
     *
     * @param args   Array of arguments for the application. Each array element is the path to a
     *               file. If no files are specified stdin is used. If one file is specified, stdin used
     *               for other file.
     * @param stdin  An InputStream. The input for the command is read from this InputStream if no
     *               files are specified.
     * @param stdout An OutputStream. The output of the command is written to this OutputStream.
     */
    @Override
    public void run(String[] args, InputStream stdin, OutputStream stdout) throws PasteException {
        if (stdin == null) {
            throw new PasteException(ERR_NO_ISTREAM);
        }
        if (stdout == null) {
            throw new PasteException(ERR_NULL_STREAMS);
        }
        final PasteArgsParser parser = new PasteArgsParser();

        try {
            parser.parse(args);
        } catch (InvalidArgsException e) {
            throw new PasteException(e.getMessage(), e);
        }

        final Boolean isSerial = parser.isSerial();
        final Boolean hasStdin = parser.hasStdin();
        final String[] nonFlagArgs = listToArray(parser.getNonFlagArgs());

        final StringBuilder output = new StringBuilder();
        if (nonFlagArgs.length == 0) {
            output.append(mergeStdin(isSerial, stdin));
        } else {
            output.append(mergeFileAndStdin(isSerial, stdin, nonFlagArgs));
        }

        try {
            if (output.length() != 0) {
                stdout.write(output.toString().getBytes());
                stdout.write(STRING_NEWLINE.getBytes());
            }
        } catch (IOException e) {
            throw new PasteException(ERR_WRITE_STREAM, e);
        }
    }

    /**
     * Returns string of line-wise concatenated (tab-separated) Stdin arguments.
     * If only one Stdin arg is specified, echo back the Stdin.
     *
     * @param isSerial Paste one file at a time instead of in parallel
     * @param stdin    InputStream containing arguments from Stdin
     * @throws PasteException If fails to get stdin data
     */
    @Override
    public String mergeStdin(Boolean isSerial, InputStream stdin) throws PasteException {
        if (stdin == null) {
            throw new PasteException(ERR_NULL_STREAMS);
        }

        List<List<String>> output = new ArrayList<>();
        try {
            output.add(IOUtils.getLinesFromInputStream(stdin));
        } catch (IOException e) {
            throw new PasteException(ERR_IO_EXCEPTION, e);
        }

        return isSerial ? mergeInSerial(output) : mergeInParallel(output);
    }


    /**
     * Returns string of line-wise concatenated (tab-separated) files.
     * If only one file is specified, echo back the file content.
     *
     * @param isSerial Paste one file at a time instead of in parallel
     * @param fileName Array of file names to be read and merged (not including "-" for reading from stdin)
     * @throws PasteException
     */
    @Override
    public String mergeFile(Boolean isSerial, String... fileName) throws PasteException {
        if (fileName == null || fileName.length == 0) {
            throw new PasteException(ERR_NULL_ARGS);
        }

        List<List<String>> output = new ArrayList<>();
        for (String file : fileName) {
            File node = IOUtils.resolveFilePath(file).toFile();
            if (!node.exists()) {
                throw new PasteException(String.format("'%s': %s", node.getName(), ERR_FILE_NOT_FOUND));
            }
            if (node.isDirectory()) {
                // throw new PasteException(String.format("'%s': %s", node.getName(), ERR_IS_DIR));
                continue;
            }
            if (!node.canRead()) {
                throw new PasteException(String.format("'%s': %s", node.getName(), ERR_READING_FILE));
            }

            try (InputStream input = IOUtils.openInputStream(file)) {
                output.add(IOUtils.getLinesFromInputStream(input));
                IOUtils.closeInputStream(input);
            } catch (ShellException | IOException e) {
                throw new PasteException(e.getMessage(), e);
            }
        }

        return isSerial ? mergeInSerial(output) : mergeInParallel(output);
    }

    /**
     * Returns string of line-wise concatenated (tab-separated) files and Stdin arguments.
     *
     * @param isSerial Paste one file at a time instead of in parallel
     * @param stdin    InputStream containing arguments from Stdin
     * @param fileName Array of file names to be read and merged (including "-" for reading from stdin)
     * @throws PasteException
     */
    @Override
    public String mergeFileAndStdin(Boolean isSerial, InputStream stdin, String... fileName) throws PasteException {
        if (stdin == null) {
            throw new PasteException(ERR_NULL_STREAMS);
        }
        if (fileName == null || fileName.length == 0) {
            throw new PasteException(ERR_NULL_ARGS);
        }

        List<String> output = new ArrayList<>();
        for (String file : fileName) {
            if ("-".equals(file)) {
                output.add(mergeStdin(isSerial, stdin));
            } else {
                output.add(mergeFile(isSerial, file));
            }
        }

        List<List<String>> totalLines = output.stream().filter(s -> !s.isEmpty())
                .map(s -> Arrays.asList((s.split(STRING_NEWLINE))))
                .collect(Collectors.toList());

        String result = isSerial ? mergeInSerial(totalLines) : mergeInParallel(totalLines);
        return String.join(STRING_NEWLINE, result);
    }

    /**
     * Takes in a List of Lists of Strings and merges the lists in serial.
     * Each inner list represents data from a file, where each element represents a line of data.
     * Columns within a row are separated by a tab character ('\t'), and rows are separated by a newline character ('\n').
     *
     * @param listOfFiles List of Lists of Strings representing the data from multiple files to be merged
     * @return Merged data as a single String where columns within a row are separated by a
     * tab character ('\t') and rows are separated by a newline character ('\n')
     */
    public String mergeInSerial(List<List<String>> listOfFiles) {
        List<String> mergedLines = new ArrayList<>();
        for (List<String> files : listOfFiles) {
            mergedLines.add(String.join(STRING_TAB, files));
        }
        return String.join(STRING_NEWLINE, mergedLines);
    }

    /**
     * Merges lists in parallel, where each sublist corresponds to a column in the merged result.
     * If a sublist does not have an element at a particular index, an empty string is inserted.
     *
     * @param listOfFiles A List of Lists of Strings representing the data to be merged in parallel
     * @return A String representing the merged data with elements separated by tabs and rows separated by newlines
     */
    public String mergeInParallel(List<List<String>> listOfFiles) {
        int maxFileLength = listOfFiles.stream()
                .mapToInt(List::size)
                .max()
                .orElse(0);

        List<List<String>> parallelizedFiles = new ArrayList<>();
        for (int i = 0; i < maxFileLength; i++) {
            List<String> currIndexList = new ArrayList<>();
            for (List<String> file : listOfFiles) {
                currIndexList.add(i < file.size() ? file.get(i) : ""); // Empty string if the list do not have an element at index i
            }
            parallelizedFiles.add(currIndexList);
        }

        List<String> mergedLines = new ArrayList<>();
        for (List<String> files : parallelizedFiles) {
            mergedLines.add(String.join(STRING_TAB, files));
        }
        return String.join(STRING_NEWLINE, mergedLines);
    }
}
