package com.restautomation.models.responsefromjson.places;

import lombok.Data;

/** Status/message responses of update, delete and not-found calls. */
@Data
public class PlaceMessageResponse {
    private String status;
    private String msg;
}
