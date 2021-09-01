function initializeCoreMod()
{
	var Opcodes = Java.type("org.objectweb.asm.Opcodes");
	var ASMAPI = Java.type("net.minecraftforge.coremod.api.ASMAPI");
	var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
	var renderLevel = ASMAPI.mapMethod("m_109599_");
	var renderDebug = ASMAPI.mapMethod("m_109793_");

	return {
		"LevelRenderer#renderLevel":
		{
			target:
			{
				type: "METHOD",
				class: "net.minecraft.client.renderer.LevelRenderer",
				methodName: renderLevel,
				methodDesc: "(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lcom/mojang/math/Matrix4f;)V"
			},
			transformer: function(methodNode)
			{
				ASMAPI.insertInsnList(
					methodNode,
					ASMAPI.MethodType.VIRTUAL, "net/minecraft/client/renderer/LevelRenderer", renderDebug, "(Lnet/minecraft/client/Camera;)V",
					ASMAPI.listOf(
						new VarInsnNode(Opcodes.ALOAD, 1), // Load MatrixStack (first argument)
						ASMAPI.buildMethodCall("melonslise/spacetest/client/mixin/LevelRendererMixin", "renderPreFabulous", "(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ASMAPI.MethodType.STATIC)
					),
					ASMAPI.InsertMode.INSERT_BEFORE);

				return methodNode;
			}
		}
	};
}