package com.restautomation.controller.db;

import com.restautomation.commons.db.ReadDbQueryFromYAML;
import com.restautomation.commons.utilities.dbhelpers.SnowFlakeDbConnector;
import com.restautomation.commons.utilities.enumhelpers.DataType;
import com.restautomation.models.responsefromdb.getItems.ItemsDbRecord;
import org.springframework.stereotype.Component;

import java.util.List;

/** Warehouse queries for the GetItems API. */
@Component
public class ItemsController extends BaseDbController {

    public ItemsController(ReadDbQueryFromYAML queries, SnowFlakeDbConnector connector) {
        super(queries, connector);
    }

    @Override
    public DataType getDataType() {
        return DataType.GetItems;
    }

    public ItemsDbRecord fetchItemById(String itemId) {
        return connector.getQueryResult(sql("byId"), List.of(itemId), getDataType(), false);
    }
}
