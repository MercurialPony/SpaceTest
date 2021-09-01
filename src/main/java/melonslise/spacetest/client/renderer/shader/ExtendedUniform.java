package melonslise.spacetest.client.renderer.shader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.shaders.Shader;
import com.mojang.blaze3d.shaders.Uniform;

import net.minecraft.server.ChainedJsonException;
import net.minecraft.util.GsonHelper;

public class ExtendedUniform extends Uniform
{
	public final float[] defaults;
	public final float min, max;

	public ExtendedUniform(String name, int type, int size, float[] defaults, float min, float max, Shader shader)
	{
		super(name, type, size, shader);
		this.defaults = defaults;
		this.min = min;
		this.max = max;
	}

	public static ExtendedUniform parse(JsonElement uniformElement, Shader shader) throws ChainedJsonException
	{
		JsonObject uniformObject = GsonHelper.convertToJsonObject(uniformElement, "uniform");
		String name = GsonHelper.getAsString(uniformObject, "name");
		int type = Uniform.getTypeFromString(GsonHelper.getAsString(uniformObject, "type"));
		int size = GsonHelper.getAsInt(uniformObject, "count");
		float[] values = new float[Math.max(size, 16)];
		JsonArray valueArray = GsonHelper.getAsJsonArray(uniformObject, "values");

		if (valueArray.size() != size && valueArray.size() > 1)
			throw new ChainedJsonException("Invalid amount of values specified (expected " + size + ", found " + valueArray.size() + ")");

		int k = 0;

		for (JsonElement valueElement : valueArray)
		{
			try
			{
				values[k++] = GsonHelper.convertToFloat(valueElement, "value");
			}
			catch (Exception exception)
			{
				ChainedJsonException chainedjsonexception = ChainedJsonException.forException(exception);
				chainedjsonexception.prependJsonKey("values[" + k + "]");
				throw chainedjsonexception;
			}
		}

		if (size > 1 && valueArray.size() == 1)
			while (k < size)
				values[k++] = values[0];

		// modifications
		float minValue = Float.MIN_VALUE;
		float maxValue = Float.MAX_VALUE;
		if(uniformObject.has("range"))
		{
			JsonObject rangeObject = GsonHelper.getAsJsonObject(uniformObject, "range");
			minValue = GsonHelper.getAsFloat(rangeObject, "min");
			maxValue = GsonHelper.getAsFloat(rangeObject, "max");
		}
		//

		int i = size > 1 && size <= 4 && type < 8 ? size - 1 : 0;
		//
		ExtendedUniform uniform = new ExtendedUniform(name, type + i, size, values, minValue, maxValue, shader);
		if (type <= 3)
			uniform.setSafe((int) values[0], (int) values[1], (int) values[2], (int) values[3]);
		else if (type <= 7)
			uniform.setSafe(values[0], values[1], values[2], values[3]);
		else
			uniform.set(values);

		return uniform;
	}
}