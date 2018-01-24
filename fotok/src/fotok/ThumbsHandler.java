package fotok;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.eclipse.jetty.server.handler.ResourceHandler;

import hu.qgears.commons.UtilFile;

public class ThumbsHandler extends ResourceHandler {
	FotosStorage storage;

	public ThumbsHandler(FotosStorage storage) {
		this.storage = storage;
	}
	private Map<String, Future<Object>> processing=new HashMap<>();

	/**
	 * Create a resized version of the file and return its path within the thumbs folder.
	 * @param ff
	 * @param size
	 * @return null means the original file must be shown.
	 * @throws IOException 
	 */
	public String createThumb(FotosFile file, ESize size) throws IOException {
		if(size==null||size==ESize.size)
		{
			return null;
		}
		File tgFile = file.getFile();
		File cacheFile = file.getCacheFile(size);
		if (tgFile.isDirectory()) {
			return null;
		}
		if(cacheFile==null)
		{
			return null;
		}
		long lastMod=tgFile.lastModified();
		if (!cacheFile.exists()||cacheFile.lastModified()<lastMod) {
			if(FotosFile.isImage(cacheFile))
			{
				Point imgSize=getSize(file, tgFile, lastMod);
				if(imgSize!=null)
				{
					int maxSize=Math.max(imgSize.x, imgSize.y);
					if(maxSize<size.reqSize())
					{
						// Downscale is not required because original image is small enough
						return null;
					}
					String key=cacheFile.getAbsolutePath();
					cacheFile.getParentFile().mkdirs();
					ProcessBuilder pb;
					switch(size)
					{
					case normal:
						float scale=(float)size.reqSize()/maxSize;
						int w=(int)(scale*imgSize.x);
						int h=(int)(scale*imgSize.y);
						pb = new ProcessBuilder("convert", "-auto-orient", "-resize", ""+w+"x"+h, tgFile.getAbsolutePath(),
							cacheFile.getAbsolutePath()).redirectError(Redirect.INHERIT).redirectOutput(Redirect.INHERIT);
						break;
					case thumb:
						pb = new ProcessBuilder("convert", "-auto-orient", "-thumbnail", "320x200", tgFile.getAbsolutePath(),
								cacheFile.getAbsolutePath()).redirectError(Redirect.INHERIT).redirectOutput(Redirect.INHERIT);
						break;
					default:
						throw new IOException("Size not implemented: "+size);
					}
					execWithKey(key, pb);
				}
			}
		}
		return "/"+file.getCacheFilePath(size).toStringPath();
	}
	private void execWithKey(String key, ProcessBuilder pb)
	{
		Future<Object> p=null;
		synchronized (this) {
			p=processing.get(key);
			if(p==null)
			{
				p=storage.args.getThumbingExecutor().submit(new Callable<Object>() {
					
					@Override
					public Object call() {
						try {
							Process p=pb.start();
							p.waitFor();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						return null;
					}
				});
				processing.put(key, p);
			}
		}
		try {
			p.get();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		synchronized (this) {
			processing.remove(key);
		}
	}

	private Point getSize(FotosFile file, File tgFile, long lastMod) {
		File sizeFile=file.getCacheFile(ESize.size);
		if(sizeFile.exists()&&sizeFile.lastModified()>=lastMod)
		{
			try {
				return loadSize(sizeFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		sizeFile.getParentFile().mkdirs();
		String key=sizeFile.getAbsolutePath();
		ProcessBuilder pb=new ProcessBuilder("identify", "-format", "%wx%h",  tgFile.getAbsolutePath()).redirectOutput(sizeFile);
		execWithKey(key, pb);
		try {
			return loadSize(sizeFile);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	private Point loadSize(File sizeFile) throws IOException {
		String s=UtilFile.loadAsString(sizeFile);
		String[] parts=s.split("x");
		int w=Integer.parseInt(parts[0]);
		int h=Integer.parseInt(parts[1]);
		if(w<1||h<1)
		{
			throw new RuntimeException("Image size is 0 or negative: "+s);
		}
		return new Point(w, h);
	}
}
