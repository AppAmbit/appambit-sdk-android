package com.appambit.sdk.models.breadcrumbs;

import com.appambit.sdk.utils.JsonKey;

public class Breadcrumb  {

    @JsonKey("name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
