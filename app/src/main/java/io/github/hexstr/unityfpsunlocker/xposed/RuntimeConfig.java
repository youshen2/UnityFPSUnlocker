package io.github.hexstr.UnityFPSUnlocker.xposed;

public final class RuntimeConfig {
    public final int displayModeId;
    public final int delay;
    public final int fps;
    public final boolean modifyOpcode;
    public final float scale;

    RuntimeConfig(
            int displayModeId,
            int delay,
            int fps,
            boolean modifyOpcode,
            float scale) {
        this.displayModeId = displayModeId;
        this.delay = delay;
        this.fps = fps;
        this.modifyOpcode = modifyOpcode;
        this.scale = scale;
    }
}
