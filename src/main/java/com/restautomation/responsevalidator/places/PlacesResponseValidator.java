package com.restautomation.responsevalidator.places;

import com.fasterxml.jackson.databind.JsonNode;
import com.restautomation.commons.utilities.common.JsonUtils;
import com.restautomation.commons.utilities.datavalidation.ComparisonOptions;
import com.restautomation.commons.utilities.datavalidation.DataSet;
import com.restautomation.commons.utilities.datavalidation.DataValidationService;
import com.restautomation.commons.utilities.parsers.DataConversionHelper;
import com.restautomation.commons.utilities.parsers.ResponseToClassObjectParser;
import com.restautomation.models.requestmodels.places.AddPlaceRequest;
import org.assertj.core.api.SoftAssertions;
import com.restautomation.context.ScenarioContext;
import com.restautomation.models.responsefromjson.places.AddPlaceResponse;
import com.restautomation.models.responsefromjson.places.GetPlaceResponse;
import com.restautomation.services.request.PlacesRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Places API checks: AddPlace result, and GetPlace read-back against the request or external test data. */
@Component
@RequiredArgsConstructor
public class PlacesResponseValidator {

    private final ScenarioContext context;
    private final ResponseToClassObjectParser responseParser;
    private final DataValidationService dataValidationService;

    public void validatePlaceCreated() {
        AddPlaceResponse response = responseParser.parse(context.getResponse(), AddPlaceResponse.class);
        assertThat(response.getStatus()).as("status").isEqualTo("OK");
        assertThat(response.getPlaceId()).as("place_id").isNotBlank();
    }

    /** Typed comparison: the {@link GetPlaceResponse} POJO against the {@link AddPlaceRequest} POJO that was sent. */
    public void validateGetMatchesAddRequest() {
        String addRequest = context.get(PlacesRequestService.CTX_ADD_REQUEST, String.class);
        if (addRequest == null) {
            throw new IllegalStateException("No AddPlace request was sent in this scenario");
        }
        AddPlaceRequest sent = JsonUtils.fromJson(addRequest, AddPlaceRequest.class);
        GetPlaceResponse stored = responseParser.parse(context.getResponse(), GetPlaceResponse.class);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(stored.getName()).as("name").isEqualTo(sent.getName());
        soft.assertThat(stored.getAddress()).as("address").isEqualTo(sent.getAddress());
        soft.assertThat(stored.getPhoneNumber()).as("phone_number").isEqualTo(sent.getPhoneNumber());
        soft.assertThat(stored.getWebsite()).as("website").isEqualTo(sent.getWebsite());
        soft.assertThat(stored.getLanguage()).as("language").isEqualTo(sent.getLanguage());
        // The service returns numbers as strings and types as "a,b"; compare normalised values.
        soft.assertThat(DataConversionHelper.valuesEqual(sent.getAccuracy(), stored.getAccuracy()))
                .as("accuracy sent=%s stored=%s", sent.getAccuracy(), stored.getAccuracy()).isTrue();
        soft.assertThat(DataConversionHelper.valuesEqual(sent.getTypes(), stored.getTypes()))
                .as("types sent=%s stored=%s", sent.getTypes(), stored.getTypes()).isTrue();
        soft.assertThat(stored.getLocation()).as("location").isNotNull();
        if (stored.getLocation() != null && sent.getLocation() != null) {
            soft.assertThat(DataConversionHelper.valuesEqual(sent.getLocation().getLat(), stored.getLocation().getLat()))
                    .as("location.lat sent=%s stored=%s", sent.getLocation().getLat(), stored.getLocation().getLat()).isTrue();
            soft.assertThat(DataConversionHelper.valuesEqual(sent.getLocation().getLng(), stored.getLocation().getLng()))
                    .as("location.lng sent=%s stored=%s", sent.getLocation().getLng(), stored.getLocation().getLng()).isTrue();
        }
        soft.assertAll();
    }

    /** Test-data values may be typed (YAML/JSON) or text (CSV/Excel); text is interpreted like feature-file values. */
    public void validateGetMatchesTestData(String testCaseId, String file, Map<String, Object> testData) {
        Map<String, Object> expected = new LinkedHashMap<>();
        testData.forEach((field, value) -> {
            Object typed = value instanceof String text ? JsonUtils.toTypedValue(text) : value;
            if (typed instanceof JsonNode node) {
                typed = node.isArray() ? JsonUtils.convert(node, List.class) : JsonUtils.scalarValue(node);
            }
            expected.put(field, typed);
        });
        assertSamePlace(DataSet.fromRows("test data " + testCaseId + " in " + file, List.of(expected)));
    }

    public void validateUpdatedAddress() {
        GetPlaceResponse place = responseParser.parse(context.getResponse(), GetPlaceResponse.class);
        assertThat(place.getAddress()).as("address after update")
                .isEqualTo(context.get(PlacesRequestService.CTX_UPDATED_ADDRESS, String.class));
    }

    private void assertSamePlace(DataSet expected) {
        DataSet actual = DataSet.fromJson("GetPlace response", JsonUtils.readTree(context.getResponse().asString()));
        // The live GET returns location.latitude/longitude although the add contract uses lat/lng.
        // Every expected field must be present, so a renamed or dropped field fails instead of being skipped.
        ComparisonOptions options = ComparisonOptions.builder()
                .mapColumn("location.latitude", "location.lat")
                .mapColumn("location.longitude", "location.lng")
                .failOnMissingColumns(true)
                .build();
        dataValidationService.assertMatch(dataValidationService.compare(actual, expected, options), options);
    }
}
