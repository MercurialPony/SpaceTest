package melonslise.mixin.client;

import melonslise.immptl.client.RenderChunkContainer;
import melonslise.immptl.mixinInterfaces.ILevelRenderer_CustomViewArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class Mixin_LevelRenderer_CustomViewArea implements ILevelRenderer_CustomViewArea
{
    @Shadow
    private ClientLevel level;

    @Shadow
    private ChunkRenderDispatcher chunkRenderDispatcher;

    @Shadow
    private Minecraft minecraft;

    private RenderChunkContainer container;

    // TODO move to constructor and change how it's all handled.
    @Inject(
            method = "allChanged",
            at = @At("RETURN")
    )
    public void createRenderContainer(CallbackInfo ci)
    {
        this.container = new RenderChunkContainer(this.level, this.chunkRenderDispatcher, this.minecraft.options.renderDistance);
    }

    @Override
    public RenderChunkContainer getRenderChunkContainer()
    {
        return this.container;
    }
}
