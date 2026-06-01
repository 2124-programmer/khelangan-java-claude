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
 * CreateBookingRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class CreateBookingRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long venueId;

  private Long courtId;

  private Long slotId;

  private String sport;

  private String couponCode;

  private String paymentMethod = "CARD";

  public CreateBookingRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateBookingRequest(Long venueId, Long courtId, Long slotId, String sport) {
    this.venueId = venueId;
    this.courtId = courtId;
    this.slotId = slotId;
    this.sport = sport;
  }

  public CreateBookingRequest venueId(Long venueId) {
    this.venueId = venueId;
    return this;
  }

  /**
   * Get venueId
   * @return venueId
  */
  @NotNull 
  @JsonProperty("venueId")
  public Long getVenueId() {
    return venueId;
  }

  public void setVenueId(Long venueId) {
    this.venueId = venueId;
  }

  public CreateBookingRequest courtId(Long courtId) {
    this.courtId = courtId;
    return this;
  }

  /**
   * Get courtId
   * @return courtId
  */
  @NotNull 
  @JsonProperty("courtId")
  public Long getCourtId() {
    return courtId;
  }

  public void setCourtId(Long courtId) {
    this.courtId = courtId;
  }

  public CreateBookingRequest slotId(Long slotId) {
    this.slotId = slotId;
    return this;
  }

  /**
   * Get slotId
   * @return slotId
  */
  @NotNull 
  @JsonProperty("slotId")
  public Long getSlotId() {
    return slotId;
  }

  public void setSlotId(Long slotId) {
    this.slotId = slotId;
  }

  public CreateBookingRequest sport(String sport) {
    this.sport = sport;
    return this;
  }

  /**
   * Get sport
   * @return sport
  */
  @NotNull 
  @JsonProperty("sport")
  public String getSport() {
    return sport;
  }

  public void setSport(String sport) {
    this.sport = sport;
  }

  public CreateBookingRequest couponCode(String couponCode) {
    this.couponCode = couponCode;
    return this;
  }

  /**
   * Get couponCode
   * @return couponCode
  */
  
  @JsonProperty("couponCode")
  public String getCouponCode() {
    return couponCode;
  }

  public void setCouponCode(String couponCode) {
    this.couponCode = couponCode;
  }

  public CreateBookingRequest paymentMethod(String paymentMethod) {
    this.paymentMethod = paymentMethod;
    return this;
  }

  /**
   * Get paymentMethod
   * @return paymentMethod
  */
  
  @JsonProperty("paymentMethod")
  public String getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(String paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateBookingRequest createBookingRequest = (CreateBookingRequest) o;
    return Objects.equals(this.venueId, createBookingRequest.venueId) &&
        Objects.equals(this.courtId, createBookingRequest.courtId) &&
        Objects.equals(this.slotId, createBookingRequest.slotId) &&
        Objects.equals(this.sport, createBookingRequest.sport) &&
        Objects.equals(this.couponCode, createBookingRequest.couponCode) &&
        Objects.equals(this.paymentMethod, createBookingRequest.paymentMethod);
  }

  @Override
  public int hashCode() {
    return Objects.hash(venueId, courtId, slotId, sport, couponCode, paymentMethod);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateBookingRequest {\n");
    sb.append("    venueId: ").append(toIndentedString(venueId)).append("\n");
    sb.append("    courtId: ").append(toIndentedString(courtId)).append("\n");
    sb.append("    slotId: ").append(toIndentedString(slotId)).append("\n");
    sb.append("    sport: ").append(toIndentedString(sport)).append("\n");
    sb.append("    couponCode: ").append(toIndentedString(couponCode)).append("\n");
    sb.append("    paymentMethod: ").append(toIndentedString(paymentMethod)).append("\n");
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

