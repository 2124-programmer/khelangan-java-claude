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
 * UpdateCourtRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class UpdateCourtRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private String name;

  private Long sportId;

  private String type;

  private Integer pricePerSlot;

  private Integer peakPrice;

  public UpdateCourtRequest name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
  */
  
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public UpdateCourtRequest sportId(Long sportId) {
    this.sportId = sportId;
    return this;
  }

  /**
   * Get sportId
   * @return sportId
  */
  
  @JsonProperty("sportId")
  public Long getSportId() {
    return sportId;
  }

  public void setSportId(Long sportId) {
    this.sportId = sportId;
  }

  public UpdateCourtRequest type(String type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
  */
  
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public UpdateCourtRequest pricePerSlot(Integer pricePerSlot) {
    this.pricePerSlot = pricePerSlot;
    return this;
  }

  /**
   * Get pricePerSlot
   * @return pricePerSlot
  */
  
  @JsonProperty("pricePerSlot")
  public Integer getPricePerSlot() {
    return pricePerSlot;
  }

  public void setPricePerSlot(Integer pricePerSlot) {
    this.pricePerSlot = pricePerSlot;
  }

  public UpdateCourtRequest peakPrice(Integer peakPrice) {
    this.peakPrice = peakPrice;
    return this;
  }

  /**
   * Get peakPrice
   * @return peakPrice
  */
  
  @JsonProperty("peakPrice")
  public Integer getPeakPrice() {
    return peakPrice;
  }

  public void setPeakPrice(Integer peakPrice) {
    this.peakPrice = peakPrice;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateCourtRequest updateCourtRequest = (UpdateCourtRequest) o;
    return Objects.equals(this.name, updateCourtRequest.name) &&
        Objects.equals(this.sportId, updateCourtRequest.sportId) &&
        Objects.equals(this.type, updateCourtRequest.type) &&
        Objects.equals(this.pricePerSlot, updateCourtRequest.pricePerSlot) &&
        Objects.equals(this.peakPrice, updateCourtRequest.peakPrice);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, sportId, type, pricePerSlot, peakPrice);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateCourtRequest {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    sportId: ").append(toIndentedString(sportId)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    pricePerSlot: ").append(toIndentedString(pricePerSlot)).append("\n");
    sb.append("    peakPrice: ").append(toIndentedString(peakPrice)).append("\n");
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

