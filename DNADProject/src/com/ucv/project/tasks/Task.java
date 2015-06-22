package com.ucv.project.tasks;

import javax.xml.bind.annotation.XmlElement;

import java.util.GregorianCalendar;

public class Task {
  private String code;
  private double complexity;
  private GregorianCalendar date;
  private long estimatedProcessorsNeeded;
  private long estimatedMemoryNeeded;

  public Task() {
  }

  
  public Task(String code, double complexity, GregorianCalendar date, long estimatedProcessorsNeeded,
      long estimatedMemoryNeeded) {
    super();
    this.code = code;
    this.complexity = complexity;
    this.date = date;
    this.estimatedProcessorsNeeded = estimatedProcessorsNeeded;
    this.estimatedMemoryNeeded = estimatedMemoryNeeded;
  }


  public Task(String code, double complexity, GregorianCalendar date) {
    this.code = code;
    this.complexity = complexity;
    this.date = date;
  }

  public String getCode() {
    return code;
  }

  @XmlElement
  public void setCode(String code) {
    this.code = code;
  }

  public double getComplexity() {
    return complexity;
  }

  @XmlElement
  public void setComplexity(double complexity) {
    this.complexity = complexity;
  }

  public GregorianCalendar getDate() {
    return date;
  }

  @XmlElement
  public void setDate(GregorianCalendar date) {
    this.date = date;
  }

  @Override
  public String toString() {
    return "ID " + code + ", " + complexity + "C" + ", " + date.getTime();
  }


  public long getEstimatedProcessorsNeeded() {
    return estimatedProcessorsNeeded;
  }

  @XmlElement
  public void setEstimatedProcessorsNeeded(long estimatedProcessorsNeeded) {
    this.estimatedProcessorsNeeded = estimatedProcessorsNeeded;
  }


  public long getEstimatedMemoryNeeded() {
    return estimatedMemoryNeeded;
  }

  @XmlElement
  public void setEstimatedMemoryNeeded(long estimatedMemoryNeeded) {
    this.estimatedMemoryNeeded = estimatedMemoryNeeded;
  }
  
}