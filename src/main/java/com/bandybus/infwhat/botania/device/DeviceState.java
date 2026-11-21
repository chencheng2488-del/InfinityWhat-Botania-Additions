package com.bandybus.infwhat.botania.device;

import java.util.List;

/** Snapshot sent to the client while a device GUI is open. */
public final class DeviceState {

	private final int rate;
	private final int effectiveRate;
	private final boolean enabled;
	private final long buffer;
	private final long bufferMax;
	private final String networkName;
	private final int reachX;
	private final int reachY;
	private final int reachZ;
	private final int offsetX;
	private final int offsetY;
	private final int offsetZ;
	private final boolean showRange;
	private final List<LinkEntry> links;

	public DeviceState(int rate, int effectiveRate, boolean enabled, long buffer, long bufferMax,
					   String networkName, int reachX, int reachY, int reachZ,
					   int offsetX, int offsetY, int offsetZ, boolean showRange,
					   List<LinkEntry> links) {
		this.rate = rate;
		this.effectiveRate = effectiveRate;
		this.enabled = enabled;
		this.buffer = buffer;
		this.bufferMax = bufferMax;
		this.networkName = networkName == null ? "" : networkName;
		this.reachX = reachX;
		this.reachY = reachY;
		this.reachZ = reachZ;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.offsetZ = offsetZ;
		this.showRange = showRange;
		this.links = links;
	}

	public int rate() {
		return rate;
	}

	public int effectiveRate() {
		return effectiveRate;
	}

	public boolean enabled() {
		return enabled;
	}

	public long buffer() {
		return buffer;
	}

	public long bufferMax() {
		return bufferMax;
	}

	public String networkName() {
		return networkName;
	}

	public int reachX() {
		return reachX;
	}

	public int reachY() {
		return reachY;
	}

	public int reachZ() {
		return reachZ;
	}

	public int offsetX() {
		return offsetX;
	}

	public int offsetY() {
		return offsetY;
	}

	public int offsetZ() {
		return offsetZ;
	}

	public boolean showRange() {
		return showRange;
	}

	public List<LinkEntry> links() {
		return links;
	}
}
