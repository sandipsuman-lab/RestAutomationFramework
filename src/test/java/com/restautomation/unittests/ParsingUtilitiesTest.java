package com.restautomation.unittests;

import com.restautomation.commons.utilities.common.DateTimeUtils;
import com.restautomation.commons.utilities.common.PlaceholderResolver;
import com.restautomation.commons.utilities.parsers.DataConversionHelper;
import com.restautomation.commons.utilities.parsers.ParseTableNames;
import org.testng.annotations.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ParsingUtilitiesTest {

    @Test
    public void parsesDateTimesInManyFormats() {
        Instant expected = Instant.parse("2026-10-05T10:15:30Z");
        assertThat(DateTimeUtils.parseToInstant("2026-10-05T10:15:30Z")).isEqualTo(expected);
        assertThat(DateTimeUtils.parseToInstant("2026-10-05T12:15:30+02:00")).isEqualTo(expected);
        assertThat(DateTimeUtils.parseToInstant("2026-10-05 10:15:30")).isEqualTo(expected);
        assertThat(DateTimeUtils.parseToInstant("2026-10-05 10:15:30.000")).isEqualTo(expected);
        assertThat(DateTimeUtils.isParseable("12345")).isFalse();
    }

    @Test
    public void normalisesValuesForComparison() {
        assertThat(DataConversionHelper.valuesEqual("10", 10.00)).isTrue();
        assertThat(DataConversionHelper.valuesEqual("Y", true)).isTrue();
        assertThat(DataConversionHelper.valuesEqual(null, "")).isTrue();
        assertThat(DataConversionHelper.valuesEqual("abc", "ABC")).isFalse();
        assertThat(DataConversionHelper.valuesEqual(List.of("shoe park", "shop"), "shoe park,shop")).isTrue();
        assertThat(DataConversionHelper.valuesEqual(List.of("shoe park", "shop"), "shop,shoe park")).isFalse();
    }

    @Test
    public void resolvesPlaceholders() {
        String resolved = PlaceholderResolver.resolve("id-${randomNumber:4}|${now:yyyy}|${custom}|${unknown}",
                key -> Map.of("custom", "value").get(key));

        assertThat(resolved).matches("id-\\d{4}\\|\\d{4}\\|value\\|\\$\\{unknown}");
    }

    @Test
    public void extractsTableNames() {
        assertThat(ParseTableNames.parse("SELECT * FROM ANALYTICS.ITEMS_VW i JOIN ref.CATEGORY c ON 1=1"))
                .containsExactly("ANALYTICS.ITEMS_VW", "ref.CATEGORY");
    }
}
