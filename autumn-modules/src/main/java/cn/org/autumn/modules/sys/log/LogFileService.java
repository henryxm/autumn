package cn.org.autumn.modules.sys.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.RollingPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * 读取 / 清空 Logback FILE appender 按日落盘的应用日志。
 */
@Slf4j
@Service
public class LogFileService {

    public static final int DEFAULT_LINES = 500;
    public static final int MAX_LINES = 5000;
    public static final String FILE_APPENDER_NAME = "FILE";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Pattern DATE_IN_NAME = Pattern.compile("\\.(\\d{4}-\\d{2}-\\d{2})(?:\\.gz)?$");
    private static final String TRUNCATE_MARK = "...[truncated]";

    @Autowired
    private Environment environment;

    /** 单测可注入固定日志根目录，覆盖 Environment / CWD。 */
    private Path logRootOverride;

    void setLogRootOverride(Path logRootOverride) {
        this.logRootOverride = logRootOverride;
    }

    void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public LocalDate parseDate(String date) {
        if (StringUtils.isBlank(date)) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(date.trim(), DAY);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期格式须为 yyyy-MM-dd");
        }
    }

    public int clampLines(Integer lines) {
        if (lines == null || lines <= 0) {
            return DEFAULT_LINES;
        }
        return Math.min(lines, MAX_LINES);
    }

    public Path resolveLogRoot() throws IOException {
        if (logRootOverride != null) {
            return logRootOverride.toAbsolutePath().normalize();
        }
        String logPath = firstNonBlank(
                System.getProperty("LOG_PATH"),
                environment != null ? environment.getProperty("LOG_PATH") : null,
                environment != null ? environment.getProperty("logging.file.path") : null,
                "logs/");
        Path root = Paths.get(ensureTrailingSeparator(logPath)).toAbsolutePath().normalize();
        return root;
    }

    public String resolveLogFileBaseName() {
        String name = firstNonBlank(
                System.getProperty("LOG_FILE"),
                environment != null ? environment.getProperty("LOG_FILE") : null,
                environment != null ? environment.getProperty("logging.file.name") : null,
                "file.log");
        Path asPath = Paths.get(name);
        Path fileName = asPath.getFileName();
        return fileName != null ? fileName.toString() : "file.log";
    }

    /**
     * 解析指定日期的日志文件：优先明文，其次 .gz；今日优先 Logback 活动文件。
     */
    public File resolveLogFile(LocalDate date) throws IOException {
        if (date == null) {
            date = LocalDate.now();
        }
        Path root = resolveLogRoot();
        Path dailyDir = root.resolve("daily").normalize();
        ensureUnderRoot(root, dailyDir);

        if (date.equals(LocalDate.now())) {
            File active = resolveActiveFileFromLogback();
            if (active != null && active.isFile()) {
                ensureUnderRoot(root, active.toPath());
                return active;
            }
        }

        String baseName = resolveLogFileBaseName();
        String day = date.format(DAY);
        File plain = dailyDir.resolve(baseName + "." + day).toFile();
        if (plain.isFile()) {
            ensureUnderRoot(root, plain.toPath());
            return plain;
        }
        File gz = dailyDir.resolve(baseName + "." + day + ".gz").toFile();
        if (gz.isFile()) {
            ensureUnderRoot(root, gz.toPath());
            return gz;
        }
        return plain;
    }

    public LogFileViewResult view(LocalDate date, int lines, String keyword, int maxLineLength) throws IOException {
        LocalDate day = date != null ? date : LocalDate.now();
        int limit = clampLines(lines);
        File file = resolveLogFile(day);
        LogFileViewResult result = new LogFileViewResult();
        result.setDate(day.format(DAY));
        result.setPath(toDisplayPath(file));
        result.setGzip(file.getName().endsWith(".gz"));
        if (!file.isFile()) {
            result.setExists(false);
            result.setSize(0);
            result.setLines(Collections.emptyList());
            return result;
        }
        result.setExists(true);
        result.setSize(file.length());
        TailRead tail = readTailInternal(file, limit, keyword, maxLineLength);
        result.setLines(tail.lines);
        result.setTruncated(tail.lineContentTruncated);
        return result;
    }

    public List<String> listAvailableDates() throws IOException {
        Path root = resolveLogRoot();
        Path dailyDir = root.resolve("daily").normalize();
        ensureUnderRoot(root, dailyDir);
        if (!Files.isDirectory(dailyDir)) {
            return Collections.emptyList();
        }
        String baseName = resolveLogFileBaseName();
        List<String> dates = new ArrayList<>();
        try (var stream = Files.list(dailyDir)) {
            stream.filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.startsWith(baseName + "."))
                    .forEach(name -> {
                        Matcher m = DATE_IN_NAME.matcher(name.substring(baseName.length()));
                        if (m.find()) {
                            String d = m.group(1);
                            if (!dates.contains(d)) {
                                dates.add(d);
                            }
                        }
                    });
        }
        dates.sort(Collections.reverseOrder());
        return dates;
    }

    /**
     * 清空指定日日志：活动明文 truncate(0)；仅 .gz 则删除；均不存在视为已空。
     */
    public String clearDay(LocalDate date) throws IOException {
        if (date == null) {
            throw new IllegalArgumentException("date 不能为空");
        }
        Path root = resolveLogRoot();
        Path dailyDir = root.resolve("daily").normalize();
        ensureUnderRoot(root, dailyDir);
        String baseName = resolveLogFileBaseName();
        String day = date.format(DAY);
        File plain = dailyDir.resolve(baseName + "." + day).toFile();
        File gz = dailyDir.resolve(baseName + "." + day + ".gz").toFile();

        File active = null;
        if (date.equals(LocalDate.now())) {
            active = resolveActiveFileFromLogback();
        }

        boolean touched = false;
        if (active != null && active.isFile()) {
            ensureUnderRoot(root, active.toPath());
            truncateFile(active);
            touched = true;
        } else if (plain.isFile()) {
            ensureUnderRoot(root, plain.toPath());
            truncateFile(plain);
            touched = true;
        }

        if (gz.isFile()) {
            ensureUnderRoot(root, gz.toPath());
            if (!Files.deleteIfExists(gz.toPath())) {
                throw new IOException("删除压缩日志失败: " + gz.getAbsolutePath());
            }
            touched = true;
        }

        return touched ? "cleared" : "empty";
    }

    TailRead readTailInternal(File file, int lines, String keyword, int maxLineLength) throws IOException {
        if (file.getName().endsWith(".gz")) {
            return readTailGzip(file, lines, keyword, maxLineLength);
        }
        return readTailPlain(file, lines, keyword, maxLineLength);
    }

    TailRead readTailPlain(File file, int lines, String keyword, int maxLineLength) throws IOException {
        Deque<String> deque = new ArrayDeque<>(Math.min(lines, 1024));
        boolean contentTruncated = false;
        if (file.length() == 0) {
            return new TailRead(Collections.emptyList(), false);
        }
        try (ReversedLinesFileReader reader = ReversedLinesFileReader.builder()
                .setFile(file)
                .setCharset(StandardCharsets.UTF_8)
                .get()) {
            String raw;
            while (deque.size() < lines && (raw = reader.readLine()) != null) {
                String applied = applyFilters(raw, keyword, maxLineLength);
                if (applied == null) {
                    continue;
                }
                if (maxLineLength > 0 && raw.length() > maxLineLength) {
                    contentTruncated = true;
                }
                deque.addFirst(applied);
            }
        }
        return new TailRead(new ArrayList<>(deque), contentTruncated);
    }

    TailRead readTailGzip(File file, int lines, String keyword, int maxLineLength) throws IOException {
        Deque<String> deque = new ArrayDeque<>(Math.min(lines, 1024));
        boolean contentTruncated = false;
        try (GZIPInputStream gis = new GZIPInputStream(Files.newInputStream(file.toPath()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8))) {
            String raw;
            while ((raw = reader.readLine()) != null) {
                String applied = applyFilters(raw, keyword, maxLineLength);
                if (applied == null) {
                    continue;
                }
                if (maxLineLength > 0 && raw.length() > maxLineLength) {
                    contentTruncated = true;
                }
                if (deque.size() == lines) {
                    deque.removeFirst();
                }
                deque.addLast(applied);
            }
        }
        return new TailRead(new ArrayList<>(deque), contentTruncated);
    }

    static String applyFilters(String raw, String keyword, int maxLineLength) {
        if (raw == null) {
            return null;
        }
        if (StringUtils.isNotBlank(keyword) && !raw.contains(keyword)) {
            return null;
        }
        if (maxLineLength > 0 && raw.length() > maxLineLength) {
            return raw.substring(0, maxLineLength) + TRUNCATE_MARK;
        }
        return raw;
    }

    static void truncateFile(File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(0);
        }
    }

    File resolveActiveFileFromLogback() {
        try {
            LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger root = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
            Appender<ILoggingEvent> appender = root.getAppender(FILE_APPENDER_NAME);
            if (!(appender instanceof RollingFileAppender)) {
                return null;
            }
            RollingFileAppender<ILoggingEvent> rfa = (RollingFileAppender<ILoggingEvent>) appender;
            RollingPolicy policy = rfa.getRollingPolicy();
            String activeName = null;
            if (policy instanceof TimeBasedRollingPolicy) {
                activeName = ((TimeBasedRollingPolicy<ILoggingEvent>) policy).getActiveFileName();
            }
            if (StringUtils.isBlank(activeName)) {
                activeName = rfa.getFile();
            }
            if (StringUtils.isBlank(activeName)) {
                return null;
            }
            return new File(activeName);
        } catch (Exception e) {
            log.debug("解析 Logback 活动日志文件失败: {}", e.getMessage());
            return null;
        }
    }

    void ensureUnderRoot(Path root, Path candidate) throws IOException {
        Path rootCanon = root.toAbsolutePath().normalize();
        if (!Files.exists(rootCanon)) {
            Files.createDirectories(rootCanon);
        }
        Path rootReal = rootCanon.toRealPath();
        Path candAbs = candidate.toAbsolutePath().normalize();
        Path candCheck = Files.exists(candAbs) ? candAbs.toRealPath() : candAbs;
        if (!candCheck.startsWith(rootReal)) {
            throw new SecurityException("日志路径越界: " + candidate);
        }
    }

    private String toDisplayPath(File file) {
        try {
            Path root = resolveLogRoot();
            Path abs = file.toPath().toAbsolutePath().normalize();
            if (abs.startsWith(root)) {
                return root.relativize(abs).toString().replace('\\', '/');
            }
            return abs.toString();
        } catch (Exception e) {
            return file.getPath();
        }
    }

    private static String ensureTrailingSeparator(String path) {
        if (StringUtils.isBlank(path)) {
            return "logs" + File.separator;
        }
        String p = path.trim();
        if (!p.endsWith("/") && !p.endsWith("\\")) {
            p = p + File.separator;
        }
        return p;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (StringUtils.isNotBlank(v)) {
                return v.trim();
            }
        }
        return null;
    }

    static final class TailRead {
        final List<String> lines;
        final boolean lineContentTruncated;

        TailRead(List<String> lines, boolean lineContentTruncated) {
            this.lines = lines;
            this.lineContentTruncated = lineContentTruncated;
        }
    }
}
