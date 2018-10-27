package com.ef.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(
        name = "log_entry",
        indexes = {@Index(columnList = "ip_address, log_time")})
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "log_time", nullable = false)
    private LocalDateTime logTime;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "request_type", nullable = false)
    private String requestType;

    @Column(name = "http_code", nullable = false)
    private String httpCode;

    @Column(name = "user_agent", nullable = false)
    private String browserInfo;

    @Column(name = "reason_blocked", nullable = false)
    private String reasonBlocked;

    public LogEntry() {
    }

    public LogEntry(LocalDateTime logTime, String ipAddress, String requestType, String httpCode, String browserInfo) {
        this.logTime = logTime;
        this.ipAddress = ipAddress;
        this.requestType = requestType;
        this.httpCode = httpCode;
        this.browserInfo = browserInfo;
    }

    @PrePersist
    public void prePersist() {
        reasonBlocked = String.format("%s request block for making too many requests", getIpAddress());
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getLogTime() {
        return logTime;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getRequestType() {
        return requestType;
    }

    public String getHttpCode() {
        return httpCode;
    }

    public String getBrowserInfo() {
        return browserInfo;
    }

    public String getReasonBlocked() {
        return reasonBlocked;
    }

    @Override
    public String toString() {
        return "LogEntry{"
                + "logTime="
                + logTime
                + ", ipAddress='"
                + ipAddress
                + '\''
                + ", requestType='"
                + requestType
                + '\''
                + ", httpCode='"
                + httpCode
                + '\''
                + ", browserInfo='"
                + browserInfo
                + '\''
                + '}';
    }
}
