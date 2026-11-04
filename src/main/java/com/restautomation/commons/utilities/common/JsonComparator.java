package com.restautomation.commons.utilities.common;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Compares an actual JSON payload with an expected fixture and returns readable differences.
 * <p>
 * Fixture values may be matcher tokens instead of literals:
 * <ul>
 *   <li>{@code <any>} – field must exist (any value, including null)</li>
 *   <li>{@code <notNull>} / {@code <notEmpty>} – present and non-null / non-blank</li>
 *   <li>{@code <number>}, {@code <boolean>}, {@code <string>}</li>
 *   <li>{@code <isoDateTime>} – parseable date-time; {@code <uuid>}</li>
 *   <li>{@code <regex:...>} – textual value matches the pattern</li>
 *   <li>{@code <oneOf:a|b|c>} – value is one of the listed values</li>
 * </ul>
 * Fields named in {@code ignoreFields} are skipped entirely (at any depth).
 */
public final class JsonComparator {

    private static final Pattern UUID = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private JsonComparator() {
    }

    public static List<String> compare(JsonNode expected, JsonNode actual, Set<String> ignoreFields,
                                       boolean allowExtraFields, boolean strictArrayOrder) {
        List<String> differences = new ArrayList<>();
        compare("$", expected, actual, ignoreFields, allowExtraFields, strictArrayOrder, differences);
        return differences;
    }

    /** Lenient default: extra fields allowed, array order ignored. */
    public static List<String> compare(JsonNode expected, JsonNode actual, Set<String> ignoreFields) {
        return compare(expected, actual, ignoreFields, true, false);
    }

    private static void compare(String path, JsonNode expected, JsonNode actual, Set<String> ignore,
                                boolean allowExtra, boolean strictOrder, List<String> diffs) {
        if (expected.isTextual() && isToken(expected.asText())) {
            String failure = matchToken(expected.asText(), actual);
            if (failure != null) {
                diffs.add(path + ": " + failure);
            }
            return;
        }
        if (actual == null || actual.isMissingNode()) {
            diffs.add(path + ": expected " + expected + " but field is missing");
            return;
        }
        if (expected.isObject()) {
            if (!actual.isObject()) {
                diffs.add(path + ": expected an object but was " + actual.getNodeType());
                return;
            }
            Iterator<Map.Entry<String, JsonNode>> fields = expected.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (ignore.contains(field.getKey())) {
                    continue;
                }
                String childPath = path + "." + field.getKey();
                JsonNode actualChild = actual.get(field.getKey());
                if (actualChild == null && !(field.getValue().isTextual() && isToken(field.getValue().asText()))) {
                    diffs.add(childPath + ": field is missing");
                    continue;
                }
                compare(childPath, field.getValue(), actualChild, ignore, allowExtra, strictOrder, diffs);
            }
            if (!allowExtra) {
                actual.fieldNames().forEachRemaining(name -> {
                    if (!expected.has(name) && !ignore.contains(name)) {
                        diffs.add(path + "." + name + ": unexpected field");
                    }
                });
            }
        } else if (expected.isArray()) {
            if (!actual.isArray()) {
                diffs.add(path + ": expected an array but was " + actual.getNodeType());
                return;
            }
            if (expected.size() != actual.size()) {
                diffs.add(path + ": expected " + expected.size() + " elements but found " + actual.size());
                return;
            }
            if (strictOrder) {
                for (int i = 0; i < expected.size(); i++) {
                    compare(path + "[" + i + "]", expected.get(i), actual.get(i), ignore, allowExtra, true, diffs);
                }
            } else {
                boolean[] used = new boolean[actual.size()];
                for (int i = 0; i < expected.size(); i++) {
                    boolean matched = false;
                    for (int j = 0; j < actual.size() && !matched; j++) {
                        if (!used[j] && compare(expected.get(i), actual.get(j), ignore, allowExtra, false).isEmpty()) {
                            used[j] = true;
                            matched = true;
                        }
                    }
                    if (!matched) {
                        diffs.add(path + "[" + i + "]: no matching element for " + expected.get(i));
                    }
                }
            }
        } else if (expected.isNumber() && actual.isNumber()) {
            if (expected.decimalValue().compareTo(actual.decimalValue()) != 0) {
                diffs.add(path + ": expected " + expected + " but was " + actual);
            }
        } else if (!expected.equals(actual)) {
            diffs.add(path + ": expected " + expected + " but was " + actual);
        }
    }

    private static boolean isToken(String text) {
        return text.startsWith("<") && text.endsWith(">") && text.length() > 2;
    }

    /** Returns null when the actual value satisfies the token, otherwise a failure message. */
    private static String matchToken(String token, JsonNode actual) {
        boolean missing = actual == null || actual.isMissingNode();
        String inner = token.substring(1, token.length() - 1);
        if (missing) {
            return "expected " + token + " but field is missing";
        }
        String text = actual.isValueNode() ? actual.asText() : actual.toString();
        boolean ok;
        if (inner.startsWith("regex:")) {
            ok = !actual.isNull() && Pattern.compile(inner.substring(6)).matcher(text).matches();
        } else if (inner.startsWith("oneOf:")) {
            ok = !actual.isNull() && List.of(inner.substring(6).split("\\|")).contains(text);
        } else {
            ok = switch (inner) {
                case "any" -> true;
                case "notNull" -> !actual.isNull();
                case "notEmpty" -> !actual.isNull() && !text.isBlank() && !(actual.isContainerNode() && actual.isEmpty());
                case "number" -> actual.isNumber();
                case "boolean" -> actual.isBoolean();
                case "string" -> actual.isTextual();
                case "isoDateTime" -> actual.isTextual() && DateTimeUtils.isParseable(text);
                case "uuid" -> actual.isTextual() && UUID.matcher(text).matches();
                default -> !actual.isNull() && text.equals(token); // unknown token: literal compare
            };
        }
        return ok ? null : "expected " + token + " but was " + actual;
    }
}
