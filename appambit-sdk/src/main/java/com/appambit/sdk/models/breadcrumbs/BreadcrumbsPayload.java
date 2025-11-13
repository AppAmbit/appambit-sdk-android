package com.appambit.sdk.models.breadcrumbs;

import com.appambit.sdk.models.analytics.SessionBatch;
import com.appambit.sdk.utils.JsonKey;

import java.util.List;

public class BreadcrumbsPayload {

    public List<BreadcrumbData> getBreadcrumbs() {
        return breadcrumbs;
    }

    public void setBreadcrumbs(List<BreadcrumbData> breadcrumbs) {
        this.breadcrumbs = breadcrumbs;
    }

    @JsonKey("breadcrumbs")
    private List<BreadcrumbData> breadcrumbs;
}
