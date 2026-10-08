package cn.org.autumn.modules.sys.log;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class LogFileServiceTest {

    private LogFileService service;
    private Path tempRoot;

    @BeforeEach
    void setUp() throws Exception {
        tempRoot = Files.createTempDirectory("autumn-logfile-test");
        service = new LogFileService();
        MockEnvironment env = new MockEnvironment();
        env.setProperty("logging.file.path", tempRoot.toString() + File.separator);
        env.setProperty("logging.file.name", "file.log");
        service.setEnvironment(env);
        service.setLogRootOverride(tempRoot);
        Files.createDirectories(tempRoot.resolve("daily"));
    }

    @AfterEach
    void tearDown() {
        if (tempRoot != null) {
            deleteRecursive(tempRoot.toFile());
        }
    }

    @Test
    void parseDate_defaultToday() {
        LocalDate d = service.parseDate(null);
        assertEquals(LocalDate.now(), d);
    }

    @Test
    void parseDate_invalid() {
        assertThrows(IllegalArgumentException.class, () -> service.parseDate("2026/10/08"));
    }

    @Test
    void clampLines() {
        assertEquals(LogFileService.DEFAULT_LINES, service.clampLines(null));
        assertEquals(LogFileService.DEFAULT_LINES, service.clampLines(0));
        assertEquals(100, service.clampLines(100));
        assertEquals(LogFileService.MAX_LINES, service.clampLines(99999));
    }

    @Test
    void resolveLogFile_plainAndMissing() throws Exception {
        LocalDate day = LocalDate.of(2026, 10, 7);
        File missing = service.resolveLogFile(day);
        assertFalse(missing.isFile());
        assertTrue(missing.getName().endsWith("file.log.2026-10-07"));

        writePlain(day, "line1\nline2\n");
        File found = service.resolveLogFile(day);
        assertTrue(found.isFile());
        assertFalse(found.getName().endsWith(".gz"));
    }

    @Test
    void resolveLogFile_prefersPlainOverGz() throws Exception {
        LocalDate day = LocalDate.of(2026, 10, 6);
        writePlain(day, "plain\n");
        writeGzip(day, "gzip-only\n");
        File found = service.resolveLogFile(day);
        assertFalse(found.getName().endsWith(".gz"));
        assertTrue(new String(Files.readAllBytes(found.toPath()), StandardCharsets.UTF_8).contains("plain"));
    }

    @Test
    void resolveLogFile_fallsBackToGz() throws Exception {
        LocalDate day = LocalDate.of(2026, 10, 5);
        writeGzip(day, "only-gz\n");
        File found = service.resolveLogFile(day);
        assertTrue(found.getName().endsWith(".gz"));
    }

    @Test
    void readTail_plainLastN() throws Exception {
        LocalDate day = LocalDate.of(2026, 10, 4);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 10; i++) {
            sb.append("L").append(i).append('\n');
        }
        writePlain(day, sb.toString());
        LogFileViewResult view = service.view(day, 3, null, 0);
        assertTrue(view.isExists());
        assertEquals(3, view.getLines().size());
        assertEquals("L8", view.getLines().get(0));
        assertEquals("L9", view.getLines().get(1));
        assertEquals("L10", view.getLines().get(2));
        assertFalse(view.isTruncated());
    }

    @Test
    void readTail_keywordFilter() throws Exception {
        LocalDate day = LocalDate.of(2026, 10, 3);
        writePlain(day, "aaa\nbbb ERROR\nccc\nERROR again\n");
        LogFileViewResult view = service.view(day, 100, "ERROR", 0);
        assertEquals(2, view.getLines().size());
        assertTrue(view.getLines().get(0).contains("ERROR"));
        assertTrue(view.getLines().get(1).contains("ERROR"));
    }

    @Test
    void readTail_maxLineLengthZero_noTruncate() throws Exception {
        LocalDate day = LocalDate.of(2026, 10, 2);
        String longLine = "x".repeat(5000);
        writePlain(day, longLine + "\n");
        LogFileViewResult view = service.view(day, 10, null, 0);
        assertEquals(1, view.getLines().size());
        assertEquals(5000, view.getLines().get(0).length());
        assertFalse(view.isTruncated());
    }

    @Test
    void readTail_maxLineLengthTruncates() throws Exception {
        LocalDate day = LocalDate.of(2026, 10, 1);
        writePlain(day, "abcdefghij\n");
        LogFileViewResult view = service.view(day, 10, null, 5);
        assertEquals(1, view.getLines().size());
        assertEquals("abcde...[truncated]", view.getLines().get(0));
        assertTrue(view.isTruncated());
    }

    @Test
    void readTail_gzip() throws Exception {
        LocalDate day = LocalDate.of(2026, 9, 30);
        writeGzip(day, "g1\ng2\ng3\n");
        LogFileViewResult view = service.view(day, 2, null, 0);
        assertTrue(view.isGzip());
        assertEquals(2, view.getLines().size());
        assertEquals("g2", view.getLines().get(0));
        assertEquals("g3", view.getLines().get(1));
    }

    @Test
    void clearDay_truncatePlain() throws Exception {
        LocalDate day = LocalDate.of(2026, 9, 29);
        writePlain(day, "to-clear\n");
        File f = service.resolveLogFile(day);
        assertTrue(f.length() > 0);
        String status = service.clearDay(day);
        assertEquals("cleared", status);
        assertTrue(f.isFile());
        assertEquals(0, f.length());
        Files.write(f.toPath(), "after\n".getBytes(StandardCharsets.UTF_8));
        LogFileViewResult view = service.view(day, 10, null, 0);
        assertEquals(1, view.getLines().size());
        assertEquals("after", view.getLines().get(0));
    }

    @Test
    void clearDay_deleteGz() throws Exception {
        LocalDate day = LocalDate.of(2026, 9, 28);
        writeGzip(day, "gz\n");
        File f = service.resolveLogFile(day);
        assertTrue(f.isFile());
        assertEquals("cleared", service.clearDay(day));
        assertFalse(f.isFile());
    }

    @Test
    void clearDay_idempotentEmpty() throws Exception {
        LocalDate day = LocalDate.of(2026, 9, 27);
        assertEquals("empty", service.clearDay(day));
    }

    @Test
    void listAvailableDates() throws Exception {
        writePlain(LocalDate.of(2026, 9, 20), "a\n");
        writeGzip(LocalDate.of(2026, 9, 21), "b\n");
        List<String> dates = service.listAvailableDates();
        assertTrue(dates.contains("2026-09-20"));
        assertTrue(dates.contains("2026-09-21"));
        assertTrue(dates.indexOf("2026-09-21") < dates.indexOf("2026-09-20"));
    }

    @Test
    void ensureUnderRoot_rejectsEscape() {
        assertThrows(SecurityException.class, () -> {
            Path root = service.resolveLogRoot();
            service.ensureUnderRoot(root, tempRoot.getParent().resolve("outside.txt"));
        });
    }

    @Test
    void applyFilters_static() {
        assertNull(LogFileService.applyFilters("hello", "nope", 0));
        assertEquals("hello", LogFileService.applyFilters("hello", "ell", 0));
        assertEquals("hel...[truncated]", LogFileService.applyFilters("hello", null, 3));
    }

    private void writePlain(LocalDate day, String content) throws Exception {
        Path file = tempRoot.resolve("daily").resolve("file.log." + day);
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    private void writeGzip(LocalDate day, String content) throws Exception {
        Path file = tempRoot.resolve("daily").resolve("file.log." + day + ".gz");
        try (GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(file.toFile()));
             OutputStreamWriter w = new OutputStreamWriter(gos, StandardCharsets.UTF_8)) {
            w.write(content);
        }
    }

    private static void deleteRecursive(File f) {
        if (f == null || !f.exists()) {
            return;
        }
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) {
                    deleteRecursive(c);
                }
            }
        }
        f.delete();
    }
}
