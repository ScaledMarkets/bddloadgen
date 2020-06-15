package loadgen.profile;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;


/** See Explanation in BaseProfileWriter. */
public abstract class BaseProfileReader
{
	public abstract String getProfileFile();

	public abstract void createProfile();

	public abstract void setRequestType(String rt);

	public abstract void setTagString(String ts);

	public void parseNextPartLine(String data) {}


	/**
	 * Read and parse the profile.
	 */
	public void readProfile()
	{
		System.out.println("profileFile=" + getProfileFile());
		int lineNo = 0;
		BufferedReader file;
		try { file = new BufferedReader(new FileReader(getProfileFile())); }
		catch (FileNotFoundException ex) { throw new RuntimeException(ex); }
		for (;;)
		{
			String line;
			try { line = file.readLine(); }
			catch (IOException ex) { throw new RuntimeException(ex); }
			if (line == null) break;

			String data = line.trim();
			lineNo = lineNo + 1;
			if (lineNo == 1)  // first line contains the request type.
			{
				String[] parts = data.split(" ");
				String profileType = parts[0];
				String requestType = parts[1];

				if (!profileType.equals("PerformanceProfile"))
					throw new RuntimeException("Unrecognized profile type: " + profileType);
				createProfile();
				setRequestType(requestType);
			}
			else if (lineNo == 2)  // second line contains the tags string.
			{
				setTagString(data);
			}
			else
				parseNextPartLine(data);
		}
	}
}
