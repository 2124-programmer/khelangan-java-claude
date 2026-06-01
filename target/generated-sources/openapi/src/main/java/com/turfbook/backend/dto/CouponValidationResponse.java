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
 * CouponValidationResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class CouponValidationResponse implements Serializable {

  private static final long serialVersionUID = 1L;

  private Boolean valid;

  private Integer discount;

  private String message;

  public CouponValidationResponse valid(Boolean valid) {
    this.valid = valid;
    return this;
  }

  /**
   * Get valid
   * @return valid
  */
  
  @JsonProperty("valid")
  public Boolean getValid() {
    return valid;
  }

  public void setValid(Boolean valid) {
    this.valid = valid;
  }

  public CouponValidationResponse discount(Integer discount) {
    this.discount = discount;
    return this;
  }

  /**
   * Get discount
   * @return discount
  */
  
  @JsonProperty("discount")
  public Integer getDiscount() {
    return discount;
  }

  public void setDiscount(Integer discount) {
    this.discount = discount;
  }

  public CouponValidationResponse message(String message) {
    this.message = message;
    return this;
  }

  /**
   * Get message
   * @return message
  */
  
  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CouponValidationResponse couponValidationResponse = (CouponValidationResponse) o;
    return Objects.equals(this.valid, couponValidationResponse.valid) &&
        Objects.equals(this.discount, couponValidationResponse.discount) &&
        Objects.equals(this.message, couponValidationResponse.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(valid, discount, message);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CouponValidationResponse {\n");
    sb.append("    valid: ").append(toIndentedString(valid)).append("\n");
    sb.append("    discount: ").append(toIndentedString(discount)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
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

