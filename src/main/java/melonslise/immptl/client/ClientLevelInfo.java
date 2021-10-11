package melonslise.immptl.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;

public class ClientLevelInfo {
    public final LevelRenderer renderer;
    public final ClientLevel level;
    public final RenderChunkContainer container;

    public ClientLevelInfo(LevelRenderer renderer, ClientLevel level, RenderChunkContainer container)
    {
        this.renderer = renderer;
        this.level = level;
        this.container = container;
    }

    @Override
    public String toString()
    {
        return "Level Info object. Level: "+this.level+"; Renderer: "+this.renderer+"; RenderChunkContainer: "+this.container;
    }

    public static final ClientLevelInfo emptyInfo = new ClientLevelInfo(null, null, null);
}
