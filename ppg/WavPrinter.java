import java.io.*;

public class WavPrinter
{
	public static void
	main (String[] inArgs)
		throws Exception
	{
		if (inArgs.length == 0)
		{
			System.err.println ("usage: java WavPrinter wavfile");
			System.exit (1);
		}
		
		FileInputStream	fis = new FileInputStream (inArgs [0]);
		byte[]	buffer = new byte [1024];
		
		int	blockAlign = 0;
		int	bitsPerSample = 0;
		int	numChannels = 0;
		
		// read the RIFF header

		System.out.println ("RIFF literal = " + read4ByteLiteral (fis));
		System.out.println ("chunk size = " + read4ByteInteger (fis));
		System.out.println ("format = " + read4ByteLiteral (fis));

		for (int i = 1; fis.available () > 0; i++)
		{
			byte[]	vsFrame = new byte [3];
			
			String	subChunkType = read4ByteLiteral (fis);
			System.out.println ("subchunk" + i + " ID = " + subChunkType);
			
			int	subChunkSize = read4ByteInteger (fis);
			System.out.println ("subchunk" + i + " size = " + subChunkSize);

			if (subChunkType.equalsIgnoreCase ("fmt "))
			{
				System.out.println ("audio format = " + read2ByteInteger (fis));
				numChannels = read2ByteInteger (fis);
				System.out.println ("num channels = " + numChannels);
				System.out.println ("sample rate = " + read4ByteInteger (fis));
				System.out.println ("byte rate = " + read4ByteInteger (fis));
				blockAlign = read2ByteInteger (fis);
				System.out.println ("block align = " + blockAlign);
				bitsPerSample = read2ByteInteger (fis);
				System.out.println ("bits per sample = " + bitsPerSample);
				
				if (subChunkSize > 16)
				{
					fis.skip (subChunkSize - 16);
				}
			}
			else
			if (subChunkType.equalsIgnoreCase ("data"))
			{
				int	wavFrames = subChunkSize / blockAlign;
				System.out.println ("wavFrames = " + wavFrames);
				
				int[]	samples = new int [wavFrames];
				
				for (int j = 0; j < wavFrames; j++)
				{
					double	doubleSample = 0.0;
					
					if (bitsPerSample == 8)
					{
						int	sample = fis.read ();
						
						// 8-bit samples assumed to be unsigned
						
						doubleSample = (double) sample;
						doubleSample /= 256;
					}
					else
					if (bitsPerSample == 16)
					{
						int	sample = read2ByteInteger (fis);
						
						// honour the sign of the sample
						sample <<= 16;
						sample >>= 16;
						
// System.err.println ("raw sample " + j + " = " + sample);

						// convert to unsigned
						sample += 32768;
						
// System.err.println ("unsigned sample " + j + " = " + sample);

						doubleSample = (double) sample;
						doubleSample /= 65536;
					}
					else
					if (bitsPerSample == 24)
					{
						int	sample = read3ByteInteger (fis);

						// honour the sign of the sample
						sample <<= 8;
						sample >>= 8;

						// convert to unsigned
						sample += 8388608;
						
						doubleSample = (double) sample;
						doubleSample /= 16777216;
					}

					doubleSample *= 4096;
					samples [j] = (int) doubleSample;
				}

				int	vsFrames = 128;
				int	vsIndex = 0;
				int	wavIndex = 0;
				
				// write out the data in P-VS format
				FileOutputStream	fos = new FileOutputStream ("vs-wave.bin");

				if (vsFrames == wavFrames)
				{
					for (int j = 0; j < vsFrames; j++)
					{
						int	vsSample = samples [j];						

						// now make a VS wave frame, 3 nybbles, LSB first
						vsFrame [0] = (byte) (vsSample & 0xf);
						vsFrame [1] = (byte) ((vsSample >> 4) & 0xf);
						vsFrame [2] = (byte) ((vsSample >> 8) & 0xf);
							
						fos.write (vsFrame);
					}
				}
				else
				if (vsFrames < wavFrames)
				{
					for (int j = 0; j < vsFrames; j++)
					{
						double	wavIndexDouble = (((double) j / (double) vsFrames) * (double) wavFrames);
						int	nextWavIndex = (int) Math.ceil (wavIndexDouble);
						nextWavIndex++;
						
						int	total = 0;
						int	numSamples = nextWavIndex - wavIndex;
						
						// average out the extra samples into one
						do
						{
							total += samples [wavIndex++];
						}
						while (wavIndex < nextWavIndex);

						int	vsSample = total / numSamples;						

// System.err.println ("vs sample " + j + " = " + vsSample);

						// now make a VS wave frame, 3 nybbles, LSB first
						vsFrame [0] = (byte) (vsSample & 0xf);
						vsFrame [1] = (byte) ((vsSample >> 4) & 0xf);
						vsFrame [2] = (byte) ((vsSample >> 8) & 0xf);
							
						fos.write (vsFrame);
					}
				}
				else
				{
					for (int j = 0; j < wavFrames; j++)
					{
						double	vsIndexDouble = (((double) j / (double) wavFrames) * (double) vsFrames);
						int	nextVSIndex = (int) Math.ceil (vsIndexDouble);
						nextVSIndex++;
						
						int	bump = (samples [wavIndex + 1] - samples [wavIndex]) / (nextVSIndex - vsIndex);
						
						for (int vsSample = samples [wavIndex]; vsIndex < nextVSIndex; vsSample += bump)
						do
						{
							// now make a VS wave frame, 3 nybbles, LSB first
							vsFrame [0] = (byte) (vsSample & 0xf);
							vsFrame [1] = (byte) ((vsSample >> 4) & 0xf);
							vsFrame [2] = (byte) ((vsSample >> 8) & 0xf);
								
							fos.write (vsFrame);
						}
						while (vsIndex < nextVSIndex);
					}
				}
				
				fos.close ();				
			}
			else
			if (subChunkType.equalsIgnoreCase ("smpl"))
			{
				// aha! some clue as to what's in the sample -- good-o!
				System.out.println ("mfr ID = " + read4ByteInteger (fis));
				System.out.println ("product ID = " + read4ByteInteger (fis));
				System.out.println ("sample period = " + read4ByteInteger (fis));
				System.out.println ("MIDI unity note = " + read4ByteInteger (fis));
				System.out.println ("MIDI pitch fraction note = " + read4ByteInteger (fis));
				System.out.println ("SMPTE format = " + read4ByteInteger (fis));
				System.out.println ("SMPTE offset = " + read4ByteInteger (fis));
				System.out.println ("num sample loops = " + read4ByteInteger (fis));
				System.out.println ("sampler data = " + read4ByteInteger (fis));

				// skip the rest of the chunk
				fis.skip (subChunkSize - (9 * 4));
			}
			else
			{
				// just skip if we don't recognise it
				fis.skip (subChunkSize);
			}
		}
	}

	public static int
	read2ByteInteger (InputStream inStream)
		throws IOException
	{
		int	value = inStream.read ();
		value |= inStream.read () << 8;
		
		return value;
	}

	public static int
	read3ByteInteger (InputStream inStream)
		throws IOException
	{
		int	value = inStream.read ();
		value |= inStream.read () << 8;
		value |= inStream.read () << 16;
		
		return value;
	}

	public static int
	read4ByteInteger (InputStream inStream)
		throws IOException
	{
		int	value = inStream.read ();
		value |= inStream.read () << 8;
		value |= inStream.read () << 16;
		value |= inStream.read () << 24;
		
		return value;
	}

	public static String
	read4ByteLiteral (InputStream inStream)
		throws IOException
	{
		byte[]	buffer = new byte [4];
		int	cc = inStream.read (buffer);
		if (cc < 1)
		{
			throw new EOFException ();
		}
		return new String (buffer, 0, 4);
	}
}
