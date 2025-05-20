package com.seristic.badges.util.helpers;

import net.kyori.adventure.text.Component;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class PluginLogger {
    private static final Logger logger = Logger.getLogger("Badges");
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String LOG_FILE_PATH = "plugins/Badges/logs/badges.log";
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PluginLogger.class);

    static {
        Logger rootLogger = Logger.getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new BadgeLogFormatter());
        consoleHandler.setLevel(Level.ALL);
        logger.addHandler(consoleHandler);

        File logDir = new File("plugins/Badges/logs");
        if (!logDir.exists()) logDir.mkdirs();

        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
    }
    /**
     * Logs a message at INFO level.
     */
    public static void info(String message) {
        logger.info(message);
    }

    /**
     * Logs a warning message.
     */
    public static void warning(String message) {
        logger.warning(message);
    }

    /**
     * Logs a severe error message.
     */
    public static void severe(String message) {
        logger.severe(message);
    }
    /**
     * Logs an exception with detailed formatting.
     * Includes thread, timestamp, class, method, and nicely formatted stack trace.
     */
    public static void logException(String contextMessage, Throwable throwable) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n=== Exception Occurred ===\n");
        sb.append("Time: ").append(TIMESTAMP_FORMAT.format(new Date())).append("\n");
        sb.append("Thread:").append(Thread.currentThread().getName()).append("\n");

        StackTraceElement origin = throwable.getStackTrace()[0];
        sb.append("Origin: ").append(origin.getClass()).append(".").append(origin.getMethodName())
                .append(" (").append(origin.getFileName()).append(":").append(origin.getLineNumber()).append(")\n");

        if (contextMessage != null && !contextMessage.isBlank()) {
            sb.append("Context: ").append(contextMessage).append("\n");
        }

        sb.append("Exception: ").append(throwable.getClass().getName())
                .append(": ").append(throwable.getMessage()).append("\n");

        for (StackTraceElement element : throwable.getStackTrace()) {
            String className = element.getClassName();
            if (className.startsWith("java.lang.reflect") || className.startsWith("sun.reflect")) {
                continue;
            }
            sb.append("    at ").append(element.toString()).append("\n");
        }

        logger.severe(sb.toString());

        Throwable cause = throwable.getCause();
        while (cause != null) {
            logger.severe(formatCause(cause));
            cause = cause.getCause();
        }
    }

    private static String formatCause(Throwable cause) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- Caused by: ").append(cause.getClass().getName());
        if (cause.getMessage() != null) {
            sb.append(": ").append(cause.getMessage());
        }
        sb.append("\n");

        for (StackTraceElement element : cause.getStackTrace()) {
            sb.append("    at ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
    private static class BadgeLogFormatter extends Formatter {
        private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

        @Override
        public String format(LogRecord record) {
            StringBuilder builder = new StringBuilder();

            builder.append("[")
                    .append(dateFormat.format(new Date(record.getMillis())))
                    .append("] ")
                    .append("[")
                    .append(record.getLevel().getName())
                    .append("] ");

            if (record.getLoggerName() != null) {
                builder.append("[")
                        .append(record.getLoggerName())
                        .append("] ");
            }

            builder.append(formatMessage(record))
                    .append("\n");

            if (record.getThrown() != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                builder.append(sw.toString());
            }
            return builder.toString();
        }
    }
    public static void componentInfo(String prefix, Component component) {
        StringBuilder readableFormat = new StringBuilder();
        readableFormat.append("[").append(prefix).append("] ");
        
        if (component instanceof net.kyori.adventure.text.TextComponent) {
            extractComponentInfo(component, readableFormat);
        }
        
        logger.info(readableFormat.toString());
    }

    private static void extractComponentInfo(Component component, StringBuilder builder) {
        if (component instanceof net.kyori.adventure.text.TextComponent textComponent) {
            String content = textComponent.content();
            if (!content.isEmpty()) {
                builder.append(content);
            }
            
            if (!textComponent.children().isEmpty()) {
                for (Component child : textComponent.children()) {
                    extractComponentInfo(child, builder);
                }
            }
        }
    }
}