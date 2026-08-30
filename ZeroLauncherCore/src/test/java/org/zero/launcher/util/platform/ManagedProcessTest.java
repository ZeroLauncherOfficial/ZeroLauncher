package org.zero.launcher.util.platform;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public final class ManagedProcessTest {

    @Test
    public void testBoundedLines() throws Exception {
        ProcessBuilder pb;
        if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS) {
            pb = new ProcessBuilder("cmd.exe", "/c", "echo test");
        } else {
            pb = new ProcessBuilder("echo", "test");
        }
        ManagedProcess process = new ManagedProcess(pb);
        for (int i = 0; i < ManagedProcess.MAX_LOG_LINES + 500; i++) {
            process.addLine("line-" + i);
        }

        List<String> lines = process.getLines(null);
        assertEquals(ManagedProcess.MAX_LOG_LINES, lines.size());
        assertEquals("line-500", lines.get(0));
        assertEquals("line-" + (ManagedProcess.MAX_LOG_LINES + 499), lines.get(lines.size() - 1));
    }
}
