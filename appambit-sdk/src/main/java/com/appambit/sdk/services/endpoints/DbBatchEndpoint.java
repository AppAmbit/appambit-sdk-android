package com.appambit.sdk.services.endpoints;

import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.models.db.DbBatchRequest;
import com.appambit.sdk.models.db.DbStatement;
import com.appambit.sdk.services.interfaces.IEndpoint;
import java.util.List;

public class DbBatchEndpoint extends BaseEndpoint implements IEndpoint {
    public DbBatchEndpoint(List<DbStatement> statements, boolean transaction) {
        this.setUrl("/db/batch");
        this.setMethod(HttpMethodEnum.POST);
        this.setPayload(new DbBatchRequest(statements, transaction));
    }
}
