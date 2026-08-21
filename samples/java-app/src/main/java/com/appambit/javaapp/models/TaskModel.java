package com.appambit.javaapp.models;

import com.appambit.sdk.annotations.DbColumn;

public class TaskModel {
    @DbColumn("id")
    public int id;
    @DbColumn("title")
    public String title;

    @DbColumn("is_completed")
    public int isCompleted;

    @DbColumn("priority")
    public String priority;

    @DbColumn("due_date")
    public String dueDate;

    public TaskModel() {}
}
