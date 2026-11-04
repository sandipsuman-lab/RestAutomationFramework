package com.restautomation.commons.utilities.common;

import com.restautomation.commons.utilities.enumhelpers.DateTimeOperation;
import com.restautomation.commons.utilities.enumhelpers.DateTimeUnit;
import com.restautomation.context.ScenarioContext;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@code ${...}} tokens in request templates, SQL parameters and feature-file values.
 * <pre>
 * ${uuid}                      random UUID
 * ${randomString:8}            8 random letters        ${randomAlphanumeric:8}
 * ${randomNumber:6}            6 random digits         ${randomEmail}
 * ${now}                       now, ISO UTC            ${now:yyyy-MM-dd}
 * ${now-4d} / ${now+2h:pattern} shifted now (units s, m, h, d, w, M, y)
 * ${ctx:key}                   value stored in the ScenarioContext
 * ${env:NAME} / ${prop:key}    environment variable / Spring property
 * ${anyKey}                    value from the map passed to resolve(...)
 * </pre>
 * Unknown tokens are left untouched.
 */
@Component
@RequiredArgsConstructor
public class PlaceholderResolver {

    private static final Pattern TOKEN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern NOW = Pattern.compile("^now(?:([+-])(\\d+)([smhdwMy]))?(?::(.+))?$");

    private final ScenarioContext scenarioContext;
    private final Environment environment;

    public String resolve(String template) {
        return resolve(template, Map.of());
    }

    public String resolve(String template, Map<String, ?> values) {
        return resolve(template, key -> {
            if (values != null && values.containsKey(key)) {
                return String.valueOf(values.get(key));
            }
            if (key.startsWith("ctx:")) {
                Object value = scenarioContext.get(key.substring(4));
                return value == null ? null : String.valueOf(value);
            }
            if (key.startsWith("prop:")) {
                return environment.getProperty(key.substring(5));
            }
            return null;
        });
    }

    /** Context-free resolution (generators, env vars and the supplied lookup only). */
    public static String resolve(String template, Function<String, String> lookup) {
        if (template == null || !template.contains("${")) {
            return template;
        }
        Matcher matcher = TOKEN.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = builtIn(key);
            if (value == null && lookup != null) {
                value = lookup.apply(key);
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(value != null ? value : matcher.group(0)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String builtIn(String key) {
        String[] parts = key.split(":", 2);
        String name = parts[0];
        String arg = parts.length > 1 ? parts[1] : null;
        switch (name) {
            case "uuid":
                return RandomDataUtils.uuid();
            case "randomString":
                return RandomDataUtils.alphabetic(arg == null ? 8 : Integer.parseInt(arg));
            case "randomAlphanumeric":
                return RandomDataUtils.alphanumeric(arg == null ? 8 : Integer.parseInt(arg));
            case "randomNumber":
                return RandomDataUtils.numeric(arg == null ? 6 : Integer.parseInt(arg));
            case "randomEmail":
                return RandomDataUtils.email();
            case "env":
                return arg == null ? null : System.getenv(arg);
            default:
                break;
        }
        Matcher now = NOW.matcher(key);
        if (now.matches()) {
            ZonedDateTime value = DateTimeUtils.nowUtc();
            if (now.group(1) != null) {
                DateTimeOperation operation = "+".equals(now.group(1)) ? DateTimeOperation.PLUS : DateTimeOperation.MINUS;
                value = operation.apply(value, Long.parseLong(now.group(2)), DateTimeUnit.fromSymbol(now.group(3)));
            }
            return DateTimeUtils.format(value, now.group(4) != null ? now.group(4) : DateTimeUtils.ISO_UTC_PATTERN);
        }
        return null;
    }
}
