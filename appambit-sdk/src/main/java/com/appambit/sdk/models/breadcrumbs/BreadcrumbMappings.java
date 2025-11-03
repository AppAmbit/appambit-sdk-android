package com.appambit.sdk.models.breadcrumbs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class BreadcrumbMappings {

    private BreadcrumbMappings() { }

    public static BreadcrumEntity toEntity(BreadcrumbData data) {
        if (data == null) throw new IllegalArgumentException("data");
        UUID id = data.getId() != null ? data.getId() : UUID.randomUUID();
        Date ts = data.getTimestamp() != null ? data.getTimestamp() : new Date();

        BreadcrumEntity e = new BreadcrumEntity();
        e.setId(id);
        e.setCreatedAt(ts);
        e.setSessionId(data.getSessionId() != null ? data.getSessionId() : "");
        e.setName(data.getName() != null ? data.getName() : "");
        return e;
    }

    public static BreadcrumbData toData(BreadcrumEntity entity) {
        if (entity == null) throw new IllegalArgumentException("entity");
        BreadcrumbData d = new BreadcrumbData();
        d.setId(entity.getId());
        d.setSessionId(entity.getSessionId());
        d.setTimestamp(entity.getCreatedAt());
        d.setName(entity.getName());
        return d;
    }

    public static List<BreadcrumEntity> toEntities(List<BreadcrumbData> items) {
        if (items == null) return Collections.emptyList();
        List<BreadcrumEntity> out = new ArrayList<>(items.size());
        for (BreadcrumbData d : items) {
            if (d != null) out.add(toEntity(d));
        }
        return out;
    }

    public static List<BreadcrumbData> toDataList(List<BreadcrumEntity> items) {
        if (items == null) return Collections.emptyList();
        List<BreadcrumbData> out = new ArrayList<>(items.size());
        for (BreadcrumEntity e : items) {
            if (e != null) out.add(toData(e));
        }
        return out;
    }
}
