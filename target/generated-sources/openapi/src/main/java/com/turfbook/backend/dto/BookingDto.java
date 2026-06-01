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
 * BookingDto
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class BookingDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long id;

  private Long playerId;

  private String playerName;

  private Long venueId;

  private String venueName;

  private Long courtId;

  private String courtName;

  private Long slotId;

  private String sport;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate date;

  private String startTime;

  private String endTime;

  private Integer amount;

  private Integer convenienceFee;

  private Integer discount;

  private Integer commission;

  private String status;

  private String paymentStatus;

  private String couponCode;

  private Boolean hasReview;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public BookingDto id(Long id) {
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

  public BookingDto playerId(Long playerId) {
    this.playerId = playerId;
    return this;
  }

  /**
   * Get playerId
   * @return playerId
  */
  
  @JsonProperty("playerId")
  public Long getPlayerId() {
    return playerId;
  }

  public void setPlayerId(Long playerId) {
    this.playerId = playerId;
  }

  public BookingDto playerName(String playerName) {
    this.playerName = playerName;
    return this;
  }

  /**
   * Get playerName
   * @return playerName
  */
  
  @JsonProperty("playerName")
  public String getPlayerName() {
    return playerName;
  }

  public void setPlayerName(String playerName) {
    this.playerName = playerName;
  }

  public BookingDto venueId(Long venueId) {
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

  public BookingDto venueName(String venueName) {
    this.venueName = venueName;
    return this;
  }

  /**
   * Get venueName
   * @return venueName
  */
  
  @JsonProperty("venueName")
  public String getVenueName() {
    return venueName;
  }

  public void setVenueName(String venueName) {
    this.venueName = venueName;
  }

  public BookingDto courtId(Long courtId) {
    this.courtId = courtId;
    return this;
  }

  /**
   * Get courtId
   * @return courtId
  */
  
  @JsonProperty("courtId")
  public Long getCourtId() {
    return courtId;
  }

  public void setCourtId(Long courtId) {
    this.courtId = courtId;
  }

  public BookingDto courtName(String courtName) {
    this.courtName = courtName;
    return this;
  }

  /**
   * Get courtName
   * @return courtName
  */
  
  @JsonProperty("courtName")
  public String getCourtName() {
    return courtName;
  }

  public void setCourtName(String courtName) {
    this.courtName = courtName;
  }

  public BookingDto slotId(Long slotId) {
    this.slotId = slotId;
    return this;
  }

  /**
   * Get slotId
   * @return slotId
  */
  
  @JsonProperty("slotId")
  public Long getSlotId() {
    return slotId;
  }

  public void setSlotId(Long slotId) {
    this.slotId = slotId;
  }

  public BookingDto sport(String sport) {
    this.sport = sport;
    return this;
  }

  /**
   * Get sport
   * @return sport
  */
  
  @JsonProperty("sport")
  public String getSport() {
    return sport;
  }

  public void setSport(String sport) {
    this.sport = sport;
  }

  public BookingDto date(LocalDate date) {
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

  public BookingDto startTime(String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Get startTime
   * @return startTime
  */
  
  @JsonProperty("startTime")
  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public BookingDto endTime(String endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * Get endTime
   * @return endTime
  */
  
  @JsonProperty("endTime")
  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public BookingDto amount(Integer amount) {
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

  public BookingDto convenienceFee(Integer convenienceFee) {
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

  public BookingDto discount(Integer discount) {
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

  public BookingDto commission(Integer commission) {
    this.commission = commission;
    return this;
  }

  /**
   * Get commission
   * @return commission
  */
  
  @JsonProperty("commission")
  public Integer getCommission() {
    return commission;
  }

  public void setCommission(Integer commission) {
    this.commission = commission;
  }

  public BookingDto status(String status) {
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

  public BookingDto paymentStatus(String paymentStatus) {
    this.paymentStatus = paymentStatus;
    return this;
  }

  /**
   * Get paymentStatus
   * @return paymentStatus
  */
  
  @JsonProperty("paymentStatus")
  public String getPaymentStatus() {
    return paymentStatus;
  }

  public void setPaymentStatus(String paymentStatus) {
    this.paymentStatus = paymentStatus;
  }

  public BookingDto couponCode(String couponCode) {
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

  public BookingDto hasReview(Boolean hasReview) {
    this.hasReview = hasReview;
    return this;
  }

  /**
   * Get hasReview
   * @return hasReview
  */
  
  @JsonProperty("hasReview")
  public Boolean getHasReview() {
    return hasReview;
  }

  public void setHasReview(Boolean hasReview) {
    this.hasReview = hasReview;
  }

  public BookingDto createdAt(OffsetDateTime createdAt) {
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
    BookingDto bookingDto = (BookingDto) o;
    return Objects.equals(this.id, bookingDto.id) &&
        Objects.equals(this.playerId, bookingDto.playerId) &&
        Objects.equals(this.playerName, bookingDto.playerName) &&
        Objects.equals(this.venueId, bookingDto.venueId) &&
        Objects.equals(this.venueName, bookingDto.venueName) &&
        Objects.equals(this.courtId, bookingDto.courtId) &&
        Objects.equals(this.courtName, bookingDto.courtName) &&
        Objects.equals(this.slotId, bookingDto.slotId) &&
        Objects.equals(this.sport, bookingDto.sport) &&
        Objects.equals(this.date, bookingDto.date) &&
        Objects.equals(this.startTime, bookingDto.startTime) &&
        Objects.equals(this.endTime, bookingDto.endTime) &&
        Objects.equals(this.amount, bookingDto.amount) &&
        Objects.equals(this.convenienceFee, bookingDto.convenienceFee) &&
        Objects.equals(this.discount, bookingDto.discount) &&
        Objects.equals(this.commission, bookingDto.commission) &&
        Objects.equals(this.status, bookingDto.status) &&
        Objects.equals(this.paymentStatus, bookingDto.paymentStatus) &&
        Objects.equals(this.couponCode, bookingDto.couponCode) &&
        Objects.equals(this.hasReview, bookingDto.hasReview) &&
        Objects.equals(this.createdAt, bookingDto.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, playerId, playerName, venueId, venueName, courtId, courtName, slotId, sport, date, startTime, endTime, amount, convenienceFee, discount, commission, status, paymentStatus, couponCode, hasReview, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingDto {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    playerId: ").append(toIndentedString(playerId)).append("\n");
    sb.append("    playerName: ").append(toIndentedString(playerName)).append("\n");
    sb.append("    venueId: ").append(toIndentedString(venueId)).append("\n");
    sb.append("    venueName: ").append(toIndentedString(venueName)).append("\n");
    sb.append("    courtId: ").append(toIndentedString(courtId)).append("\n");
    sb.append("    courtName: ").append(toIndentedString(courtName)).append("\n");
    sb.append("    slotId: ").append(toIndentedString(slotId)).append("\n");
    sb.append("    sport: ").append(toIndentedString(sport)).append("\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
    sb.append("    convenienceFee: ").append(toIndentedString(convenienceFee)).append("\n");
    sb.append("    discount: ").append(toIndentedString(discount)).append("\n");
    sb.append("    commission: ").append(toIndentedString(commission)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    paymentStatus: ").append(toIndentedString(paymentStatus)).append("\n");
    sb.append("    couponCode: ").append(toIndentedString(couponCode)).append("\n");
    sb.append("    hasReview: ").append(toIndentedString(hasReview)).append("\n");
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

