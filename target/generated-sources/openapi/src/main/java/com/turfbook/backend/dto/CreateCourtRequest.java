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
 * CreateCourtRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class CreateCourtRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private String name;

  private Long sportId;

  private String type;

  private Integer pricePerSlot;

  private Integer peakPrice = 0;

  public CreateCourtRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateCourtRequest(String name, Long sportId, String type, Integer pricePerSlot) {
    this.name = name;
    this.sportId = sportId;
    this.type = type;
    this.pricePerSlot = pricePerSlot;
  }

  public CreateCourtRequest name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
  */
  @NotNull 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public CreateCourtRequest sportId(Long sportId) {
    this.sportId = sportId;
    return this;
  }

  /**
   * Get sportId
   * @return sportId
  */
  @NotNull 
  @JsonProperty("sportId")
  public Long getSportId() {
    return sportId;
  }

  public void setSportId(Long sportId) {
    this.sportId = sportId;
  }

  public CreateCourtRequest type(String type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
  */
  @NotNull 
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public CreateCourtRequest pricePerSlot(Integer pricePerSlot) {
    this.pricePerSlot = pricePerSlot;
    return this;
  }

  /**
   * Get pricePerSlot
   * @return pricePerSlot
  */
  @NotNull 
  @JsonProperty("pricePerSlot")
  public Integer getPricePerSlot() {
    return pricePerSlot;
  }

  public void setPricePerSlot(Integer pricePerSlot) {
    this.pricePerSlot = pricePerSlot;
  }

  public CreateCourtRequest peakPrice(Integer peakPrice) {
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
    CreateCourtRequest createCourtRequest = (CreateCourtRequest) o;
    return Objects.equals(this.name, createCourtRequest.name) &&
        Objects.equals(this.sportId, createCourtRequest.sportId) &&
        Objects.equals(this.type, createCourtRequest.type) &&
        Objects.equals(this.pricePerSlot, createCourtRequest.pricePerSlot) &&
        Objects.equals(this.peakPrice, createCourtRequest.peakPrice);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, sportId, type, pricePerSlot, peakPrice);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateCourtRequest {\n");
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

