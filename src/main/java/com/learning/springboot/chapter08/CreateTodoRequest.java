package com.learning.springboot.chapter08;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request DTO used by TodoController for POST requests. */
public class CreateTodoRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @Min(value = 1, message = "Priority must be 1 (low), 2 (medium), or 3 (high)")
    @Max(value = 3, message = "Priority must be 1 (low), 2 (medium), or 3 (high)")
    private int priority = 1;

    public CreateTodoRequest() {}

    public CreateTodoRequest(String title) { this.title = title; }
    public CreateTodoRequest(String title, int priority) {
        this.title    = title;
        this.priority = priority;
    }

    public String getTitle()    { return title; }
    public void   setTitle(String t) { this.title = t; }
    public int    getPriority() { return priority; }
    public void   setPriority(int p) { this.priority = p; }
}

