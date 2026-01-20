package com.batchweaver.core.fileprocess.reader;

import java.util.regex.Pattern;

/**
 * Detects whether a line should be treated as a footer line.
 * <p>
 * Implementations can plug in custom logic for different footer formats.
 */
@FunctionalInterface
public interface FooterLineDetector {

    /**
     * Returns {@code true} if the line represents a footer.
     *
     * @param line raw line content
     * @return true when the line is a footer
     */
    boolean isFooterLine(String line);

    /**
     * Default detector implementation for common footer formats:
     * <ul>
     *   <li>pure digits (e.g. "3")</li>
     *   <li>"T|" prefix (e.g. "T|3")</li>
     *   <li>"R" prefix with digits (e.g. "R00003")</li>
     * </ul>
     *
     * @return default detector
     */
    static FooterLineDetector defaultDetector() {
        return new DefaultFooterLineDetector();
    }

    /**
     * Default footer detector with common numeric patterns.
     */
    final class DefaultFooterLineDetector implements FooterLineDetector {
        private static final Pattern PURE_NUMBER = Pattern.compile("^\\d+$");
        private static final Pattern T_PREFIX = Pattern.compile("^[Tt]\\|\\d+$");
        private static final Pattern R_PREFIX = Pattern.compile("^[Rr]\\d+$");

        @Override
        public boolean isFooterLine(String line) {
            if (line == null) {
                return false;
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                return false;
            }
            return PURE_NUMBER.matcher(trimmed).matches()
                || T_PREFIX.matcher(trimmed).matches()
                || R_PREFIX.matcher(trimmed).matches();
        }
    }
}
