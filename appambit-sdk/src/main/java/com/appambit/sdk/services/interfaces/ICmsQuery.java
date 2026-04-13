package com.appambit.sdk.services.interfaces;

import com.appambit.sdk.CmsQuery.CmsQueryResult;
import java.util.List;

public interface ICmsQuery<T> {
    ICmsQuery<T> search(String query);
    ICmsQuery<T> equals(String field, String value);
    ICmsQuery<T> notEquals(String field, String value);
    ICmsQuery<T> contains(String field, String value);
    ICmsQuery<T> startsWith(String field, String value);
    ICmsQuery<T> greaterThan(String field, Number value);
    ICmsQuery<T> greaterThanOrEqual(String field, Number value);
    ICmsQuery<T> lessThan(String field, Number value);
    ICmsQuery<T> lessThanOrEqual(String field, Number value);
    ICmsQuery<T> inList(String field, List<String> values);
    ICmsQuery<T> notInList(String field, List<String> values);
    ICmsQuery<T> orderByAscending(String field);
    ICmsQuery<T> orderByDescending(String field);
    ICmsQuery<T> getPage(int page);
    ICmsQuery<T> getPerPage(int perPage);
    CmsQueryResult<T> getList();
}
