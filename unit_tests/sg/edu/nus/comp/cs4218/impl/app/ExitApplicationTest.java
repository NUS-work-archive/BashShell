package sg.edu.nus.comp.cs4218.impl.app;

import static com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExitApplicationTest {

    private ExitApplication app;

    @BeforeEach
    public void renewApplication() {
        app = new ExitApplication();
    }

    @Test
    void run_NoArgs_ExitCodeZero() {
        int exitCode = assertDoesNotThrow(() ->
                catchSystemExit(() -> app.terminateExecution())
        );
        assertEquals(0, exitCode);
    }
}
