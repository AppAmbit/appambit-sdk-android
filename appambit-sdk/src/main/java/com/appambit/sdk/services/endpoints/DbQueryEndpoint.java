package com.appambit.sdk.services.endpoints;

import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.models.db.DbQueryRequest;
import com.appambit.sdk.services.interfaces.IEndpoint;
import java.util.List;

public class DbQueryEndpoint extends BaseEndpoint implements IEndpoint {
    public DbQueryEndpoint(String sql, List<Object> params) {
        this.setUrl("/db/query");
        this.setMethod(HttpMethodEnum.POST);
        this.setPayload(new DbQueryRequest(sql, params));
    }
}
