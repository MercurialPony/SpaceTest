package melonslise.spacetest.planet;

import melonslise.spacetest.util.GeneralUtil;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;

// FIXME use doubles
// FIXME potentially use faster pow and log (approximate) algos / or cache
// FIXME also replace sqrt and other functions with joml ones

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
	public static Vector3f faceToSpace(PlanetProperties planetProps, PlanetState planetState, CubemapFace face, Vector3f pos)
	{
		float height = pos.y;

		// convert xz to uv [0, 1]
		pos.div(planetProps.getFaceSize() * 16.0f);
		// convert uv to [-1, 1] cube
		uvToCube(face, pos);
		// convert cube to UNIT sphere
		cubeToSphere(pos);
		// give the sphere a calculated radius depending on the initial height of the point
		pos.mul(heightToRadius(planetProps, height));

		// transform from planet-center-relative-space to world-space (since the above sphere is centered at 0, 0)
		pos.rotate(new Quaternionf(planetState.getRotation())); // FIXME ughh object creationnnn
		Vector3d planetPos = planetState.getPosition();
		pos.add((float) planetPos.x, (float) planetPos.y, (float) planetPos.z);

		return pos;
	}

	/**
	 * Maps a point in planet-space to space-space given the planet properties
	 *
	 * Works as described above. Does the same thing as {@link #faceToSpace(PlanetProperties, PlanetState, CubemapFace, Vector3f)} but also determines which face the point is on right away
	 */
	public static Vector3f planetToSpace(PlanetProperties planetProps, PlanetState planetState, Vector3f pos)
	{
		int faceSizeBlocks = planetProps.getFaceSize() * 16;

		// determine the face this point is on
		CubemapFace face = determineFaceInBlocks(planetProps, MathHelper.floor(pos.x), MathHelper.floor(pos.z));
		// find the local face-space coordinates
		pos.sub(face.offsetX * faceSizeBlocks, 0.0f, face.offsetZ * faceSizeBlocks);

		// do the rest
		faceToSpace(planetProps, planetState, face, pos);

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
	public static Vector3f spaceToPlanet(PlanetProperties planetProps, PlanetState planetState, Vector3f pos)
	{
		// Move from absolute coords to planet-center-relative-space
		// so that the sphere center is back to (0, 0) and normalization will properly scale it (among other things)
		Vector3d planetPos = planetState.getPosition();
		pos.sub((float) planetPos.x, (float) planetPos.y, (float) planetPos.z);
		pos.rotate(new Quaternionf(planetState.getRotation()).conjugate()); // FIXME ughh object creationnnn

		// save the radius for later and downscale the sphere by normalizing the point
		float radius = MathHelper.sqrt(pos.dot(pos));
		pos.div(radius);

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
	public static float heightToRadius(PlanetProperties planetProps, float height)
	{
		return planetProps.getStartRadius() * (float) Math.pow(planetProps.getRadiusRatio(), height);
	}

	// FIXME this probably does too
	public static CubemapFace determineFaceInChunks(PlanetProperties planetProps, int x, int z)
	{
		ChunkSectionPos origin = planetProps.getOrigin();
		int faceSizeChunks = planetProps.getFaceSize();

		// transform the point to be relative to planet origin
		// determine which face it is on
		return CubemapFace.from(Math.floorDiv(x - origin.getX(), faceSizeChunks), Math.floorDiv(z - origin.getZ(), faceSizeChunks));
	}

	// FIXME: THIS BUGS ON POSITIVE (16) EDGES HARD
	public static CubemapFace determineFaceInBlocks(PlanetProperties planetProps, int x, int z)
	{
		return determineFaceInChunks(planetProps, ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z));
	}

	// Thank you
	// https://en.wikipedia.org/wiki/Cube_mapping
	/**
	 * Maps a point on the face of a cubemap to a [-1, 1] cube (ignores the y component of the uv vec)
	 * @return point on the surface of a [-1, 1] cube
	 */
	public static Vector3f uvToCube(CubemapFace face, Vector3f uv) // mirrors the same function in the shader
	{
		uv.mul(2.0f);
		uv.add(-1.0f, -1.0f, -1.0f);

		switch (face)
		{
			case NORTH -> uv.set(uv.x, uv.z, -1.0f);
			case SOUTH -> uv.set(-uv.x, uv.z, 1.0f);
			case EAST -> uv.set(1.0f, uv.z, uv.x);
			case WEST -> uv.set(-1.0f, uv.z, -uv.x);
			case UP -> uv.set(uv.x, 1.0f, uv.z);
			case DOWN -> uv.set(uv.x, -1.0f, -uv.z);
		}

		return uv;
	}

	// Thanks
	// https://catlikecoding.com/unity/tutorials/cube-sphere/
	/**
	 * Maps a point on a [-1, 1] cube to a unit sphere
	 * @return point on the surface of a unit sphere
	 */
	private static Vector3f cubeToSphere(Vector3f pos)
	{
		float x2 = pos.x * pos.x;
		float y2 = pos.y * pos.y;
		float z2 = pos.z * pos.z;

		pos.mul(
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
	private static Vector2f aux(float s, float t)
	{
		float R = 2.0f * (s * s - t * t);
		float S = MathHelper.sqrt( Math.max(0f, (3.0f + R) * (3.0f + R) - 24.0f * s * s) );
		float s_ = Math.signum(s) * (float) I_SQRT2 * MathHelper.sqrt(Math.max(0f, 3.0f + R - S));
		float t_ = Math.signum(t) * (float) I_SQRT2 * MathHelper.sqrt(Math.max(0f, 3.0f - R - S));
		return new Vector2f(s_, t_);
	}

	/**
	 * Maps a point on a UNIT sphere to a [-1, 1] cube
	 * Important: this expects a normalized point
	 * @return point on the surface of a [-1, 1] cube
	 */
	private static Vector3f sphereToCube(Vector3f pos)
	{
		float ax = Math.abs(pos.x);
		float ay = Math.abs(pos.y);
		float az = Math.abs(pos.z);
		float max = Math.max(Math.max(ax, ay), az);

		if (max == ax)
		{
			Vector2f aux = aux(pos.y, pos.y);
			pos.set(Math.signum(pos.x), aux.x, aux.y);
			return pos;
		}

		if (max == ay)
		{
			Vector2f aux = aux(pos.z, pos.x);
			pos.set(aux.y, Math.signum(pos.y), aux.x);
			return pos;
		}

		Vector2f aux = aux(pos.x, pos.x);
		pos.set(aux.x, aux.y, Math.signum(pos.z));
		return pos;
	}

	// Thank you
	// https://en.wikipedia.org/wiki/Cube_mapping
	/**
	 * Maps a point from a [-1, 1] cube to a cubemap
	 * @return [0, 1] uv coordinates of the point on the face of a cubemap, and the index of that face as the last component
	 */
	private static Vector3f cubeToUv(Vector3f pos)
	{
		float ax = Math.abs(pos.x);
		float ay = Math.abs(pos.y);
		float az = Math.abs(pos.z);

		if (az >= ax && az >= ay)
		{
			boolean positive = pos.z > 0.0f;
			pos.set(pos.x / az * (positive ? -1.0f : 1.0f), pos.y / az, positive ? 1.0f : 0.0f);
		}
		else if (ax >= ay && ax >= az)
		{
			boolean positive = pos.x > 0.0f;
			pos.set(pos.z / ax * (positive ? 1.0f : -1.0f), pos.y / ax, positive ? 2.0f : 3.0f);
		}
		else
		{
			boolean positive = pos.y > 0.0f;
			pos.set(pos.x / ay, pos.z / ay * (positive ? 1.0f : -1.0f), positive ? 4.0f : 5.0f);
		}

		pos.mul(0.5f, 0.5f, 1.0f);
		pos.add(0.5f, 0.5f, 0.0f);

		return pos;
	}

	/**
	 * Maps a [0, 1] uv coordinate to the absolute horizontal (XZ) planet coordinates (without height) given the planet origin, face size and face
	 * IMPORTANT: the last component of the UV vec is used as the face index
	 * IMPORTANT: does not include height
	 * @return absolute horizontal XZ planet coordinates (without height)
	 */
	private static Vector3f uvToPlane(ChunkSectionPos origin, int faceSize, Vector3f uvf)
	{
		CubemapFace face = CubemapFace.values()[(int) uvf.z];

		uvf.set(uvf.x, 0.0f, uvf.y);
		uvf.add(face.offsetX, 0.0f, face.offsetZ);
		uvf.mul(faceSize * 16.0f, 0.0f, faceSize * 16.0f);
		uvf.add(origin.getMinX(), origin.getMinY(), origin.getMinZ());

		return uvf;
	}
}