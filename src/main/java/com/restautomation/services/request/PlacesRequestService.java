package com.restautomation.services.request;

import com.restautomation.commons.endpoints.ReadEndpointsFromYAML;
import com.restautomation.commons.utilities.common.JsonUtils;
import com.restautomation.commons.utilities.common.PlaceholderResolver;
import com.restautomation.commons.utilities.enumhelpers.DataType;
import com.restautomation.commons.utilities.enumhelpers.RequestType;
import com.restautomation.commons.utilities.parsers.ResponseToClassObjectParser;
import com.restautomation.commons.utilities.readerhelpers.TestDataReader;
import com.restautomation.commons.utilities.requesthelpers.RequestGenerator;
import com.restautomation.context.ScenarioContext;
import com.restautomation.exceptions.FixtureNotFoundException;
import com.restautomation.models.requestmodels.places.AddPlaceRequest;
import com.restautomation.models.requestmodels.places.DeletePlaceRequest;
import com.restautomation.models.requestmodels.places.UpdatePlaceRequest;
import com.restautomation.models.responsefromjson.places.AddPlaceResponse;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Request side of the Places API: builds requests from templates / test data, calls the endpoints
 * named in endpoints.yml and keeps the scenario's place state (current place_id, AddPlace request,
 * updated address, places to clean up) on the {@link ScenarioContext}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlacesRequestService {

    public static final DataType TYPE = DataType.Places;
    public static final String ADD_PLACE = "AddPlaceAPI";
    public static final String GET_PLACE = "GetPlaceAPI";
    public static final String UPDATE_PLACE = "UpdatePlaceAPI";
    public static final String DELETE_PLACE = "DeletePlaceAPI";

    public static final String CTX_PLACE_ID = "placeId";
    public static final String CTX_ADD_REQUEST = "addPlaceRequestBody";
    public static final String CTX_UPDATED_ADDRESS = "updatedAddress";
    private static final String CTX_CREATED_PLACES = "createdPlaceIds";
    private static final String TEST_CASE_COLUMN = "testCaseId";
    private static final Map<String, Class<?>> TEMPLATE_MODELS = Map.of(
            "addPlace.json", AddPlaceRequest.class,
            "updatePlace.json", UpdatePlaceRequest.class,
            "deletePlace.json", DeletePlaceRequest.class);

    private final ScenarioContext context;
    private final RequestGenerator requestGenerator;
    private final RequestBodyBuilderService requestBodyBuilderService;
    private final ResponseToClassObjectParser responseParser;
    private final TestDataReader testDataReader;
    private final PlaceholderResolver placeholderResolver;
    private final ReadEndpointsFromYAML endpointsYaml;

    /** Fresh request against the Places API (base URL + key from endpoints.yml). */
    public void newRequest() {
        context.resetRequest();
        requestGenerator.selectApi(TYPE);
    }

    /**
     * Template + overrides → request POJO (AddPlaceRequest / UpdatePlaceRequest / DeletePlaceRequest),
     * serialised by REST Assured when sent. Templates without a model are sent as raw JSON.
     */
    public void setBodyFromTemplate(String template, Map<String, ?> overrides) {
        Class<?> model = TEMPLATE_MODELS.get(template);
        Object body = model != null
                ? requestBodyBuilderService.buildRequest(TYPE, template, overrides, model)
                : requestBodyBuilderService.buildRequestBody(TYPE, template, overrides);
        requestGenerator.setRequestBody(body);
    }

    /** UpdatePlace body built directly as a POJO; the key is the one configured in endpoints.yml. */
    public UpdatePlaceRequest setUpdatePlaceBody(String placeId, String address) {
        UpdatePlaceRequest body = new UpdatePlaceRequest(placeId, placeholderResolver.resolve(address),
                endpointsYaml.getApi(TYPE.getApiName()).getQueryParams().get("key"));
        requestGenerator.setRequestBody(body);
        return body;
    }

    public DeletePlaceRequest setDeletePlaceBody(String placeId) {
        DeletePlaceRequest body = new DeletePlaceRequest(placeId);
        requestGenerator.setRequestBody(body);
        return body;
    }

    public void addPlaceIdQueryParam(String placeId) {
        requestGenerator.addQueryParam("place_id", placeId);
    }

    public void usePlaceId(String placeId) {
        context.put(CTX_PLACE_ID, placeId);
    }

    public String currentPlaceId() {
        String placeId = context.get(CTX_PLACE_ID, String.class);
        if (placeId == null || placeId.isBlank()) {
            throw new IllegalStateException("No place_id available - add a place first");
        }
        return placeId;
    }

    /** Calls a named endpoint; on success records the state later steps and clean-up need. */
    public Response call(String endpointName, RequestType methodOverride) {
        Response response = requestGenerator.callNamedEndpoint(TYPE, endpointName, methodOverride);
        if (response.statusCode() == 200) {
            Object requestBody = context.getRequestBody();
            String body = requestBody instanceof String text ? text : JsonUtils.toJson(requestBody);
            switch (endpointName) {
                case ADD_PLACE -> {
                    String placeId = responseParser.parse(response, AddPlaceResponse.class).getPlaceId();
                    if (placeId != null) {
                        usePlaceId(placeId);
                        context.put(CTX_ADD_REQUEST, body);
                        createdPlaces().add(placeId);
                    }
                }
                case UPDATE_PLACE -> context.put(CTX_UPDATED_ADDRESS, JsonUtils.readTree(body).path("address").asText());
                case DELETE_PLACE -> createdPlaces().remove(JsonUtils.readTree(body).path("place_id").asText());
                default -> {
                    // nothing to record for reads
                }
            }
        }
        return response;
    }

    /** Test-data record for {@code testCaseId} (YAML, JSON, CSV or Excel), without the id and empty values. */
    public Map<String, Object> testData(String file, String testCaseId) {
        Map<String, Object> row = testDataReader.readRecords(file).stream()
                .filter(r -> Objects.equals(String.valueOf(r.get(TEST_CASE_COLUMN)), testCaseId))
                .findFirst()
                .orElseThrow(() -> new FixtureNotFoundException("No record with " + TEST_CASE_COLUMN + "=" + testCaseId + " in " + file));
        Map<String, Object> values = new LinkedHashMap<>(row);
        values.remove(TEST_CASE_COLUMN);
        values.values().removeIf(Objects::isNull);
        return values;
    }

    /** Deletes every place the scenario created and did not delete itself. Never throws. */
    public void deleteCreatedPlaces() {
        Set<String> created = context.get(CTX_CREATED_PLACES);
        if (created == null || created.isEmpty()) {
            return;
        }
        for (String placeId : Set.copyOf(created)) {
            try {
                newRequest();
                setDeletePlaceBody(placeId);
                Response response = requestGenerator.callNamedEndpoint(TYPE, DELETE_PLACE, null);
                log.info("Clean-up: deleted place {} -> HTTP {}", placeId, response.statusCode());
            } catch (RuntimeException e) {
                log.warn("Clean-up: could not delete place {}: {}", placeId, e.getMessage());
            }
        }
        created.clear();
    }

    private Set<String> createdPlaces() {
        Set<String> created = context.get(CTX_CREATED_PLACES);
        if (created == null) {
            created = new LinkedHashSet<>();
            context.put(CTX_CREATED_PLACES, created);
        }
        return created;
    }
}
