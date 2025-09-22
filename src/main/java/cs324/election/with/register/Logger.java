package cs324.election.with.register;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Centralized logging utility for the LCR election system. Provides consistent,
 * configurable logging with ANSI colors.
 */
public class Logger {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String SEPARATOR_LINE = "─".repeat(50);

    // Configurable log levels
    public enum LogLevel {
        ERROR, WARN, INFO, DEBUG
    }

    private static LogLevel currentLevel = LogLevel.INFO; // Default: INFO (DEBUG for verbose)

    // Set log level (call once at startup if needed)
    public static void setLogLevel(LogLevel level) {
        currentLevel = level;
    }

    public static boolean shouldLog(LogLevel level) {
        return level.ordinal() <= currentLevel.ordinal();
    }

    // Core logging methods
    public static void error(String message) {
        if (shouldLog(LogLevel.ERROR)) {
            System.err.println(coloredError("[" + timestamp() + "] ❌ " + message));
        }
    }

    public static void warn(String message) {
        if (shouldLog(LogLevel.WARN)) {
            System.out.println(coloredWarn("[" + timestamp() + "] ⚠️  " + message));
        }
    }

    public static void info(String message) {
        if (shouldLog(LogLevel.INFO)) {
            System.out.println(coloredInfo("[" + timestamp() + "] " + message));
        }
    }

    public static void debug(String message) {
        if (shouldLog(LogLevel.DEBUG)) {
            System.out.println(coloredDebug("[" + timestamp() + "] 🔍 " + message));
        }
    }

    // Node-specific logging with ID prefix
    public static void nodeInfo(int nodeId, String message) {
        if (shouldLog(LogLevel.INFO)) {
            System.out.println(coloredInfo("[" + timestamp() + "] Node " + String.format("%03d", nodeId) + ": " + message));
        }
    }

    public static void nodeWarn(int nodeId, String message) {
        if (shouldLog(LogLevel.WARN)) {
            System.out.println(coloredWarn("[" + timestamp() + "] Node " + String.format("%03d", nodeId) + ": " + message));
        }
    }

    public static void nodeError(int nodeId, String message) {
        if (shouldLog(LogLevel.ERROR)) {
            System.err.println(coloredError("[" + timestamp() + "] Node " + String.format("%03d", nodeId) + ": " + message));
        }
    }

    public static void nodeDebug(int nodeId, String message) {
        if (shouldLog(LogLevel.DEBUG)) {
            System.out.println(coloredDebug("[" + timestamp() + "] Node " + String.format("%03d", nodeId) + ": " + message));
        }
    }

    // Election-specific logging with round/leader context
    public static void electionInfo(int nodeId, int round, String message) {
        if (shouldLog(LogLevel.INFO)) {
            System.out.println(coloredInfo("[" + timestamp() + "] Node " + String.format("%03d", nodeId)
                    + " [R" + round + "]: " + message));
        }
    }

    // Utility methods
    public static String timestamp() {
        return SDF.format(new Date());
    }

    public static String separator() {
        return SEPARATOR_LINE;
    }

    public static String separator(int length) {
        return "─".repeat(Math.max(20, Math.min(80, length)));
    }

    // ANSI color helpers
    private static String coloredInfo(String message) {
        return "\033[1;32m" + message + "\033[0m"; // Bold green
    }

    private static String coloredWarn(String message) {
        return "\033[1;33m" + message + "\033[0m"; // Bold yellow
    }

    private static String coloredError(String message) {
        return "\033[1;31m" + message + "\033[0m"; // Bold red
    }

    private static String coloredDebug(String message) {
        return "\033[1;36m" + message + "\033[0m"; // Bold cyan
    }

    // Banner methods for startup/status
    public static void printBanner(String title) {
        System.out.println("\n" + separator());
        System.out.println(coloredInfo("[" + timestamp() + "] " + title));
        System.out.println(separator() + "\n");
    }

    public static void printStatusHeader(String title) {
        System.out.println("\n" + separator());
        System.out.println(coloredInfo("[" + timestamp() + "] 📊 " + title));
        System.out.println(separator());
    }

    public static void printStatusFooter() {
        System.out.println(separator() + "\n");
    }
}
