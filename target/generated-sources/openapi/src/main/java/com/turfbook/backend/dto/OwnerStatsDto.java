package com.turfbook.backend.dto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * OwnerStatsDto
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class OwnerStatsDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long todayBookings;

  private Long todayRevenue;

  private Long weekRevenue;

  private Long monthRevenue;

  private Long pendingPayout;

  public OwnerStatsDto todayBookings(Long todayBookings) {
    this.todayBookings = todayBookings;
    return this;
  }

  /**
   * Get todayBookings
   * @return todayBookings
  */
  
  @JsonProperty("todayBookings")
  public Long getTodayBookings() {
    return todayBookings;
  }

  public void setTodayBookings(Long todayBookings) {
    this.todayBookings = todayBookings;
  }

  public OwnerStatsDto todayRevenue(Long todayRevenue) {
    this.todayRevenue = todayRevenue;
    return this;
  }

  /**
   * Get todayRevenue
   * @return todayRevenue
  */
  
  @JsonProperty("todayRevenue")
  public Long getTodayRevenue() {
    return todayRevenue;
  }

  public void setTodayRevenue(Long todayRevenue) {
    this.todayRevenue = todayRevenue;
  }

  public OwnerStatsDto weekRevenue(Long weekRevenue) {
    this.weekRevenue = weekRevenue;
    return this;
  }

  /**
   * Get weekRevenue
   * @return weekRevenue
  */
  
  @JsonProperty("weekRevenue")
  public Long getWeekRevenue() {
    return weekRevenue;
  }

  public void setWeekRevenue(Long weekRevenue) {
    this.weekRevenue = weekRevenue;
  }

  public OwnerStatsDto monthRevenue(Long monthRevenue) {
    this.monthRevenue = monthRevenue;
    return this;
  }

  /**
   * Get monthRevenue
   * @return monthRevenue
  */
  
  @JsonProperty("monthRevenue")
  public Long getMonthRevenue() {
    return monthRevenue;
  }

  public void setMonthRevenue(Long monthRevenue) {
    this.monthRevenue = monthRevenue;
  }

  public OwnerStatsDto pendingPayout(Long pendingPayout) {
    this.pendingPayout = pendingPayout;
    return this;
  }

  /**
   * Get pendingPayout
   * @return pendingPayout
  */
  
  @JsonProperty("pendingPayout")
  public Long getPendingPayout() {
    return pendingPayout;
  }

  public void setPendingPayout(Long pendingPayout) {
    this.pendingPayout = pendingPayout;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OwnerStatsDto ownerStatsDto = (OwnerStatsDto) o;
    return Objects.equals(this.todayBookings, ownerStatsDto.todayBookings) &&
        Objects.equals(this.todayRevenue, ownerStatsDto.todayRevenue) &&
        Objects.equals(this.weekRevenue, ownerStatsDto.weekRevenue) &&
        Objects.equals(this.monthRevenue, ownerStatsDto.monthRevenue) &&
        Objects.equals(this.pendingPayout, ownerStatsDto.pendingPayout);
  }

  @Override
  public int hashCode() {
    return Objects.hash(todayBookings, todayRevenue, weekRevenue, monthRevenue, pendingPayout);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OwnerStatsDto {\n");
    sb.append("    todayBookings: ").append(toIndentedString(todayBookings)).append("\n");
    sb.append("    todayRevenue: ").append(toIndentedString(todayRevenue)).append("\n");
    sb.append("    weekRevenue: ").append(toIndentedString(weekRevenue)).append("\n");
    sb.append("    monthRevenue: ").append(toIndentedString(monthRevenue)).append("\n");
    sb.append("    pendingPayout: ").append(toIndentedString(pendingPayout)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

