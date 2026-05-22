package com.yhzcake.magicio.block.zhenbus;

import java.util.Set;

import net.minecraft.core.Direction;

public record PortBinding(
        Direction hostFace,
        Direction targetFace,
        Set<String> inputZones,
        Set<String> outputZones
) {
    public PortBinding(Direction hostFace, Direction targetFace) {
        this(hostFace, targetFace, Set.of(), Set.of());
    }
}
