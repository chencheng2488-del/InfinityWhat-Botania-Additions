package com.bandybus.infwhat.botania.device;

import java.util.List;

/** Snapshot sent to the client while an accelerator GUI is open. */
public final class AcceleratorState {

	private final boolean enabled;
	private final boolean featureEnabled;
	private final int rate;
	private final int cooldown;
	private final int reachX;
	private final int reachY;
	private final int reachZ;
	private final int offsetX;
	private final int offsetY;
	private final int offsetZ;
	private final boolean showRange;
	private final List<AcceleratorTarget> targets;

	public AcceleratorState(boolean enabled, boolean featureEnabled, int rate, int cooldown,
							int reachX, int reachY, int reachZ,
							int offsetX, int offsetY, int offsetZ, boolean showRange,
							List<AcceleratorTarget> targets) {
		this.enabled = enabled;
		this.featureEnabled = featureEnabled;
		this.rate = rate;
		this.cooldown = cooldown;
		this.reachX = reachX;
		this.reachY = reachY;
		this.reachZ = reachZ;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.offsetZ = offsetZ;
		this.showRange = showRange;
		this.targets = targets;
	}

	public boolean enabled() {
		return enabled;
	}

	public boolean featureEnabled() {
		return featureEnabled;
	}

	public int rate() {
		return rate;
	}

	public int cooldown() {
		return cooldown;
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

	public List<AcceleratorTarget> targets() {
		return targets;
	}
}
