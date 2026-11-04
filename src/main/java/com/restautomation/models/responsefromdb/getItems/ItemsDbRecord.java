package com.restautomation.models.responsefromdb.getItems;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/** One row of the items view in the warehouse (column labels are lower-cased by the connector). */
@Data
public class ItemsDbRecord {
    @JsonProperty("item_id")
    private String itemId;
    @JsonProperty("item_name")
    private String itemName;
    private String category;
    private BigDecimal price;
    private Integer quantity;
    private String status;
    @JsonProperty("updated_at")
    private String updatedAt;
    @JsonProperty("tenant_id")
    private String tenantId;
    @JsonProperty("department_id")
    private String departmentId;
    @JsonProperty("opt_out_flag")
    private Boolean optOutFlag;
}
