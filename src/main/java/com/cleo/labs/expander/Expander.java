package com.cleo.labs.expander;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Expander {

    /**
     * For date formatting, use {@code date(format)[timezone]} where {@code format} is a
     * {@link SimpleDateFormat}. Since date formats may contain arbitrary text,
     * any closing parenthesis {@code )} in the format must be escaped with a
     * prefix backslash, which will be removed before the format is applied.
     * The {@code [timezone]} is optional and defaults to the local time zone.
     * The {@code format} is also optional and defaults to the ISO8601 format.
     * {@code [format]} can be specified only if a {@code (format)} is supplied,
     * although the {@code format} may be empty (i.e. the parentheses are required).
     * The following are valid:
     * <ul><li>{@code date}&mdash;formats a date parameter in ISO8601 format</li>
     *     <li>{@code date()}&mdash;formats a date parameter in ISO8601 format</li>
     *     <li>{@code date(format)}&mdash;formats a date parameter in the specified format</li>
     *     <li>{@code date(format)[GMT]}&mdash;formats a date parameter in the specified format as a GM time</li>
     *     <li>{@code now(format)[GMT]}&mdash;formats the current time in the specified format as a GM time</li>
     *     <li>{@code now}&mdash;formats the current time in ISO8601 format</li>
     * </ul>
     */
    private static final String DATE_OPTION = "(?<tag>date|now)(?:\\((?<format>(?:[^\\)\\\\]|\\\\.)*)\\)(?:\\[(?<zone>\\w+)\\])?)?";
    /**
     * A precompiled {@link Pattern} to match the {@code DATE_OPTION}.
     */
    private static final Pattern DATE_PATTERN = Pattern.compile(DATE_OPTION, Pattern.CASE_INSENSITIVE);
    /**
     * For a format option, you can use %format.
     * 
     * @see {@link Formatter}
     */
    private static final String FORMAT_OPTION = "%[-#\\+ 0,\\(]?\\d*(?:\\.\\d+)?[bBhHsScCdoxXeEfgGaA]";
    /**
     * For substring operations you can have:
     * <ul>
     * <li>{@code [start]} to get substring from {@code start} (inclusive,
     * starting at 0) to end of string</li>
     * <li>{@code [start,end]} to get substring from {@code start} to
     * {@code end} (exclusive)</li>
     * <li>{@code [start:length]} to get substring from {@code start} for
     * {@code length}</li>
     * <li>{@code [/pattern/]} to get substring matching {@code pattern}</li>
     * <li>{@code [/pattern/group]} to get capture group {@code group} (numeric
     * or string) from substring matching {@code pattern}</li>
     * </ul>
     * Unlike Java {@link String#substring(int, int)}, any out-of-bounds
     * scenarios are adjusted to fit within bounds, possibly returning an empty
     * string.
     * <p/>
     * You may use {} or {index} to use a replacement parameter for any of the
     * {@code start}, {@code end}, or {@code length} tokens.
     */
    private static final String SUBSTR_OPTION = "\\[(?:(\\d+|\\{\\d*\\})(?:([:,])(-?\\d+|\\{\\d*\\}))?|/((?:[^/\\\\]|\\\\.)*)/([^\\]]*))\\]";
    /**
     * A precompiled {@link Pattern} to parse {@code INDEX_OPTION} into five
     * capture groups:
     * <ol>
     * <li>the {@code start} index</li>
     * <li>the separator {@code ","}, {@code ":"}, or {@code null}</li>
     * <li>the {@code end} or {@code length}</li>
     * <li>the {@code pattern} if the match option is used</li>
     * <li>the {@code group} if the match option is used</li>
     * </ol>
     */
    private static final Pattern SUBSTR_OPTION_PATTERN = Pattern.compile(SUBSTR_OPTION);
    /**
     * The options you can include inside the replacement token braces {}:
     * <ul>
     * <li>{@code trim} to trim leading and trailing whitespace</li>
     * <li>{@code lower} or {@code tolower} to convert to lower case</li>
     * <li>{@code upper} or {@code toupper} to convert to upper case</li>
     * <li>{@code [substring expression]} to select a substring (see
     * {@link #SUBSTR_OPTION})</li>
     * <li>{@code %format} to use a {@link Formatter} format (on parameter of an
     * appropriate type)</li>
     * <li>{@code date(format)} to use a {@link SimpleDateFormat} format (on a
     * {@link Date} parameter)</li>
     * <li>{@code number} to specify a specific parameter (starting from 1)
     * instead of just using the "next one"</li>
     * <li>{@code now} to use the current date/time (as a {@link Date}) instead
     * of using one of the parameters</li>
     * </ul>
     * Options are case-insensitive.
     */
    private static final String OPTION = "(?:\\d+|trim|(?:to)?(?:lower|upper)|" + SUBSTR_OPTION + "|" + FORMAT_OPTION + "|" + DATE_OPTION + ")";
    /**
     * A precompiled {@link Pattern} to parse the option string (in between
     * braces {}).
     */
    private static final Pattern OPTION_PATTERN = Pattern.compile(OPTION, Pattern.CASE_INSENSITIVE);
    /**
     * The capture group name for the optional escape prefix \ or \\ before the
     * braces {}.
     */
    private static final String ESCAPE = "escape";
    /**
     * The capture group name for the option string (in between braces {}).
     */
    private static final String OPTIONS = "options";
    /**
     * A precompiled {@link Pattern} matching a parameter replacement token,
     * consisting of an optional escape prefix (\ or \\), and braces {},
     * optionally containing options.
     */
    private static final Pattern TOKEN = Pattern.compile("(?i)(?<" + ESCAPE + ">\\\\{0,2})\\{(?<" + OPTIONS + ">(?:" + OPTION + ",?)*)?\\}");

    /**
     * The option types (see {@link #OPTION}).
     */
    private enum OpType {
        TRIM, LOWER, UPPER, INDEX_TO, INDEX_LENGTH, MATCH, FORMAT, DATE
    };

    /**
     * Encapsulates an option and the operation to be performed, including (in
     * the case of substring options) the parameters.
     */
    private static class Op {
        public OpType type;
        Reference p1 = null;
        Reference p2 = null;

        private Op(OpType type, Reference p1, Reference p2) {
            this.type = type;
            this.p1 = p1;
            this.p2 = p2;
        }

        public static Op trim() {
            return new Op(OpType.TRIM, null, null);
        }

        public static Op lower() {
            return new Op(OpType.LOWER, null, null);
        }

        public static Op upper() {
            return new Op(OpType.UPPER, null, null);
        }

        public static Op index_to(String from, String to) {
            return new Op(OpType.INDEX_TO, new Reference(from), new Reference(to));
        }

        public static Op index_length(String from, String length) {
            return new Op(OpType.INDEX_LENGTH, new Reference(from), new Reference(length));
        }

        public static Op match(String pattern, String group) {
            return new Op(OpType.MATCH, new Reference(pattern), new Reference(group));
        }

        public static Op format(String format) {
            return new Op(OpType.FORMAT, new Reference(format), null);
        }

        public static Op date(String format, String zone) {
            return new Op(OpType.DATE, new Reference(format), new Reference(zone));
        }

        public String apply(Object o, Object[] args, int[] arg) {
            switch (type) {
            case TRIM:
                return o.toString().trim();
            case LOWER:
                return o.toString().toLowerCase();
            case UPPER:
                return o.toString().toUpperCase();
            case INDEX_TO:
                String s = o.toString();
                int from = Math.min(p1.resolveInt(args, arg), s.length() - 1);
                int to = p2.resolveInt(args, arg);
                if (to < 0) {
                    to = Math.max(0, s.length() + 1 + to);
                } else {
                    to = Math.min(to, s.length());
                }
                to = Math.max(to, from);
                return s.substring(from, to);
            case INDEX_LENGTH:
                s = o.toString();
                from = Math.min(p1.resolveInt(args, arg), s.length() - 1);
                to = Math.min(from + Math.max(0, p2.resolveInt(args, arg)), s.length());
                return s.substring(from, to);
            case MATCH:
                s = o.toString();
                String pattern = p1.resolveString(args, arg);
                Matcher matcher = Pattern.compile(pattern).matcher(s);
                if (matcher.find()) {
                    String group = p2.resolveString(args, arg);
                    if (group == null || group.isEmpty()) {
                        group = "0";
                    }
                    String result;
                    try {
                        if (group.matches("^\\d+$")) {
                            result = matcher.group(Integer.valueOf(group));
                        } else {
                            result = matcher.group(group);
                        }
                    } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
                        return "";
                    }
                    return result == null ? "" : result;
                } else {
                    return "";
                }
            case FORMAT:
                String format = p1.resolveString(args, arg);
                return String.format(format, o);
            case DATE:
                try {
                    format = p1.resolveString(args, arg);
                    String zone = p2.resolveString(args, arg);
                    DateFormat df = new SimpleDateFormat(format);
                    if (zone != null && !zone.isEmpty()) {
                        df.setTimeZone(TimeZone.getTimeZone(zone));
                    }
                    return df.format((Date) o);
                } catch (ClassCastException e) {
                    return "";
                }
            default:
                return "";
            }
        }
    }

    /**
     * Encapsulates a substring index or pattern/group reference, one of:
     * <ul>
     * <li>a number or string literal &mdash; {@code indirect} is {@code false}
     * and {@code value} is the value</li>
     * <li>empty braces {} &mdash; {@code indirect} is {@code true} and
     * {@code index} is {@code 0}</li>
     * <li>braced parameter index {index} &mdash; {@code indirect} is
     * {@code true} and {@code index} is the index</li>
     * </ul>
     */
    private static class Reference {
        private boolean indirect;
        private int index;
        private String value;

        public Reference(String s) {
            if (s != null && s.matches("^\\{\\d*\\}$")) {
                indirect = true;
                if (s.equals("{}")) {
                    index = 0;
                } else {
                    index = Integer.valueOf(s.substring(1, s.length() - 1));
                }
            } else {
                indirect = false;
                value = s;
            }
        }

        public int resolveInt(Object[] args, int[] arg) {
            if (indirect) {
                if (index == 0) {
                    index = arg[0] + 1;
                    arg[0]++;
                }
                if (index > args.length) {
                    return 0;
                }
                return asint(args[index - 1]);
            } else {
                return Integer.valueOf(value);
            }
        }

        public String resolveString(Object[] args, int[] arg) {
            if (indirect) {
                if (index == 0) {
                    index = arg[0] + 1;
                    arg[0]++;
                }
                if (index > args.length) {
                    return "";
                }
                return args[index - 1].toString();
            } else {
                return value;
            }
        }

        private int asint(Object o) {
            if (o instanceof Integer) {
                return (Integer) o;
            } else if (o instanceof Double) {
                return ((Double) o).intValue();
            } else {
                String s = o.toString();
                return s.isEmpty() ? 0 : Integer.valueOf(s);
            }
        }
    }

    private static final String ISO8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    private static final int INDEX_NEXT = 0;
    private static final int INDEX_NOW = -1;

    public static String expand(String template, Object... args) {
        Matcher m = TOKEN.matcher(template);
        StringBuffer s = new StringBuffer();
        int[] arg = { 0 };
        String replacement;
        while (m.find()) {
            if (m.group(ESCAPE).length() == 1) {
                replacement = "{" + m.group(OPTIONS) + "}";
            } else {
                Object working;
                int index = INDEX_NEXT;
                // parse the options
                List<Op> ops = new ArrayList<>();
                String options = m.group(OPTIONS);
                if (options != null && !options.isEmpty()) {
                    Matcher optioner = OPTION_PATTERN.matcher(options);
                    while (optioner.find()) {
                        String option = optioner.group(0);
                        if (option.equalsIgnoreCase("trim")) {
                            ops.add(Op.trim());
                        } else if (option.equalsIgnoreCase("lower") || option.equalsIgnoreCase("tolower")) {
                            ops.add(Op.lower());
                        } else if (option.equalsIgnoreCase("upper") || option.equalsIgnoreCase("toupper")) {
                            ops.add(Op.upper());
                        } else if (option.matches("\\d+")) {
                            index = Integer.valueOf(option);
                        } else if (option.startsWith("[")) {
                            Matcher indexer = SUBSTR_OPTION_PATTERN.matcher(option);
                            indexer.matches(); // of course it does
                            if (indexer.group(4) != null) {
                                ops.add(Op.match(indexer.group(4), indexer.group(5)));
                            } else if (indexer.group(2) == null) {
                                ops.add(Op.index_to(indexer.group(1), "-1"));
                            } else if (indexer.group(2).equals(",")) {
                                ops.add(Op.index_to(indexer.group(1), indexer.group(3)));
                            } else {
                                ops.add(Op.index_length(indexer.group(1), indexer.group(3)));
                            }
                        } else if (option.startsWith("%")) {
                            ops.add(Op.format(option));
                        } else {
                            Matcher dater = DATE_PATTERN.matcher(option);
                            if (dater.matches()) {
                                boolean now = dater.group("tag").equalsIgnoreCase("now");
                                if (now) {
                                    index = INDEX_NOW;
                                }
                                String format = dater.group("format");
                                if (format == null || format.isEmpty()) {
                                    format = ISO8601;
                                }
                                if (format != null && !format.isEmpty()) {
                                    ops.add(Op.date(format.replaceAll("\\\\(.)", "$1"), dater.group("zone")));
                                }
                            }
                        }
                    }
                }
                // select the base string
                if (index == INDEX_NOW) {
                    working = new Date();
                } else {
                    if (index == INDEX_NEXT) {
                        index = arg[0] + 1;
                        arg[0]++;
                    }
                    working = index <= args.length ? args[index - 1] : "";
                }
                // apply the options
                for (Op op : ops) {
                    working = op.apply(working, args, arg);
                }
                // convert to String
                replacement = working.toString();
                // escape
                if (m.group(ESCAPE).length() == 2) {
                    replacement = "\\\\" + replacement;
                }
            }
            m.appendReplacement(s, replacement);
        }
        m.appendTail(s);
        return s.toString();
    }

}
