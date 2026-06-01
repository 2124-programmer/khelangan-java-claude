package com.turfbook.backend.dto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import java.io.Serializable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * CreateCouponRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class CreateCouponRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private String code;

  /**
   * Gets or Sets discountType
   */
  public enum DiscountTypeEnum {
    PERCENT("PERCENT"),
    
    FLAT("FLAT");

    private String value;

    DiscountTypeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static DiscountTypeEnum fromValue(String value) {
      for (DiscountTypeEnum b : DiscountTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private DiscountTypeEnum discountType;

  private Integer discountValue;

  private Integer minBooking;

  private Integer maxDiscount;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate validUntil;

  private Integer maxUses;

  public CreateCouponRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateCouponRequest(String code, DiscountTypeEnum discountType, Integer discountValue, Integer minBooking, LocalDate validUntil, Integer maxUses) {
    this.code = code;
    this.discountType = discountType;
    this.discountValue = discountValue;
    this.minBooking = minBooking;
    this.validUntil = validUntil;
    this.maxUses = maxUses;
  }

  public CreateCouponRequest code(String code) {
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

  public CreateCouponRequest discountType(DiscountTypeEnum discountType) {
    this.discountType = discountType;
    return this;
  }

  /**
   * Get discountType
   * @return discountType
  */
  @NotNull 
  @JsonProperty("discountType")
  public DiscountTypeEnum getDiscountType() {
    return discountType;
  }

  public void setDiscountType(DiscountTypeEnum discountType) {
    this.discountType = discountType;
  }

  public CreateCouponRequest discountValue(Integer discountValue) {
    this.discountValue = discountValue;
    return this;
  }

  /**
   * Get discountValue
   * @return discountValue
  */
  @NotNull 
  @JsonProperty("discountValue")
  public Integer getDiscountValue() {
    return discountValue;
  }

  public void setDiscountValue(Integer discountValue) {
    this.discountValue = discountValue;
  }

  public CreateCouponRequest minBooking(Integer minBooking) {
    this.minBooking = minBooking;
    return this;
  }

  /**
   * Get minBooking
   * @return minBooking
  */
  @NotNull 
  @JsonProperty("minBooking")
  public Integer getMinBooking() {
    return minBooking;
  }

  public void setMinBooking(Integer minBooking) {
    this.minBooking = minBooking;
  }

  public CreateCouponRequest maxDiscount(Integer maxDiscount) {
    this.maxDiscount = maxDiscount;
    return this;
  }

  /**
   * Get maxDiscount
   * @return maxDiscount
  */
  
  @JsonProperty("maxDiscount")
  public Integer getMaxDiscount() {
    return maxDiscount;
  }

  public void setMaxDiscount(Integer maxDiscount) {
    this.maxDiscount = maxDiscount;
  }

  public CreateCouponRequest validUntil(LocalDate validUntil) {
    this.validUntil = validUntil;
    return this;
  }

  /**
   * Get validUntil
   * @return validUntil
  */
  @NotNull @Valid 
  @JsonProperty("validUntil")
  public LocalDate getValidUntil() {
    return validUntil;
  }

  public void setValidUntil(LocalDate validUntil) {
    this.validUntil = validUntil;
  }

  public CreateCouponRequest maxUses(Integer maxUses) {
    this.maxUses = maxUses;
    return this;
  }

  /**
   * Get maxUses
   * @return maxUses
  */
  @NotNull 
  @JsonProperty("maxUses")
  public Integer getMaxUses() {
    return maxUses;
  }

  public void setMaxUses(Integer maxUses) {
    this.maxUses = maxUses;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateCouponRequest createCouponRequest = (CreateCouponRequest) o;
    return Objects.equals(this.code, createCouponRequest.code) &&
        Objects.equals(this.discountType, createCouponRequest.discountType) &&
        Objects.equals(this.discountValue, createCouponRequest.discountValue) &&
        Objects.equals(this.minBooking, createCouponRequest.minBooking) &&
        Objects.equals(this.maxDiscount, createCouponRequest.maxDiscount) &&
        Objects.equals(this.validUntil, createCouponRequest.validUntil) &&
        Objects.equals(this.maxUses, createCouponRequest.maxUses);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, discountType, discountValue, minBooking, maxDiscount, validUntil, maxUses);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateCouponRequest {\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    discountType: ").append(toIndentedString(discountType)).append("\n");
    sb.append("    discountValue: ").append(toIndentedString(discountValue)).append("\n");
    sb.append("    minBooking: ").append(toIndentedString(minBooking)).append("\n");
    sb.append("    maxDiscount: ").append(toIndentedString(maxDiscount)).append("\n");
    sb.append("    validUntil: ").append(toIndentedString(validUntil)).append("\n");
    sb.append("    maxUses: ").append(toIndentedString(maxUses)).append("\n");
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

