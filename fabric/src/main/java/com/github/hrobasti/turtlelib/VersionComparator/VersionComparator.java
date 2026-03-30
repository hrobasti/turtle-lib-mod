package com.github.hrobasti.turtlelib.VersionComparator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Compares semantic versions with support for alpha, beta, rc and dot or dash pre-release delimiters.
 */
public final class VersionComparator {
    private VersionComparator() {
    }

    public static int compare(String left, String right) {
        Version l = Version.parse(left);
        Version r = Version.parse(right);
        return l.compareTo(r);
    }

    public static boolean isGreater(String left, String right) {
        return compare(left, right) > 0;
    }

    static final class Version implements Comparable<Version> {
        private final List<Integer> numbers;
        private final String letterSuffix;
        private final PreRelease preRelease;

        private Version(List<Integer> numbers, String letterSuffix, PreRelease preRelease) {
            this.numbers = numbers;
            this.letterSuffix = letterSuffix;
            this.preRelease = preRelease;
        }

        static Version parse(String rawInput) {
            String raw = rawInput == null ? "" : rawInput.trim().toLowerCase(Locale.ROOT);
            if (raw.isEmpty()) {
                return new Version(List.of(0), null, null);
            }

            String main = raw;
            String pre = null;

            int dashIndex = raw.indexOf('-');
            if (dashIndex >= 0) {
                main = raw.substring(0, dashIndex);
                pre = raw.substring(dashIndex + 1);
            } else {
                int dotQualifierIndex = findDotQualifier(raw);
                if (dotQualifierIndex >= 0) {
                    main = raw.substring(0, dotQualifierIndex);
                    pre = raw.substring(dotQualifierIndex + 1);
                }
            }

            List<Integer> nums = new ArrayList<>();
            String parsedLetterSuffix = null;
            for (String segment : main.split("\\.")) {
                if (segment.isBlank()) {
                    nums.add(0);
                    continue;
                }
                int i = 0;
                while (i < segment.length() && Character.isDigit(segment.charAt(i))) {
                    i++;
                }
                String numberPart = i == 0 ? "0" : segment.substring(0, i);
                nums.add(parseIntSafe(numberPart));

                if (i < segment.length()) {
                    String suffix = segment.substring(i);
                    if (pre == null && (suffix.startsWith("alpha") || suffix.startsWith("beta") || suffix.startsWith("rc"))) {
                        pre = suffix;
                    } else if (allLetters(suffix)) {
                        parsedLetterSuffix = suffix;
                    }
                }
            }

            if (nums.isEmpty()) {
                nums.add(0);
            }

            return new Version(nums, parsedLetterSuffix, PreRelease.parse(pre));
        }

        private static boolean allLetters(String value) {
            for (int i = 0; i < value.length(); i++) {
                if (!Character.isLetter(value.charAt(i))) {
                    return false;
                }
            }
            return !value.isEmpty();
        }

        private static int findDotQualifier(String raw) {
            String[] tokens = raw.split("\\.");
            if (tokens.length <= 2) {
                return -1;
            }

            int cursor = 0;
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                if (i >= 2 && (token.startsWith("alpha") || token.startsWith("beta") || token.startsWith("rc"))) {
                    return cursor - 1;
                }
                cursor += token.length() + 1;
            }
            return -1;
        }

        private static int parseIntSafe(String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }

        @Override
        public int compareTo(Version other) {
            int max = Math.max(numbers.size(), other.numbers.size());
            for (int i = 0; i < max; i++) {
                int left = i < numbers.size() ? numbers.get(i) : 0;
                int right = i < other.numbers.size() ? other.numbers.get(i) : 0;
                if (left != right) {
                    return Integer.compare(left, right);
                }
            }

            boolean leftHasSuffix = letterSuffix != null && !letterSuffix.isEmpty();
            boolean rightHasSuffix = other.letterSuffix != null && !other.letterSuffix.isEmpty();
            if (leftHasSuffix && !rightHasSuffix) {
                return 1;
            }
            if (!leftHasSuffix && rightHasSuffix) {
                return -1;
            }
            if (leftHasSuffix) {
                int suffixCmp = letterSuffix.compareTo(other.letterSuffix);
                if (suffixCmp != 0) {
                    return suffixCmp;
                }
            }

            if (preRelease == null && other.preRelease != null) {
                return 1;
            }
            if (preRelease != null && other.preRelease == null) {
                return -1;
            }
            if (preRelease == null) {
                return 0;
            }
            return preRelease.compareTo(other.preRelease);
        }
    }

    static final class PreRelease implements Comparable<PreRelease> {
        private final String label;
        private final int number;

        private PreRelease(String label, int number) {
            this.label = label;
            this.number = number;
        }

        static PreRelease parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            String value = raw.trim().toLowerCase(Locale.ROOT);

            int i = 0;
            while (i < value.length() && Character.isLetter(value.charAt(i))) {
                i++;
            }

            String parsedLabel = i == 0 ? value : value.substring(0, i);
            int parsedNumber = 0;

            if (i < value.length()) {
                String numberPart = value.substring(i);
                try {
                    parsedNumber = Integer.parseInt(numberPart);
                } catch (NumberFormatException ignored) {
                    parsedNumber = 0;
                }
            }

            return new PreRelease(parsedLabel, parsedNumber);
        }

        @Override
        public int compareTo(PreRelease other) {
            if (Objects.equals(label, other.label)) {
                return Integer.compare(number, other.number);
            }

            int leftRank = rank(label);
            int rightRank = rank(other.label);
            if (leftRank != rightRank) {
                return Integer.compare(leftRank, rightRank);
            }

            return label.compareTo(other.label);
        }

        private static int rank(String value) {
            return switch (value) {
                case "alpha" -> 0;
                case "beta" -> 1;
                case "rc" -> 2;
                default -> -1;
            };
        }
    }
}

