package com.restautomation.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.restautomation.commons.utilities.common.JsonUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Stateful in-memory stand-in for the Places API (add / get / update / delete), mirroring the live
 * service: key=qaclick123 required, GET returns numbers as strings, {@code types} comma-joined and
 * location as latitude/longitude, unknown place_id gives 404 with a "doesn't exists" message.
 */
public class PlacesMockExtension implements ResponseDefinitionTransformerV2 {

    public static final String NAME = "places-store";
    private static final String API_KEY = "qaclick123";

    private final Map<String, ObjectNode> places = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }

    @Override
    public ResponseDefinition transform(ServeEvent serveEvent) {
        Request request = serveEvent.getRequest();
        if (!API_KEY.equals(query(request, "key"))) {
            return json(401, Map.of("msg", "Invalid or missing key"));
        }
        String path = request.getUrl().split("\\?")[0];
        String method = request.getMethod().getName();
        if (path.endsWith("/add/json") && method.equals("POST")) {
            return add(request);
        }
        if (path.endsWith("/get/json") && method.equals("GET")) {
            return get(query(request, "place_id"));
        }
        if (path.endsWith("/update/json") && method.equals("PUT")) {
            return update(request);
        }
        if (path.endsWith("/delete/json") && (method.equals("POST") || method.equals("DELETE"))) {
            return delete(request);
        }
        return json(404, Map.of("msg", "Resource not found"));
    }

    private ResponseDefinition add(Request request) {
        JsonNode body = JsonUtils.readTree(request.getBodyAsString());
        String placeId = UUID.randomUUID().toString().replace("-", "");
        places.put(placeId, (ObjectNode) body.deepCopy());
        String id = UUID.randomUUID().toString().replace("-", "");
        return json(200, Map.of("status", "OK", "place_id", placeId, "scope", "APP", "reference", id + id, "id", id));
    }

    private ResponseDefinition get(String placeId) {
        ObjectNode place = placeId == null ? null : places.get(placeId);
        if (place == null) {
            return json(404, Map.of("msg", "Get operation failed, looks like place_id  doesn't exists"));
        }
        ObjectNode view = place.deepCopy();
        JsonNode location = view.get("location");
        if (location instanceof ObjectNode loc) {
            // Live service renames lat/lng to latitude/longitude on read-back.
            ObjectNode renamed = view.putObject("location");
            renamed.put("latitude", loc.path("lat").asText());
            renamed.put("longitude", loc.path("lng").asText());
        }
        if (view.has("accuracy")) {
            view.put("accuracy", view.get("accuracy").asText());
        }
        if (view.path("types").isArray()) {
            view.put("types", StreamSupport.stream(view.get("types").spliterator(), false)
                    .map(JsonNode::asText).collect(Collectors.joining(",")));
        }
        return json(200, view);
    }

    private ResponseDefinition update(Request request) {
        JsonNode body = JsonUtils.readTree(request.getBodyAsString());
        String placeId = body.path("place_id").asText(query(request, "place_id"));
        ObjectNode place = places.get(placeId);
        if (place == null) {
            return json(404, Map.of("msg", "Update address operation failed, looks like the data doesn't exists"));
        }
        place.put("address", body.path("address").asText());
        return json(200, Map.of("msg", "Address successfully updated"));
    }

    private ResponseDefinition delete(Request request) {
        String placeId = JsonUtils.readTree(request.getBodyAsString()).path("place_id").asText();
        if (places.remove(placeId) == null) {
            return json(404, Map.of("msg", "Delete operation failed, looks like the data doesn't exists"));
        }
        return json(200, Map.of("status", "OK"));
    }

    private static String query(Request request, String name) {
        QueryParameter parameter = request.queryParameter(name);
        return parameter != null && parameter.isPresent() ? parameter.firstValue() : null;
    }

    private static ResponseDefinition json(int status, Object body) {
        return new ResponseDefinitionBuilder()
                .withStatus(status)
                .withHeader("Content-Type", "application/json;charset=UTF-8")
                .withHeader("Server", "Apache/2.4.52 (Ubuntu)")
                .withBody(JsonUtils.toJson(body))
                .build();
    }
}
