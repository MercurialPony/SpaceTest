package melonslise.spacetest.common.blockentity;

import melonslise.immptl.client.ClientRenderLoader;

/**
 * Interface for getting a block entity's renderloader, client-side
 */
public interface BlockEntityWithClientRenderLoader {
    public ClientRenderLoader getRenderLoader();
}
