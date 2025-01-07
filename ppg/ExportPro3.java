
import java.io.*;

/*

	input file is vs-wavetables-linear.bin

	the file format, as i understand it, is...

	32 wavetables x 64 entries x 256 unsigned bytes per entry

	this program exports a selected wavetable as discrete 1024-sample waveforms
	with suitable names for import into a Pro-3
*/

public class ExportPro3
{
	public static void
	main (String[] inArgs)
	throws Exception
	{
		if (inArgs.length < 2)
		{
			System.err.println ("usage: java ExportPro3 filename wavetablenum (0-31)");
			System.exit (1);
		}

		String	inputFileName = inArgs [0];
		int			wavetableNumber = Integer.parseInt (inArgs [1]);

		if (wavetableNumber < 0 || wavetableNumber > 31)
		{
			System.err.println ("wave table number must be 0-31");
			System.exit (1);
		}

		File	inputFile = new File (inputFileName);

		int	expectedLength = 32 * 64 * 256;

		if (inputFile.length () != expectedLength)
		{
			System.err.println ("file isn't of expected length " + expectedLength);
			System.exit (1);
		}

		// now read the wavetable binary
		// note we convert to 16-bit signed integers immediately, woot

		int		wavetableLength = 64 * 256;
		int[]	buffer = new int [wavetableLength];

		int	index = 0;
		FileInputStream	fis = new FileInputStream (inputFile);

		try
		{
			fis.skip (wavetableNumber * 64 * 256);

			for (int i = 0; i < wavetableLength; i++)
			{
				int	sample = fis.read ();

				// stupid Java thinks that bytes are signed
				// so convert from 8 bit unsigned to 16 bit signed
				sample &= 0xff;

				// convert to signed 16 bit
				sample -= 128;
				sample *= 256;

				buffer [i] = sample;
			}
		}
		finally
		{
			fis.close ();
		}

		// each wave takes up 256 samples in the buffer

		// Pro-3 wavetables are 1024 16-bit samples * 16 samples per table
		// why not 64???

		for (int wave = 0; wave < 16; wave++)
		{
			String	outputFileName = "" + wavetableNumber + "_" + (wave < 10 ? "0" : "") + wave + ".wav";

			FileOutputStream	fos = new FileOutputStream (outputFileName);

			// write the file header

			// RIFF magic
			write4ByteLiteral ("RIFF", fos);

			// RIFF chunk size
			// 44 (RIFF chunk + fmt chunk)
			// -8 (exclude RIFF magic and RIFF size)
			// +256 VS wave data converted to 16-bit
			write4ByteLittleEndianInteger (44 - 8 + 256, fos);

			// WAVE magic
			write4ByteLiteral ("WAVE", fos);

			// fmt magic
			write4ByteLiteral ("fmt ", fos);

			// size of fmt chunk
			write4ByteLittleEndianInteger (16, fos);

			// data format - PCM
			write2ByteLittleEndianInteger (1, fos);

			// channel count
			write2ByteLittleEndianInteger (1, fos);

			// sample rate - er what
			write4ByteLittleEndianInteger (48000, fos);

			// (sample Rate * bitsPerSample * channels) / 8
			write4ByteLittleEndianInteger (96000, fos);

			// (bits per sample * channels) / 8
			write2ByteLittleEndianInteger (2, fos);

			// bits per sample
			write2ByteLittleEndianInteger (16, fos);

			// data magic
			write4ByteLiteral ("data", fos);

			// data size - 1024 16-bit samples
			write4ByteLittleEndianInteger (2048, fos);

			// now write the samples
			// note we need to interpolate between 256 samples and 1024 samples
			// sigh

			int	offset = wave * 256;

			for (int sample = 0; sample < 256; sample++)
			{
				int	value = buffer [offset + sample];

				int	nextValue = 0;

				if (sample < 255)
				{
					nextValue = buffer [offset + sample + 1];
				}
				else
				{
					// interpolate back to base
					nextValue = buffer [0];
				}

				int	difference = nextValue - value;
				difference /= 4;

				for (int interpol = 0; interpol < 4; interpol++)
				{
					int	newSample = value + (difference * interpol);

					write2ByteLittleEndianInteger (newSample, fos);
				}
			}

			fos.close ();
		}

	}

	private static void
	write4ByteLiteral (String inLiteral, OutputStream outStream)
		throws IOException
	{
		byte[]	bytes = inLiteral.getBytes();

		outStream.write (bytes [0]);
		outStream.write (bytes [1]);
		outStream.write (bytes [2]);
		outStream.write (bytes [3]);
	}

	private static void
	write2ByteLittleEndianInteger (int inInteger, OutputStream outStream)
		throws IOException
	{
		outStream.write (inInteger & 0xff);
		outStream.write ((inInteger >> 8) & 0xff);
	}

	private static void
	write4ByteLittleEndianInteger (int inInteger, OutputStream outStream)
		throws IOException
	{
		outStream.write (inInteger & 0xff);
		outStream.write ((inInteger >> 8) & 0xff);
		outStream.write ((inInteger >> 16) & 0xff);
		outStream.write ((inInteger >> 24) & 0xff);
	}

}

