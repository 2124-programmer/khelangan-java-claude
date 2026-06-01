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
 * PlatformSettingsDto
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class PlatformSettingsDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long id;

  private Integer commissionPercent;

  private Integer convenienceFee;

  private Boolean maintenanceMode;

  private Boolean autoApproveVenues;

  public PlatformSettingsDto id(Long id) {
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

  public PlatformSettingsDto commissionPercent(Integer commissionPercent) {
    this.commissionPercent = commissionPercent;
    return this;
  }

  /**
   * Get commissionPercent
   * @return commissionPercent
  */
  
  @JsonProperty("commissionPercent")
  public Integer getCommissionPercent() {
    return commissionPercent;
  }

  public void setCommissionPercent(Integer commissionPercent) {
    this.commissionPercent = commissionPercent;
  }

  public PlatformSettingsDto convenienceFee(Integer convenienceFee) {
    this.convenienceFee = convenienceFee;
    return this;
  }

  /**
   * Get convenienceFee
   * @return convenienceFee
  */
  
  @JsonProperty("convenienceFee")
  public Integer getConvenienceFee() {
    return convenienceFee;
  }

  public void setConvenienceFee(Integer convenienceFee) {
    this.convenienceFee = convenienceFee;
  }

  public PlatformSettingsDto maintenanceMode(Boolean maintenanceMode) {
    this.maintenanceMode = maintenanceMode;
    return this;
  }

  /**
   * Get maintenanceMode
   * @return maintenanceMode
  */
  
  @JsonProperty("maintenanceMode")
  public Boolean getMaintenanceMode() {
    return maintenanceMode;
  }

  public void setMaintenanceMode(Boolean maintenanceMode) {
    this.maintenanceMode = maintenanceMode;
  }

  public PlatformSettingsDto autoApproveVenues(Boolean autoApproveVenues) {
    this.autoApproveVenues = autoApproveVenues;
    return this;
  }

  /**
   * Get autoApproveVenues
   * @return autoApproveVenues
  */
  
  @JsonProperty("autoApproveVenues")
  public Boolean getAutoApproveVenues() {
    return autoApproveVenues;
  }

  public void setAutoApproveVenues(Boolean autoApproveVenues) {
    this.autoApproveVenues = autoApproveVenues;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PlatformSettingsDto platformSettingsDto = (PlatformSettingsDto) o;
    return Objects.equals(this.id, platformSettingsDto.id) &&
        Objects.equals(this.commissionPercent, platformSettingsDto.commissionPercent) &&
        Objects.equals(this.convenienceFee, platformSettingsDto.convenienceFee) &&
        Objects.equals(this.maintenanceMode, platformSettingsDto.maintenanceMode) &&
        Objects.equals(this.autoApproveVenues, platformSettingsDto.autoApproveVenues);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, commissionPercent, convenienceFee, maintenanceMode, autoApproveVenues);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PlatformSettingsDto {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    commissionPercent: ").append(toIndentedString(commissionPercent)).append("\n");
    sb.append("    convenienceFee: ").append(toIndentedString(convenienceFee)).append("\n");
    sb.append("    maintenanceMode: ").append(toIndentedString(maintenanceMode)).append("\n");
    sb.append("    autoApproveVenues: ").append(toIndentedString(autoApproveVenues)).append("\n");
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

