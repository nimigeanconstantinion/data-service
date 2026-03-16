package com.example.data_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
//@Table(name = "messages")
public class MessageEntity {
  @Id
  @Column(length = 36)
  private String id;

  @Column(nullable = false, length = 32)
  private String action;

  @Column(columnDefinition = "TEXT")
  private String payload;

  @Column(nullable = false)
  private Instant updatedAt;

  public MessageEntity() {
  }

  public MessageEntity(String id, String action, String payload, Instant updatedAt) {
    this.id = id;
    this.action = action;
    this.payload = payload;
    this.updatedAt = updatedAt;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
