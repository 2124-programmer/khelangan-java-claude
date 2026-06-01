package com.turfbook.backend.dto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import java.io.Serializable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * PayoutDto
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class PayoutDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long id;

  private Long ownerId;

  private String ownerName;

  private Integer amount;

  private Integer commissionDeducted;

  private Integer netAmount;

  private String status;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate date;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public PayoutDto id(Long id) {
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

  public PayoutDto ownerId(Long ownerId) {
    this.ownerId = ownerId;
    return this;
  }

  /**
   * Get ownerId
   * @return ownerId
  */
  
  @JsonProperty("ownerId")
  public Long getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(Long ownerId) {
    this.ownerId = ownerId;
  }

  public PayoutDto ownerName(String ownerName) {
    this.ownerName = ownerName;
    return this;
  }

  /**
   * Get ownerName
   * @return ownerName
  */
  
  @JsonProperty("ownerName")
  public String getOwnerName() {
    return ownerName;
  }

  public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
  }

  public PayoutDto amount(Integer amount) {
    this.amount = amount;
    return this;
  }

  /**
   * Get amount
   * @return amount
  */
  
  @JsonProperty("amount")
  public Integer getAmount() {
    return amount;
  }

  public void setAmount(Integer amount) {
    this.amount = amount;
  }

  public PayoutDto commissionDeducted(Integer commissionDeducted) {
    this.commissionDeducted = commissionDeducted;
    return this;
  }

  /**
   * Get commissionDeducted
   * @return commissionDeducted
  */
  
  @JsonProperty("commissionDeducted")
  public Integer getCommissionDeducted() {
    return commissionDeducted;
  }

  public void setCommissionDeducted(Integer commissionDeducted) {
    this.commissionDeducted = commissionDeducted;
  }

  public PayoutDto netAmount(Integer netAmount) {
    this.netAmount = netAmount;
    return this;
  }

  /**
   * Get netAmount
   * @return netAmount
  */
  
  @JsonProperty("netAmount")
  public Integer getNetAmount() {
    return netAmount;
  }

  public void setNetAmount(Integer netAmount) {
    this.netAmount = netAmount;
  }

  public PayoutDto status(String status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
  */
  
  @JsonProperty("status")
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public PayoutDto date(LocalDate date) {
    this.date = date;
    return this;
  }

  /**
   * Get date
   * @return date
  */
  @Valid 
  @JsonProperty("date")
  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public PayoutDto createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
  */
  @Valid 
  @JsonProperty("createdAt")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PayoutDto payoutDto = (PayoutDto) o;
    return Objects.equals(this.id, payoutDto.id) &&
        Objects.equals(this.ownerId, payoutDto.ownerId) &&
        Objects.equals(this.ownerName, payoutDto.ownerName) &&
        Objects.equals(this.amount, payoutDto.amount) &&
        Objects.equals(this.commissionDeducted, payoutDto.commissionDeducted) &&
        Objects.equals(this.netAmount, payoutDto.netAmount) &&
        Objects.equals(this.status, payoutDto.status) &&
        Objects.equals(this.date, payoutDto.date) &&
        Objects.equals(this.createdAt, payoutDto.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, ownerId, ownerName, amount, commissionDeducted, netAmount, status, date, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PayoutDto {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    ownerId: ").append(toIndentedString(ownerId)).append("\n");
    sb.append("    ownerName: ").append(toIndentedString(ownerName)).append("\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
    sb.append("    commissionDeducted: ").append(toIndentedString(commissionDeducted)).append("\n");
    sb.append("    netAmount: ").append(toIndentedString(netAmount)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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

