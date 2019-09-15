
import java.io.*;

/*

	file format, as i understand it, is...
	
	32 wavetables x 64 entries x 256 unsigned bytes per entry
*/

public class Render
{
	public static void
	main (String[] inArgs)
	throws Exception
	{
		if (inArgs.length == 0)
		{
			System.err.println ("usage: java Render filename");
			System.exit (1);
		}
		
		String	inputFileName = inArgs [0];
		File	inputFile = new File (inputFileName);
		
		int	expectedLength = 32 * 64 * 256;
		
		if (inputFile.length () != expectedLength)
		{
			System.err.println ("file isn't of expected length " + expectedLength);
			System.exit (1);
		}
		
		// now read the juicin' lot
		// note we convert to 16-bit signed integers immediately, woot
		
		int[]	buffer = new int [expectedLength];
		
		int	index = 0;
		FileInputStream	fis = new FileInputStream (inputFile);
		
		try
		{
			for (int i = 0; i < expectedLength; i++)
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
		
		fis = new FileInputStream ("template.wav");
		
		// read the meta blocks from the template and blow them out to the new wav
		byte[]	metaBuffer = new byte [44];
		
		fis.read (metaBuffer);
		fis.close ();

		// now we write 32 wavetable files
		// 16 bit samples * 256 samples per wave
		int	offset = 0;
		
		for (int table = 0; table < 32; table++)
		{
			int	tableOffset = table * (64 * 256);
			
			String	outputFileName = "ppg_" + (table < 10 ? "0" : "") + table + ".wav";

			FileOutputStream	fos = new FileOutputStream (outputFileName);

			System.out.println ("writing " + outputFileName);
			
			try
			{
				fos.write (metaBuffer);

				for (int wave = 0; wave < 64; wave++)
				{
					int	waveOffset = wave * 256;

					for (int sample = 0; sample < 256; sample++)
					{
						int	sampleData = 0;
						
						// hack in waves 60-63
						// which weren't in the wavetable
						// and were hacked in by the PPG firmware
			
						// we probably don't need the hacked sine, square, saw etc
						// so disable
						
						if (true)
						// if (wave < 60)
						{
							offset = tableOffset + waveOffset + sample;
		
							sampleData = buffer [offset];
						}
						else
						if (wave == 60)
						{
							// triangle wave
							
							int	phase = sample / 64;
							
							if (phase == 0)
							{
								sampleData = (32768 / 64) * sample;
							}
							else
							if (phase == 1)
							{
								sampleData = 32767 - ((32768 / 64) * (sample - 64));
							}
							if (phase == 2)
							{
								sampleData = (32768 / 64) * sample;
								sampleData = -sampleData;
							}
							else
							if (phase == 3)
							{
								sampleData = 32767 - ((32768 / 64) * (sample - 64));
								sampleData = -sampleData;
							}
						}
						else
						if (wave == 61)
						{
							// 90% pulse wave
							if (sample < 245)
							{
								sampleData = -32768;
							}
							else
							{
								sampleData = 32767;
							}
						}
						else
						if (wave == 62)
						{
							// square
							if (sample < 128)
							{
								sampleData = -32768;
							}
							else
							{
								sampleData = 32767;
							}
						}
						else
						if (wave == 63)
						{
							// positive-going ramp
							sampleData = (65536 / 256) * sample;
							sampleData -= 32768;
						}

						// little-endian
						fos.write (sampleData & 0xff);
						fos.write ((sampleData >> 8) & 0xff);
					}
				}
				
			}
			finally
			{
				fos.close ();
			}
		}

		
		
	}
	
}

