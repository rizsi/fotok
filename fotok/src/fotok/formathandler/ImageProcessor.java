package fotok.formathandler;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import fotok.ESize;
import fotok.Fotok;
import hu.qgears.commons.Pair;
import hu.qgears.commons.UtilProcess;
import hu.qgears.images.SizeInt;

public class ImageProcessor extends CommandLineProcessor {

	public ExifData run(FilesProcessor fp, String hash, File file, ExifData d) throws IOException {
		ExifData ret=null;
		if(d!=null)
		{
			try {
				List<Pair<File, SizeInt>> sizes=new ArrayList<>();
				for (ESize esize: ESize.values()) {
					if(esize==ESize.original)
					{
						continue;
					}
					int maxSize=esize.reqSize();
					File root=new File(Fotok.clargs.thumbsFolder,"image");
					File f=new File(fp.getHashFolder(root, hash), hash+"."+esize+".jpg");
					f.getParentFile().mkdirs();
					if(d.width>maxSize||d.height>maxSize)
					{
						SizeInt size=fp.thumbSize(d.width, d.height, maxSize);
						sizes.add(new Pair<File, SizeInt>(f, size));
					}else
					{
						Files.copy(f.toPath(), f.toPath());
					}
				}
				ExifParser.createResizedImages(file, sizes, d);
				ret=d;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if(ret==null)
		{
			ProcessBuilder pb=new ProcessBuilder("exiftool", file.getAbsolutePath());
			pb.redirectError(Redirect.INHERIT);
			Process p=pb.start();
			ExiftoolProcessor etp=new ExiftoolProcessor();
			processLines(p.getInputStream(), etp);
			System.out.println("Width: "+etp.width+" height: "+etp.height);
			
			// In case of problematic image (EXIF data != image data in case of error in rotation tool) override the size:
			d.width=etp.width;
			d.height=etp.height;
			for (ESize esize: ESize.values()) {
				if(esize==ESize.original)
				{
					continue;
				}
				int maxSize=esize.reqSize();
				File root=new File(Fotok.clargs.thumbsFolder,"image");
				File f=new File(fp.getHashFolder(root, hash), hash+"."+esize+".jpg");
				f.getParentFile().mkdirs();
				if(d.width>maxSize||d.height>maxSize)
				{
					SizeInt size=fp.thumbSize(d.width, d.height, maxSize);
					pb = new ProcessBuilder("convert", "-auto-orient", "-resize", ""+size.getWidth()+"x"+size.getHeight(), file.getAbsolutePath(),
							f.getAbsolutePath()).redirectError(Redirect.INHERIT).redirectOutput(Redirect.INHERIT);
					p=pb.start();
					try {
						UtilProcess.getProcessReturnValueFuture(p).get(10, TimeUnit.SECONDS);
					} catch (Exception e1) {
						p.destroy();
						throw new RuntimeException(e1);
					}
				}else
				{
					Files.copy(f.toPath(), f.toPath());
				}
			}
			ret=new ExifData();
			ret.date=etp.date;
			ret.width=etp.width;
			ret.height=etp.height;
			// ret.orientation=etp.rotation;
		}
		return ret;
	}
}
