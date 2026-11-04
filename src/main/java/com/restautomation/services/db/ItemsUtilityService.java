package com.restautomation.services.db;

import com.restautomation.commons.utilities.dbhelpers.AbstractTransformerService;
import com.restautomation.commons.utilities.enumhelpers.DataType;
import com.restautomation.models.responsefromdb.getItems.ItemsDbRecord;
import org.springframework.stereotype.Service;

/** Warehouse row JSON → {@link ItemsDbRecord}. */
@Service
public class ItemsUtilityService extends AbstractTransformerService<ItemsDbRecord> {

    public ItemsUtilityService() {
        super(ItemsDbRecord.class);
    }

    @Override
    public DataType getDataType() {
        return DataType.GetItems;
    }
}
