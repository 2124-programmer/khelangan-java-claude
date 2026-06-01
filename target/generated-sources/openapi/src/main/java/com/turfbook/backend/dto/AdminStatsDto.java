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
 * AdminStatsDto
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class AdminStatsDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long bookingsToday;

  private Long revenueToday;

  private Long newUsers;

  private Long activeVenues;

  private Long pendingApprovals;

  private Long openDisputes;

  public AdminStatsDto bookingsToday(Long bookingsToday) {
    this.bookingsToday = bookingsToday;
    return this;
  }

  /**
   * Get bookingsToday
   * @return bookingsToday
  */
  
  @JsonProperty("bookingsToday")
  public Long getBookingsToday() {
    return bookingsToday;
  }

  public void setBookingsToday(Long bookingsToday) {
    this.bookingsToday = bookingsToday;
  }

  public AdminStatsDto revenueToday(Long revenueToday) {
    this.revenueToday = revenueToday;
    return this;
  }

  /**
   * Get revenueToday
   * @return revenueToday
  */
  
  @JsonProperty("revenueToday")
  public Long getRevenueToday() {
    return revenueToday;
  }

  public void setRevenueToday(Long revenueToday) {
    this.revenueToday = revenueToday;
  }

  public AdminStatsDto newUsers(Long newUsers) {
    this.newUsers = newUsers;
    return this;
  }

  /**
   * Get newUsers
   * @return newUsers
  */
  
  @JsonProperty("newUsers")
  public Long getNewUsers() {
    return newUsers;
  }

  public void setNewUsers(Long newUsers) {
    this.newUsers = newUsers;
  }

  public AdminStatsDto activeVenues(Long activeVenues) {
    this.activeVenues = activeVenues;
    return this;
  }

  /**
   * Get activeVenues
   * @return activeVenues
  */
  
  @JsonProperty("activeVenues")
  public Long getActiveVenues() {
    return activeVenues;
  }

  public void setActiveVenues(Long activeVenues) {
    this.activeVenues = activeVenues;
  }

  public AdminStatsDto pendingApprovals(Long pendingApprovals) {
    this.pendingApprovals = pendingApprovals;
    return this;
  }

  /**
   * Get pendingApprovals
   * @return pendingApprovals
  */
  
  @JsonProperty("pendingApprovals")
  public Long getPendingApprovals() {
    return pendingApprovals;
  }

  public void setPendingApprovals(Long pendingApprovals) {
    this.pendingApprovals = pendingApprovals;
  }

  public AdminStatsDto openDisputes(Long openDisputes) {
    this.openDisputes = openDisputes;
    return this;
  }

  /**
   * Get openDisputes
   * @return openDisputes
  */
  
  @JsonProperty("openDisputes")
  public Long getOpenDisputes() {
    return openDisputes;
  }

  public void setOpenDisputes(Long openDisputes) {
    this.openDisputes = openDisputes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AdminStatsDto adminStatsDto = (AdminStatsDto) o;
    return Objects.equals(this.bookingsToday, adminStatsDto.bookingsToday) &&
        Objects.equals(this.revenueToday, adminStatsDto.revenueToday) &&
        Objects.equals(this.newUsers, adminStatsDto.newUsers) &&
        Objects.equals(this.activeVenues, adminStatsDto.activeVenues) &&
        Objects.equals(this.pendingApprovals, adminStatsDto.pendingApprovals) &&
        Objects.equals(this.openDisputes, adminStatsDto.openDisputes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bookingsToday, revenueToday, newUsers, activeVenues, pendingApprovals, openDisputes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AdminStatsDto {\n");
    sb.append("    bookingsToday: ").append(toIndentedString(bookingsToday)).append("\n");
    sb.append("    revenueToday: ").append(toIndentedString(revenueToday)).append("\n");
    sb.append("    newUsers: ").append(toIndentedString(newUsers)).append("\n");
    sb.append("    activeVenues: ").append(toIndentedString(activeVenues)).append("\n");
    sb.append("    pendingApprovals: ").append(toIndentedString(pendingApprovals)).append("\n");
    sb.append("    openDisputes: ").append(toIndentedString(openDisputes)).append("\n");
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

