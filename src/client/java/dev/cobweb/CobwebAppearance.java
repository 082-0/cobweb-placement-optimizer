package dev.cobweb;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

/** Wrap the final resource-pack model rather than overriding its JSON. */
public final class CobwebAppearance implements BlockStateModel {
    private final BlockStateModel delegate;
    private CobwebAppearance(BlockStateModel delegate) { this.delegate = delegate; }

    public static void register() {
        ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake()
            .register(ModelModifier.WRAP_LAST_PHASE, (model, ctx) ->
                ctx.state().isOf(Blocks.COBWEB) ? new CobwebAppearance(model) : model));
    }

    @Override public Sprite particleSprite() { return delegate.particleSprite(); }

    @Override public Sprite particleSprite(BlockRenderView view, BlockPos pos, BlockState state) {
        return delegate.particleSprite(view, pos, state);
    }

    @Override public void emitQuads(QuadEmitter emitter, BlockRenderView view, BlockPos pos,
            BlockState state, Random random, Predicate<Direction> cullTest) {
        emitter.pushTransform(quad -> {
            quad.tintIndex(0);
            return true;
        });
        try {
            delegate.emitQuads(emitter, view, pos, state, random, cullTest);
        } finally {
            emitter.popTransform();
        }
    }

    @Override public void addParts(Random random, List<BlockModelPart> output) {
        List<BlockModelPart> parts = new ArrayList<>();
        delegate.addParts(random, parts);
        for (BlockModelPart part : parts) {
            output.add(new BlockModelPart() {
                @Override public boolean useAmbientOcclusion() { return part.useAmbientOcclusion(); }
                @Override public Sprite particleSprite() { return part.particleSprite(); }
                @Override public List<BakedQuad> getQuads(Direction face) {
                    return part.getQuads(face).stream().map(CobwebAppearance::withTint).toList();
                }
            });
        }
    }

    static BakedQuad withTint(BakedQuad q) {
        return new BakedQuad(q.position0(), q.position1(), q.position2(), q.position3(),
            q.packedUV0(), q.packedUV1(), q.packedUV2(), q.packedUV3(),
            0, q.face(), q.sprite(), q.shade(), q.lightEmission());
    }
}
