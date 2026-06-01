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
 * ValidateCouponRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class ValidateCouponRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private String code;

  private Integer bookingAmount;

  public ValidateCouponRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ValidateCouponRequest(String code, Integer bookingAmount) {
    this.code = code;
    this.bookingAmount = bookingAmount;
  }

  public ValidateCouponRequest code(String code) {
    this.code = code;
    return this;
  }

  /**
   * Get code
   * @return code
  */
  @NotNull 
  @JsonProperty("code")
  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public ValidateCouponRequest bookingAmount(Integer bookingAmount) {
    this.bookingAmount = bookingAmount;
    return this;
  }

  /**
   * Get bookingAmount
   * @return bookingAmount
  */
  @NotNull 
  @JsonProperty("bookingAmount")
  public Integer getBookingAmount() {
    return bookingAmount;
  }

  public void setBookingAmount(Integer bookingAmount) {
    this.bookingAmount = bookingAmount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ValidateCouponRequest validateCouponRequest = (ValidateCouponRequest) o;
    return Objects.equals(this.code, validateCouponRequest.code) &&
        Objects.equals(this.bookingAmount, validateCouponRequest.bookingAmount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, bookingAmount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ValidateCouponRequest {\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    bookingAmount: ").append(toIndentedString(bookingAmount)).append("\n");
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

