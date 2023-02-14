package melonslise.spacetest.test;

import melonslise.spacetest.core.planets.CubemapFace;
import melonslise.spacetest.core.planets.PlanetProjection;
import melonslise.spacetest.core.seamless_worldgen.noise.Noise4dSampler;
import melonslise.spacetest.core.seamless_worldgen.noise.PerlinNoise4d;
import net.minecraft.util.math.noise.PerlinNoiseSampler;
import net.minecraft.util.math.random.ChunkRandom;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.joml.Vector3f;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class TestThing
{
	public static void noiseTest()
	{
				/*
		java.util.Random r = new java.util.Random(3228);

		//PerlinNoiseSampler sampler = new PerlinNoiseSampler(ChunkRandom.RandomProvider.XOROSHIRO.create(3228));
		PerlinNoise4dSampler sampler = new PerlinNoise4dSampler(ChunkRandom.RandomProvider.XOROSHIRO.create(3228));

		double min = 0.0d;
		double max = 0.0d;

		long startTime = System.nanoTime();

		for(int i = 0; i < 10000000; ++i)
		{
			double d = sampler.sample(r.nextDouble() * 1000000.0d, r.nextDouble() * 1000000.0d, r.nextDouble() * 1000000.0d, 0d);

			if(d < min)
			{
				min = d;
			}

			if(d > max)
			{
				max = d;
			}
		}

		long stopTime = System.nanoTime();

		System.out.println("[" + min + ", " + max + "]");
		System.out.println("Time taken: " + (stopTime - startTime) / 1_000_000_000.0d);

		 */

		PerlinNoiseSampler sampler3d = new PerlinNoiseSampler(ChunkRandom.RandomProvider.XOROSHIRO.create(3228));
		Noise4dSampler sampler4d = PerlinNoise4d::sample; // new PerlinNoise4d(ChunkRandom.RandomProvider.XOROSHIRO.create(3228));

		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setSize(800, 600);

		MutableDouble t = new MutableDouble(0.0d);

		BufferedImage img = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_3BYTE_BGR);

		JPanel pane = new JPanel()
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);

				long start = System.nanoTime();

				for(int x = 0; x < frame.getWidth(); ++x)
				{
					for(int y = 0; y < frame.getHeight(); ++y)
					{
						double sc = 0.025f;
						double nx = x * sc;
						double ny = y * sc;
						double nt = t.getValue() * sc;

						float noise = (float) (x < frame.getWidth() / 2 ? sampler3d.sample(nx, ny, nt) : sampler4d.sample(nx, ny, 0.0d, nt));
						noise = noise * 0.5f + 0.5f;

						if(noise < 0.0d || noise > 1.0d)
						{
							System.out.println(noise);
						}

						img.setRGB(x, y, new Color(noise, noise, noise).getRGB());
						//g.setColor(new Color(noise, noise, noise));
						//g.drawRect(x, y, 1, 1);
					}
				}

				g.drawImage(img, 0, 0, null);

				long end = System.nanoTime();

				float frameDuration = (end - start) / 1_000_000_000.0f;
				float fps = 1.0f / frameDuration;

				g.setColor(Color.RED);
				g.drawString("FPS: " + fps, 0, 10);
			}
		};

		frame.add(pane);

		Timer timer = new Timer(33, e ->
		{
			t.add(1.0d);
			pane.repaint();
		});
		timer.start();

		frame.setVisible(true);
	}

	public static void mappingTest()
	{
		Vector3f out = PlanetProjection.uvToCube(CubemapFace.NORTH, new Vector3f(9f, 10.0f, 9f).div(16f));
		System.out.println(out.mul(8f));


		out = PlanetProjection.uvToCube(CubemapFace.SOUTH, new Vector3f(7f, 10.0f, 7f).div(16f));
		System.out.println(out.mul(8f));
	}

	public static void main(String[] args)
	{
		noiseTest();
	}
}