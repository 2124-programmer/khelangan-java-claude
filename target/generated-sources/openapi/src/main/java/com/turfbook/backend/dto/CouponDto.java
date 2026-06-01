package com.turfbook.backend.dto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import java.io.Serializable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * CouponDto
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class CouponDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long id;

  private String code;

  private String discountType;

  private Integer discountValue;

  private Integer minBooking;

  private Integer maxDiscount;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate validUntil;

  private Integer usedCount;

  private Integer maxUses;

  private Boolean isActive;

  public CouponDto id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
  */
  
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public CouponDto code(String code) {
    this.code = code;
    return this;
  }

  /**
   * Get code
   * @return code
  */
  
  @JsonProperty("code")
  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public CouponDto discountType(String discountType) {
    this.discountType = discountType;
    return this;
  }

  /**
   * Get discountType
   * @return discountType
  */
  
  @JsonProperty("discountType")
  public String getDiscountType() {
    return discountType;
  }

  public void setDiscountType(String discountType) {
    this.discountType = discountType;
  }

  public CouponDto discountValue(Integer discountValue) {
    this.discountValue = discountValue;
    return this;
  }

  /**
   * Get discountValue
   * @return discountValue
  */
  
  @JsonProperty("discountValue")
  public Integer getDiscountValue() {
    return discountValue;
  }

  public void setDiscountValue(Integer discountValue) {
    this.discountValue = discountValue;
  }

  public CouponDto minBooking(Integer minBooking) {
    this.minBooking = minBooking;
    return this;
  }

  /**
   * Get minBooking
   * @return minBooking
  */
  
  @JsonProperty("minBooking")
  public Integer getMinBooking() {
    return minBooking;
  }

  public void setMinBooking(Integer minBooking) {
    this.minBooking = minBooking;
  }

  public CouponDto maxDiscount(Integer maxDiscount) {
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

  public CouponDto validUntil(LocalDate validUntil) {
    this.validUntil = validUntil;
    return this;
  }

  /**
   * Get validUntil
   * @return validUntil
  */
  @Valid 
  @JsonProperty("validUntil")
  public LocalDate getValidUntil() {
    return validUntil;
  }

  public void setValidUntil(LocalDate validUntil) {
    this.validUntil = validUntil;
  }

  public CouponDto usedCount(Integer usedCount) {
    this.usedCount = usedCount;
    return this;
  }

  /**
   * Get usedCount
   * @return usedCount
  */
  
  @JsonProperty("usedCount")
  public Integer getUsedCount() {
    return usedCount;
  }

  public void setUsedCount(Integer usedCount) {
    this.usedCount = usedCount;
  }

  public CouponDto maxUses(Integer maxUses) {
    this.maxUses = maxUses;
    return this;
  }

  /**
   * Get maxUses
   * @return maxUses
  */
  
  @JsonProperty("maxUses")
  public Integer getMaxUses() {
    return maxUses;
  }

  public void setMaxUses(Integer maxUses) {
    this.maxUses = maxUses;
  }

  public CouponDto isActive(Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Get isActive
   * @return isActive
  */
  
  @JsonProperty("isActive")
  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CouponDto couponDto = (CouponDto) o;
    return Objects.equals(this.id, couponDto.id) &&
        Objects.equals(this.code, couponDto.code) &&
        Objects.equals(this.discountType, couponDto.discountType) &&
        Objects.equals(this.discountValue, couponDto.discountValue) &&
        Objects.equals(this.minBooking, couponDto.minBooking) &&
        Objects.equals(this.maxDiscount, couponDto.maxDiscount) &&
        Objects.equals(this.validUntil, couponDto.validUntil) &&
        Objects.equals(this.usedCount, couponDto.usedCount) &&
        Objects.equals(this.maxUses, couponDto.maxUses) &&
        Objects.equals(this.isActive, couponDto.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, code, discountType, discountValue, minBooking, maxDiscount, validUntil, usedCount, maxUses, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CouponDto {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    discountType: ").append(toIndentedString(discountType)).append("\n");
    sb.append("    discountValue: ").append(toIndentedString(discountValue)).append("\n");
    sb.append("    minBooking: ").append(toIndentedString(minBooking)).append("\n");
    sb.append("    maxDiscount: ").append(toIndentedString(maxDiscount)).append("\n");
    sb.append("    validUntil: ").append(toIndentedString(validUntil)).append("\n");
    sb.append("    usedCount: ").append(toIndentedString(usedCount)).append("\n");
    sb.append("    maxUses: ").append(toIndentedString(maxUses)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
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

