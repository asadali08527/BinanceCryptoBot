package com.bcb.config;

public class AppConfig {
	private Integer iterationCount = null;
	private Integer openOrderThreshold = null;
	private Integer openPositionThreshold = null;
	private boolean isToIncreamentDecreamentOpenOrderandPosition;
	public AppConfig(Integer openOrderThreshold, Integer openPositionThreshold, Integer iteration, Boolean isToIncreamentDecreamentOpenOrderandPosition) {
		super();
		this.isToIncreamentDecreamentOpenOrderandPosition = isToIncreamentDecreamentOpenOrderandPosition;
		this.iterationCount = iteration;
		this.openOrderThreshold = openOrderThreshold;
		this.openPositionThreshold = openPositionThreshold;
	}
	public Integer getOpenOrderThreshold() {
		return openOrderThreshold;
	}
	public void setOpenOrderThreshold(Integer openOrderThreshold) {
		this.openOrderThreshold = openOrderThreshold;
	}
	public Integer getOpenPositionThreshold() {
		return openPositionThreshold;
	}
	public void setOpenPositionThreshold(Integer openPositionThreshold) {
		this.openPositionThreshold = openPositionThreshold;
	}
	public Integer getIterationCount() {
		return iterationCount;
	}
	public void setIterationCount(Integer iterationCount) {
		this.iterationCount = iterationCount;
	}
	public boolean isToIncreamentDecreamentOpenOrderandPosition() {
		return isToIncreamentDecreamentOpenOrderandPosition;
	}
	public void setToIncreamentDecreamentOpenOrderandPosition(boolean isToIncreamentDecreamentOpenOrderandPosition) {
		this.isToIncreamentDecreamentOpenOrderandPosition = isToIncreamentDecreamentOpenOrderandPosition;
	}
	
}
