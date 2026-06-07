package com.turfbook.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class BlockSelectedRequest {

    @JsonProperty("date")
    private String date; // YYYY-MM-DD

    @JsonProperty("startTimes")
    private List<String> startTimes; // ["09:00", "10:00", ...]

    public BlockSelectedRequest() {}

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public List<String> getStartTimes() { return startTimes; }
    public void setStartTimes(List<String> startTimes) { this.startTimes = startTimes; }
}
