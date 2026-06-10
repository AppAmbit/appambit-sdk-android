package com.appambit.javaapp.models;

import com.appambit.sdk.annotations.DbColumn;

public class TaskModel {
    public int id;
    public String title;

    @DbColumn("is_completed")
    public int isCompleted;

    public String priority;

    @DbColumn("due_date")
    public String dueDate;

    public TaskModel() {}
}
