package melonslise.spacetest.planet;

import melonslise.spacetest.util.GeneralUtil;
import net.minecraft.util.math.*;

// FIXME use doubles
// FIXME potentially use faster pow and log (approximate) algos / or cache

/**
 * Functions for converting between space-space (lol) and planet-space
 * IMPORTANT: All of these functions modify the input vector
 *
 * There's 2 main coordinate systems here:
 * 1. Planet space - the absolute coordinates of an object inside the (flat) planet dimension
 * 2. Space space - the absolute coordinates of an object in the space dimension
 *
 * How does this mod map a flat plane world onto a sphere?
 * 1. First the world is divided into 6 square regions - these are faces of a cubemap
 * 2. Each y layer of every face is then scaled and folded into a [-1, 1] cube
 * 3. The cube is then squished into a unit sphere
 * 4. Each sphere is then scaled by a (specially calculated) radius to give each layer height
 *
 * The radius of each layer needs to be exponential because otherwise lower blocks get stretched and higher blocks get squished
 * So a formula to remap the height was derived to maintain block aspect ratio
 * (HINT: block (segment) arc length = radius difference between its top and bottom y)
 */
public final class PlanetProjection
{
	public static final double I_SQRT2 = 0.70710676908493042d;


	/**
	 * Maps a point in face-space to space-space given the planet properties and face which it is on
	 *
	 * Face-space is another one of the many local coordinate systems involved in the remapping process
	 * It is the coordinates relative to the corner of the face (e.g. if the face size is 16 blocks then X and Z will never exceed that)
	 */
	public static Vec3f faceToSpace(PlanetProperties planetProps, CubemapFace face, Vec3f pos)
	{
		float height = pos.getY();

		// convert xz to uv [0, 1]
		pos.scale(1.0f / planetProps.getFaceSize() / 16.0f);
		// convert uv to [-1, 1] cube
		uvToCube(face, pos);
		// convert cube to UNIT sphere
		cubeToSphere(pos);
		// give the sphere a calculated radius depending on the initial height of the point
		pos.scale(heightToRadius(planetProps, height));

		// transform from planet-center-relative-space to world-space (since the above sphere is centered at 0, 0)
		Vec3d planetPos = planetProps.getPosition();
		pos.add((float) planetPos.x, (float) planetPos.y, (float) planetPos.z);

		return pos;
	}

	// FIXME: THIS BUGS ON EDGES HARD
	/**
	 * Maps a point in planet-space to space-space given the planet properties
	 *
	 * Works as described above. Does the same thing as {@link #faceToSpace(PlanetProperties, CubemapFace, Vec3f)} but also determines which face the point is on right away
	 */
	public static Vec3f planetToSpace(PlanetProperties planetProps, Vec3f pos)
	{
		ChunkSectionPos origin = planetProps.getOrigin();
		int faceSizeBlocks = planetProps.getFaceSize() * 16;

		// transform the point to be relative to planet origin
		pos.add(-origin.getMinX(), -origin.getMinY(), -origin.getMinZ());
		// determine which face it is on
		CubemapFace face = CubemapFace.from(MathHelper.floor(pos.getX() / faceSizeBlocks), MathHelper.floor(pos.getZ() / faceSizeBlocks));
		// find the local face-space coordinates
		pos.add(-face.planeOffsetX * faceSizeBlocks, 0.0f, -face.planeOffsetZ * faceSizeBlocks);

		// do the rest
		faceToSpace(planetProps, face, pos);

		return pos;
	}



	/**
	 * Maps a point in space-space to planet-space relative to a planet (given its properties)
	 *
	 * In order to do this it's required to:
	 * 1. Reverse the cube->sphere mapping. So, shrink the sphere to a unit one (but saving the radius for later) and transform to a [-1, 1] cube
	 * 2. Reverse the cube->cubemap mapping. Determine the face the point is on and correctly map it onto that face
	 * 3. Transform the uv back into planet-world coordinates and reverse the previously saved radius to find the correct height
	 */
	public static Vec3f spaceToPlanet(PlanetProperties planetProps, Vec3f pos)
	{
		// Move from absolute coords to planet-center-relative-space
		// so that the sphere center is back to (0, 0) and normalization will properly scale it (among other things)
		Vec3d planetPos = planetProps.getPosition();
		pos.add((float) -planetPos.x, (float) -planetPos.y, (float) -planetPos.z);

		// save the radius for later and downscale the sphere by normalizing the point
		float radius = MathHelper.sqrt(pos.dot(pos));
		pos.scale(1.0f / radius);

		// transform to [-1, 1] cube
		sphereToCube(pos);
		// determine face and transform to [0, 1] uv
		cubeToUv(pos);
		// finally calculate the proper XZ coordinates
		uvToPlane(planetProps.getOrigin(), planetProps.getFaceSize(), pos);
		// don't forget to find the right height by reversing the radius calculation
		pos.add(0.0f, radiusToHeight(planetProps, radius), 0.0f);

		return pos;
	}



	/*
	 * ================ Steps for planet-to-space remapping ================
	 */



	/**
	 * Finds the planet-space height from the space-space height (planet layer radius)
	 */
	private static float heightToRadius(PlanetProperties planetProps, float height)
	{
		return planetProps.getStartRadius() * (float) Math.pow(planetProps.getRadiusRatio(), height);
	}

	// Thank you
	// https://en.wikipedia.org/wiki/Cube_mapping
	/**
	 * Maps a point on the face of a cubemap to a [-1, 1] cube (ignores the y component of the uv vec)
	 * @return point on the surface of a [-1, 1] cube
	 */
	private static Vec3f uvToCube(CubemapFace face, Vec3f uv) // mirrors the same function in the shader
	{
		uv.scale(2.0f);
		uv.add(-1.0f, -1.0f, -1.0f);

		switch (face)
		{
			case NORTH -> uv.set(uv.getX(), uv.getZ(), -1.0f);
			case SOUTH -> uv.set(-uv.getX(), uv.getZ(), 1.0f);
			case EAST -> uv.set(1.0f, uv.getZ(), uv.getX());
			case WEST -> uv.set(-1.0f, uv.getZ(), -uv.getX());
			case UP -> uv.set(uv.getX(), 1.0f, uv.getZ());
			case DOWN -> uv.set(uv.getX(), -1.0f, -uv.getZ());
		}

		return uv;
	}

	// Thanks
	// https://catlikecoding.com/unity/tutorials/cube-sphere/
	/**
	 * Maps a point on a [-1, 1] cube to a unit sphere
	 * @return point on the surface of a unit sphere
	 */
	private static Vec3f cubeToSphere(Vec3f pos)
	{
		float x2 = pos.getX() * pos.getX();
		float y2 = pos.getY() * pos.getY();
		float z2 = pos.getZ() * pos.getZ();

		pos.multiplyComponentwise(
				MathHelper.sqrt(1.0f - (y2 + z2) / 2.0f + y2 * z2 / 3.0f),
				MathHelper.sqrt(1.0f - (x2 + z2) / 2.0f + x2 * z2 / 3.0f),
				MathHelper.sqrt(1.0f - (x2 + y2) / 2.0f + x2 * y2 / 3.0f)
		);

		return pos;
	}



	/*
	 * ================ Steps for space-to-planet remapping ================
	 */



	/**
	 * Remaps the planet-space height to space-space height (planet layer radius)
	 */
	private static float radiusToHeight(PlanetProperties planetProps, float radius)
	{
		return GeneralUtil.log(planetProps.getRadiusRatio(), radius / planetProps.getStartRadius());
	}

	// Thank you
	// https://stackoverflow.com/a/65081330/11734319
	// Modified to work only with unit spheres
	private static Vec2f aux(float s, float t)
	{
		float R = 2.0f * (s * s - t * t);
		float S = MathHelper.sqrt( Math.max(0f, (3.0f + R) * (3.0f + R) - 24.0f * s * s) );
		float s_ = Math.signum(s) * (float) I_SQRT2 * MathHelper.sqrt(Math.max(0f, 3.0f + R - S));
		float t_ = Math.signum(t) * (float) I_SQRT2 * MathHelper.sqrt(Math.max(0f, 3.0f - R - S));
		return new Vec2f(s_, t_);
	}

	/**
	 * Maps a point on a UNIT sphere to a [-1, 1] cube
	 * Important: this expects a normalized point
	 * @return point on the surface of a [-1, 1] cube
	 */
	private static Vec3f sphereToCube(Vec3f pos)
	{
		float ax = Math.abs(pos.getX());
		float ay = Math.abs(pos.getY());
		float az = Math.abs(pos.getZ());
		float max = Math.max(Math.max(ax, ay), az);

		if (max == ax)
		{
			Vec2f aux = aux(pos.getY(), pos.getZ());
			pos.set(Math.signum(pos.getX()), aux.x, aux.y);
			return pos;
		}

		if (max == ay)
		{
			Vec2f aux = aux(pos.getZ(), pos.getX());
			pos.set(aux.y, Math.signum(pos.getY()), aux.x);
			return pos;
		}

		Vec2f aux = aux(pos.getX(), pos.getY());
		pos.set(aux.x, aux.y, Math.signum(pos.getZ()));
		return pos;
	}

	// Thank you
	// https://en.wikipedia.org/wiki/Cube_mapping
	/**
	 * Maps a point from a [-1, 1] cube to a cubemap
	 * @return [0, 1] uv coordinates of the point on the face of a cubemap, and the index of that face as the last component
	 */
	private static Vec3f cubeToUv(Vec3f pos)
	{
		float ax = Math.abs(pos.getX());
		float ay = Math.abs(pos.getY());
		float az = Math.abs(pos.getZ());

		if (az >= ax && az >= ay)
		{
			boolean positive = pos.getZ() > 0.0f;
			pos.set(pos.getX() / az * (positive ? -1.0f : 1.0f), pos.getY() / az, positive ? 1.0f : 0.0f);
		}
		else if (ax >= ay && ax >= az)
		{
			boolean positive = pos.getX() > 0.0f;
			pos.set(pos.getZ() / ax * (positive ? 1.0f : -1.0f), pos.getY() / ax, positive ? 2.0f : 3.0f);
		}
		else
		{
			boolean positive = pos.getY() > 0.0f;
			pos.set(pos.getX() / ay, pos.getZ() / ay * (positive ? 1.0f : -1.0f), positive ? 4.0f : 5.0f);
		}

		pos.multiplyComponentwise(0.5f, 0.5f, 1.0f);
		pos.add(0.5f, 0.5f, 0.0f);

		return pos;
	}

	/**
	 * Maps a [0, 1] uv coordinate to the absolute horizontal (XZ) planet coordinates (without height) given the planet origin, face size and face
	 * IMPORTANT: the last component of the UV vec is used as the face index
	 * IMPORTANT: does not include height
	 * @return absolute horizontal XZ planet coordinates (without height)
	 */
	private static Vec3f uvToPlane(ChunkSectionPos origin, int faceSize, Vec3f uvf)
	{
		CubemapFace face = CubemapFace.values()[(int) uvf.getZ()];

		uvf.set(uvf.getX(), 0.0f, uvf.getY());
		uvf.add(face.planeOffsetX, 0.0f, face.planeOffsetZ);
		uvf.multiplyComponentwise(faceSize * 16.0f, 0.0f, faceSize * 16.0f);
		uvf.add(origin.getMinX(), origin.getMinY(), origin.getMinZ());

		return uvf;
	}
}