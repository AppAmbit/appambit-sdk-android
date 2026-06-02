package com.appambit.sdk.models.db;

import com.appambit.sdk.utils.JsonKey;
import java.util.List;

public class DbBatchRequest {
    @JsonKey("statements")
    private List<DbStatement> statements;
    @JsonKey("transaction")
    private boolean transaction;

    public DbBatchRequest(List<DbStatement> statements, boolean transaction) {
        this.statements = statements;
        this.transaction = transaction;
    }
}
