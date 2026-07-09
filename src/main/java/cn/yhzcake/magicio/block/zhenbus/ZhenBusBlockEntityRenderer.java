package cn.yhzcake.magicio.block.zhenbus;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public class ZhenBusBlockEntityRenderer implements BlockEntityRenderer<ZhenBusBlockEntity, ZhenBusRenderState> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("magic_io", "textures/block/magic_circle.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);

    private static final float OFF = 0.001f;

    public ZhenBusBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public ZhenBusRenderState createRenderState() {
        return new ZhenBusRenderState();
    }

    @Override
    public void extractRenderState(
            ZhenBusBlockEntity blockEntity, ZhenBusRenderState state,
            float tickProgress, Vec3 cameraPos,
            ModelFeatureRenderer.@org.jspecify.annotations.Nullable CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);
        state.activeFaces.clear();
        ZhenBusStorage storage = blockEntity.getContainer().getStorage();
        for (Direction dir : Direction.values()) {
            if (storage.has(dir)) {
                state.activeFaces.add(dir);
            }
        }
    }

    @Override
    public void submit(ZhenBusRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (state.activeFaces.isEmpty()) return;

        int light = state.lightCoords;
        int overlay = OverlayTexture.NO_OVERLAY;
        float r = 1f, g = 1f, b = 1f, a = 1f;

        for (Direction dir : state.activeFaces) {
            poseStack.pushPose();
            collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
                switch (dir) {
                    case DOWN -> {
                        float y = OFF;
                        quadY(pose, consumer, y, light, overlay, r, g, b, a, -1);
                    }
                    case UP -> {
                        float y = 1 - OFF;
                        quadY(pose, consumer, y, light, overlay, r, g, b, a, 1);
                    }
                    case NORTH -> {
                        float z = OFF;
                        quadZ(pose, consumer, z, light, overlay, r, g, b, a, -1);
                    }
                    case SOUTH -> {
                        float z = 1 - OFF;
                        quadZ(pose, consumer, z, light, overlay, r, g, b, a, 1);
                    }
                    case WEST -> {
                        float x = OFF;
                        quadX(pose, consumer, x, light, overlay, r, g, b, a, -1);
                    }
                    case EAST -> {
                        float x = 1 - OFF;
                        quadX(pose, consumer, x, light, overlay, r, g, b, a, 1);
                    }
                }
            });
            poseStack.popPose();
        }
    }

    /** Y平面 (底面/顶面): y=固定 */
    private static void quadY(PoseStack.Pose pose, VertexConsumer c,
                               float y, int light, int overlay,
                               float r, float g, float b, float a, int sign) {
        c.addVertex(pose, 0, y, 0).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, sign, 0);
        c.addVertex(pose, 1, y, 0).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, sign, 0);
        c.addVertex(pose, 1, y, 1).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, sign, 0);
        c.addVertex(pose, 0, y, 1).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, sign, 0);
    }

    /** Z平面 (北/南面): z=固定 */
    private static void quadZ(PoseStack.Pose pose, VertexConsumer c,
                               float z, int light, int overlay,
                               float r, float g, float b, float a, int sign) {
        c.addVertex(pose, 0, 0, z).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, sign);
        c.addVertex(pose, 1, 0, z).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, sign);
        c.addVertex(pose, 1, 1, z).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, sign);
        c.addVertex(pose, 0, 1, z).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, sign);
    }

    /** X平面 (西/东面): x=固定 */
    private static void quadX(PoseStack.Pose pose, VertexConsumer c,
                               float x, int light, int overlay,
                               float r, float g, float b, float a, int sign) {
        c.addVertex(pose, x, 0, 0).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(pose, sign, 0, 0);
        c.addVertex(pose, x, 0, 1).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(pose, sign, 0, 0);
        c.addVertex(pose, x, 1, 1).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(pose, sign, 0, 0);
        c.addVertex(pose, x, 1, 0).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, sign, 0, 0);
    }
}
