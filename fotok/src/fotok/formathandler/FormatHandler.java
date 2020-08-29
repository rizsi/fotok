package fotok.formathandler;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;

import fotok.database.DatabaseAccess;

/**
 * Find out what format a file is and if it is a known type then create all preview
 * versions of the file.
 */
public class FormatHandler extends CommandLineProcessor
{
	private File file;
	private DatabaseAccess da;
	String hash;
	public FormatHandler(DatabaseAccess da, File file, String hash) {
		this.da=da;
		this.file=file;
		this.hash=hash;
	}
	public void run() throws IOException {
		try {
			ExifData d=new ExifParser().parseFile(file, null);
			// System.out.println("JPEG: "+hash+" "+file.getAbsoluteFile()+" "+d.width+" "+d.height+" "+d.date);
			da.fp.queueImage(hash, file, d);
			return;
		}catch(Exception ioex)
		{
			// ioex.printStackTrace();
			// Ignore - file may not be an image...
		}
		ProcessBuilder pb=new ProcessBuilder("exiftool", file.getAbsolutePath());
		pb.redirectError(Redirect.INHERIT);
		Process p=pb.start();
		ExiftoolProcessor etp=new ExiftoolProcessor();
		processLines(p.getInputStream(), etp);
		if(etp.mimeType!=null && etp.mimeType.startsWith("image/"))
		{
			da.fp.queueImage(hash, file, null);
		}
		if(etp.mimeType!=null && etp.mimeType.startsWith("video/"))
		{
			// System.out.println("VIDEO: "+hash+" "+file.getAbsoluteFile()+" "+etp.date);
			da.fp.queueVideo(hash, file, etp);
		}
	}
}
