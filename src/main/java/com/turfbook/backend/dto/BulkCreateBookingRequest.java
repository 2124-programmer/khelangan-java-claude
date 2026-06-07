package com.turfbook.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class BulkCreateBookingRequest {

    @JsonProperty("venueId")
    private Long venueId;

    @JsonProperty("courtId")
    private Long courtId;

    @JsonProperty("date")
    private String date; // YYYY-MM-DD

    @JsonProperty("startTimes")
    private List<String> startTimes; // ["09:00", "10:00", ...]

    @JsonProperty("sport")
    private String sport;

    @JsonProperty("couponCode")
    private String couponCode;

    @JsonProperty("paymentMethod")
    private String paymentMethod;

    public BulkCreateBookingRequest() {}

    public Long getVenueId() { return venueId; }
    public void setVenueId(Long venueId) { this.venueId = venueId; }

    public Long getCourtId() { return courtId; }
    public void setCourtId(Long courtId) { this.courtId = courtId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public List<String> getStartTimes() { return startTimes; }
    public void setStartTimes(List<String> startTimes) { this.startTimes = startTimes; }

    public String getSport() { return sport; }
    public void setSport(String sport) { this.sport = sport; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
