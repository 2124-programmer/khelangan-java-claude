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
 * CourtDto
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class CourtDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long id;

  private Long venueId;

  private String name;

  private Long sportId;

  private String type;

  private Integer pricePerSlot;

  private Integer peakPrice;

  public CourtDto id(Long id) {
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

  public CourtDto venueId(Long venueId) {
    this.venueId = venueId;
    return this;
  }

  /**
   * Get venueId
   * @return venueId
  */
  
  @JsonProperty("venueId")
  public Long getVenueId() {
    return venueId;
  }

  public void setVenueId(Long venueId) {
    this.venueId = venueId;
  }

  public CourtDto name(String name) {
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

  public CourtDto sportId(Long sportId) {
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

  public CourtDto type(String type) {
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

  public CourtDto pricePerSlot(Integer pricePerSlot) {
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

  public CourtDto peakPrice(Integer peakPrice) {
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
    CourtDto courtDto = (CourtDto) o;
    return Objects.equals(this.id, courtDto.id) &&
        Objects.equals(this.venueId, courtDto.venueId) &&
        Objects.equals(this.name, courtDto.name) &&
        Objects.equals(this.sportId, courtDto.sportId) &&
        Objects.equals(this.type, courtDto.type) &&
        Objects.equals(this.pricePerSlot, courtDto.pricePerSlot) &&
        Objects.equals(this.peakPrice, courtDto.peakPrice);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, venueId, name, sportId, type, pricePerSlot, peakPrice);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CourtDto {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    venueId: ").append(toIndentedString(venueId)).append("\n");
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

