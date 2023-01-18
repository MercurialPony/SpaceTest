package melonslise.spacetest.util;

import net.minecraft.util.math.Quaternion;

public final class QuatMath
{
	public static float dot(Quaternion a, Quaternion b)
	{
		return a.getX() * b.getX() + a.getY() * b.getY() + a.getZ() * b.getZ() + a.getW() * b.getW();
	}

	public static void lerp(Quaternion target, Quaternion from, Quaternion to, float t)
	{
		float dot = dot(from, to);

		float omt = 1.0f - t;

		if(dot < 0.0f)
		{
			t *= -1.0f;
		}

		target.set(
			omt * from.getX() + t * to.getX(),
			omt * from.getY() + t * to.getY(),
			omt * from.getZ() + t * to.getZ(),
			omt * from.getW() + t * to.getW());
	}

	public static void nlerp(Quaternion target, Quaternion from, Quaternion to, float t)
	{
		lerp(target, from, to, t);
		target.normalize();
	}
}