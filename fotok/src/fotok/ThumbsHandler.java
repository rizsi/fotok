package fotok;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.eclipse.jetty.server.handler.ResourceHandler;

import hu.qgears.commons.ProgressCounter;
import hu.qgears.commons.ProgressCounterSubTask;
import hu.qgears.commons.UtilFile;
import hu.qgears.commons.UtilString;

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
					execWithKey(key, fromProcess(pb));
				}
			}
		}
		return "/"+file.getCacheFilePath(size).toStringPath();
	}
	private Callable<Object> fromProcess(ProcessBuilder pb)
	{
		return new Callable<Object>() {
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
		};
	}
	private void execWithKey(String key, Callable<Object> callable)
	{
		Future<Object> p=null;
		synchronized (this) {
			p=processing.get(key);
			if(p==null)
			{
				p=storage.args.getThumbingExecutor().submit(
						callable);
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
		execWithKey(key, fromProcess(pb));
		try {
			return loadSize(sizeFile);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	private Point loadSize(File sizeFile) throws IOException {
		String s=UtilFile.loadAsString(sizeFile);
		List<String> parts=UtilString.split(s, "x\r\n");
		int w=Integer.parseInt(parts.get(0));
		int h=Integer.parseInt(parts.get(1));
		if(w<1||h<1)
		{
			throw new RuntimeException("Image size is 0 or negative: "+s);
		}
		return new Point(w, h);
	}
	public String convertVideo(FotosFile file) {
		// Improved command that uses intel hw acceleration
		// $ ffmpeg -vaapi_device /dev/dri/renderD128 -hwaccel vaapi -hwaccel_output_format vaapi -i 00000.MTS -f null -
		File tgFile = file.getFile();
		File cacheFile = file.getVideoCacheFile();
		if (tgFile.isDirectory()) {
			return null;
		}
		if(cacheFile==null)
		{
			return null;
		}
		long lastMod=tgFile.lastModified();
		if (!cacheFile.exists()||cacheFile.lastModified()<lastMod) {
			cacheFile.getParentFile().mkdirs();
			File c=new File(cacheFile.getParentFile(), cacheFile.getName()+".tmp");
			c.delete();
			ProcessBuilder pb=new ProcessBuilder("ffmpeg", "-i", tgFile.getAbsolutePath()
					,"-filter:v", "scale=320:-1"	// Drastically scale down resolution
					,"-f", "webm", "-vcodec", "libvpx", "-acodec", "libvorbis"
					//, "-ab", "64000"
//					,"-filter:v", "fps=fps=30"	// Reduce FPS to 30
					, "-crf", "22"
					, c.getAbsolutePath());
			// pb.redirectError(Redirect.INHERIT);
			pb.redirectOutput(Redirect.INHERIT);
			try(ProgressCounterSubTask st=ProgressCounter.getCurrent().subTask("ffmpeg", 1))
			{
				Callable<Object> conversion=new Callable<Object>() {
					@Override
					public Object call() throws Exception {
						Process p;;
						BufferedReader br;
						String l;
						p=pb.start();
						br=new BufferedReader(new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8));
						long all=Long.MAX_VALUE;
						while((l=br.readLine())!=null)
						{
							String t=l.trim();
							if(t.startsWith("Duration"))
							{
								String time=UtilString.split(t, " ,").get(1);
								System.out.println("Decoded time: "+time);
								all=decodeTime(time);
								if(all==0)
								{
									all++;
								}
							}
							if(t.startsWith("frame="))
							{
								int idx=t.indexOf("time=");
								if(idx>0)
								{
									String time=t.substring(idx+"time=".length(),idx+"time=".length()+11);
									long at=decodeTime(time);
									st.setWork(((double)at)/all);
								}else
								{
									System.err.println(t);
								}
							}else
							{
								System.err.println(l);
							}
						}
						int v=p.waitFor();
						if(v==0)
						{
							java.nio.file.Path cp=cacheFile.toPath();
							Files.move(c.toPath(), cp);
						}
						return null;
					}
				};
				execWithKey(cacheFile.getAbsolutePath(), conversion);
				System.out.println("Conversion finished: "+cacheFile.getAbsolutePath());
			}
		}
		return "/"+file.getVideoCacheFilePath().toStringPath();
	}
	protected long decodeTime(String time) {
		List<String> parts=UtilString.split(time, ".:");
		long hr=Long.parseLong(parts.get(0));
		long min=Long.parseLong(parts.get(1));
		long sec=Long.parseLong(parts.get(2));
		long decade=Long.parseLong(parts.get(3));
		
		return decade+100l*(sec+60l*(min+60l*(hr)));
	}
	public void convertAll(FotosFolder folder) {
		File f=folder.getImagesFolder();
		List<File> videofiles=new ArrayList<>();
		long size=1;
		for(File g:UtilFile.listFiles(f))
		{
			if(FotosFile.isVideo(g))
			{
				size+=g.length();
				videofiles.add(g);
			}
		}
		for(int i=0;i<videofiles.size();++i)
		{
			File g=videofiles.get(i);
			long l=g.length();
			if(l>0)
			{
				try(ProgressCounterSubTask st=ProgressCounter.getCurrent().subTask(g.getName(), ((double)l)/size))
				{
					Path ch=new Path(folder.p);
					ch.pieces.add(g.getName());
					FotosFile ff=new FotosFile(storage, ch);
					convertVideo(ff);
				}
			}
		}
	}
}
