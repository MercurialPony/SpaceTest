package melonslise.spacetest.client.render.planet;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class SimplePlanetCuller
{
	/*
	private final Matrix4f mvpInvMat = new Matrix4f();

	public final Vector4f[] vertices = GeneralUtil.fill(new Vector4f[4], i -> new Vector4f());



	// Thank you
	// https://gamedev.stackexchange.com/questions/29999/how-do-i-create-a-bounding-frustum-from-a-view-projection-matrix
	public Vector4f transform(Vector4f vertex, int x, int y, int z)
	{
		vertex.set(x, y, z, 1f);
		vertex.transform(this.mvpInvMat);
		vertex.normalizeProjectiveCoordinates();
		return vertex;
	}

	public void update(Matrix4f viewMat, Matrix4f projMat)
	{
		this.mvpInvMat.load(projMat);
		this.mvpInvMat.multiply(viewMat);
		this.mvpInvMat.invert();

		// https://stackoverflow.com/a/65306627/11734319
		// Generate front cube face vertices, transform them to world space (these are far plane coords)
		for(int i = 0; i < vertices.length; ++i)
		{
			this.transform(this.vertices[i],  GeneralUtil.testBit(i, 0) ? 1 : -1, GeneralUtil.testBit(i, 1) ? 1 : -1, 1);
		}
	}

	// https://samsymons.com/blog/math-notes-ray-plane-intersection/
	public static Vec3f rayPlane(Vec3f rayOrigin, Vec3f rayDirection, Vec3f planeOrigin, Vec3f planeNormal)
	{
		Vec3f delta = rayOrigin.copy();
		delta.subtract(planeOrigin);

		Vec3f intersect = rayDirection.copy();
		intersect.scale(-delta.dot(planeNormal) / rayDirection.dot(planeNormal));
		intersect.add(rayOrigin);

		return intersect;
	}

	public Vec3f[] test(Vec3f center)
	{
		Vec3f camPos = new Vec3f(MinecraftClient.getInstance().gameRenderer.getCamera().getPos());

		center = center.copy();
		center.subtract(camPos);

		Vec3f normal = camPos.copy();
		normal.subtract(center);

		Vec3f[] out = new Vec3f[this.vertices.length];

		for(int i = 0; i < out.length; ++i)
		{
			out[i] = rayPlane(new Vec3f(), new Vec3f(this.vertices[i]), center, normal);
			out[i].add(camPos);
		}

		return out;
	}

	 */
}