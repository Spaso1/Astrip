package org.astral.astrip.been;

import java.util.Map;

public class Project {
    private int id;
    private int user_id;
    private String name;
    private String date_create;
    private String date_start;
    private String date_end;
    private Map<String, Pointer> paths;
    private String colorARGB;

    public String getColorARGB() {
        return colorARGB;
    }

    public void setColorARGB(String colorARGB) {
        this.colorARGB = colorARGB;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate_create() {
        return date_create;
    }

    public void setDate_create(String date_create) {
        this.date_create = date_create;
    }

    public String getDate_start() {
        return date_start;
    }

    public void setDate_start(String date_start) {
        this.date_start = date_start;
    }

    public String getDate_end() {
        return date_end;
    }

    public void setDate_end(String date_end) {
        this.date_end = date_end;
    }

    public Map<String, Pointer> getPaths() {
        return paths;
    }

    public void setPaths(Map<String, Pointer> paths) {
        this.paths = paths;
    }
}
