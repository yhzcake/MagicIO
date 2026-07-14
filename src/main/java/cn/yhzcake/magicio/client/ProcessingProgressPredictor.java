package cn.yhzcake.magicio.client;

import cn.yhzcake.magicio.item.crafting.ProcessingPhase;
import cn.yhzcake.magicio.item.crafting.ProcessingStateSnapshot;

public final class ProcessingProgressPredictor {
    private ProcessingStateSnapshot anchor;
    private long receivedAtNanos;
    private double ticksPerSecond;

    public void accept(ProcessingStateSnapshot snapshot, long nowNanos) {
        if (anchor != null
                && snapshot.stateRevision() >= anchor.stateRevision()
                && snapshot.cycleId() == anchor.cycleId()
                && snapshot.phase() == ProcessingPhase.RUNNING
                && anchor.phase() == ProcessingPhase.RUNNING) {
            long elapsedNanos = nowNanos - receivedAtNanos;
            int progressed = snapshot.processTime() - anchor.processTime();
            if (elapsedNanos > 0 && progressed >= 0) {
                double observed = progressed * 1_000_000_000.0 / elapsedNanos;
                observed = Math.max(0.0, Math.min(20.0, observed));
                ticksPerSecond = ticksPerSecond == 0.0 ? observed : ticksPerSecond * 0.75 + observed * 0.25;
            }
        }
        if (anchor == null || snapshot.stateRevision() >= anchor.stateRevision()) {
            anchor = snapshot;
            receivedAtNanos = nowNanos;
        }
    }

    public double getDisplayedProgress(long nowNanos) {
        if (anchor == null || anchor.phase() == ProcessingPhase.IDLE || anchor.effectiveProcessingTime() <= 0) return 0.0;
        double progress = anchor.processTime();
        if (anchor.phase() == ProcessingPhase.RUNNING && ticksPerSecond > 0.0) {
            progress += Math.max(0L, nowNanos - receivedAtNanos) / 1_000_000_000.0 * ticksPerSecond;
        }
        return Math.min(progress, Math.max(0, anchor.effectiveProcessingTime() - 1));
    }

    public double getDisplayedFraction(long nowNanos) {
        if (anchor == null || anchor.effectiveProcessingTime() <= 0) return 0.0;
        return getDisplayedProgress(nowNanos) / anchor.effectiveProcessingTime();
    }

    public void reset() {
        anchor = null;
        receivedAtNanos = 0;
        ticksPerSecond = 0.0;
    }
}
