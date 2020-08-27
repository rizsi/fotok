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
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.Resource;

import fotok.database.GetProcessedEntryByPath;
import hu.qgears.commons.ProgressCounter;
import hu.qgears.commons.ProgressCounterSubTask;
import hu.qgears.commons.UtilFile;
import hu.qgears.commons.UtilString;

/**
 * Servers the thumbnail images and other resized versions.
 */
public class ThumbsHandler extends ResourceHandler {
	FotosStorage storage;
	Fotok fotok;
	
	public ThumbsHandler(Fotok fotok, FotosStorage storage) {
		this.storage = storage;
		this.fotok=fotok;
	}
	private Map<String, Future<Object>> processing=new HashMap<>();

//	/**
//	 * Create a resized version of the file and return its path within the thumbs folder.
//	 * @param ff
//	 * @param size
//	 * @return null means the original file must be shown.
//	 * @throws IOException 
//	 */
//	public String createThumb(FotosFile file, ESize size) throws IOException {
//		if(size==null||size==ESize.size)
//		{
//			return null;
//		}
//		File tgFile = file.getFile();
//		File cacheFile = file.getCacheFile(size);
//		if (tgFile.isDirectory()) {
//			return null;
//		}
//		if(cacheFile==null)
//		{
//			return null;
//		}
//		long lastMod=tgFile.lastModified();
//		if (!cacheFile.exists()||cacheFile.lastModified()<lastMod) {
//			if(FotosFile.isImage(cacheFile))
//			{
//				Point imgSize=getSize(file, tgFile, lastMod);
//				if(imgSize!=null)
//				{
//					int maxSize=Math.max(imgSize.x, imgSize.y);
//					if(maxSize<size.reqSize())
//					{
//						// Downscale is not required because original image is small enough
//						return null;
//					}
//					String key=cacheFile.getAbsolutePath();
//					cacheFile.getParentFile().mkdirs();
//					ProcessBuilder pb;
//					switch(size)
//					{
//					case normal:
//						float scale=(float)size.reqSize()/maxSize;
//						int w=(int)(scale*imgSize.x);
//						int h=(int)(scale*imgSize.y);
//						pb = new ProcessBuilder("convert", "-auto-orient", "-resize", ""+w+"x"+h, tgFile.getAbsolutePath(),
//							cacheFile.getAbsolutePath()).redirectError(Redirect.INHERIT).redirectOutput(Redirect.INHERIT);
//						break;
//					case thumb:
//						pb = new ProcessBuilder("convert", "-auto-orient", "-thumbnail", "320x200", tgFile.getAbsolutePath(),
//								cacheFile.getAbsolutePath()).redirectError(Redirect.INHERIT).redirectOutput(Redirect.INHERIT);
//						break;
//					default:
//						throw new IOException("Size not implemented: "+size);
//					}
//					execWithKey(key, fromProcess(pb));
//				}
//			}
//		}
//		return "/"+file.getCacheFilePath(size).toStringPath();
//	}
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

//	private Point getSize(FotosFile file, File tgFile, long lastMod) {
//		File sizeFile=file.getCacheFile(ESize.size);
//		if(sizeFile.exists()&&sizeFile.lastModified()>=lastMod)
//		{
//			try {
//				return loadSize(sizeFile);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		sizeFile.getParentFile().mkdirs();
//		String key=sizeFile.getAbsolutePath();
//		ProcessBuilder pb=new ProcessBuilder("identify", "-format", "%wx%h",  tgFile.getAbsolutePath()).redirectOutput(sizeFile);
//		execWithKey(key, fromProcess(pb));
//		try {
//			return loadSize(sizeFile);
//		} catch (IOException e) {
//			e.printStackTrace();
//			return null;
//		}
//	}
	@Override
	public Resource getResource(String path) {
		String dbPath="0"+path;
		ESize size=(ESize)Authenticator.tlRequest.get().getAttribute("size");
		GetProcessedEntryByPath gpebp=new GetProcessedEntryByPath(dbPath);
		try {
			fotok.da.commit(gpebp);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Thumbs Get resource: "+dbPath+" "+size+" type: "+gpebp.typeName);
		if(gpebp.typeName!=null)
		{
			// File is processed and we have a result file
			File f=fotok.da.getPreviewImage(gpebp.hash, gpebp.typeName, size);
			if(f!=null)
			{
				return new PathResource(f);
			}
		}
		return super.getResource(path);
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
			ProcessBuilder pb0=new ProcessBuilder("ffmpeg", "-i", tgFile.getAbsolutePath()
					,"-filter:v", "scale=320:-1"	// Drastically scale down resolution
					,"-f", "webm", "-vcodec", "libvpx", "-acodec", "libvorbis"
					//, "-ab", "64000"
//					,"-filter:v", "fps=fps=30"	// Reduce FPS to 30
					, "-crf", "22"
					, c.getAbsolutePath());
			ProcessBuilder pb=new ProcessBuilder("ffmpeg", "-i", tgFile.getAbsolutePath()
							,"-c:v", "copy"	// Copy video stream
							,"-c:a", "aac"	// Reencode sound
							,"-f", "mp4" // MP4 format is compatible with most web browsers
							,"-strict", "experimental" // Dunno but I found it online somewhere :-)
							,"-b:a", "128k" // Sound bitrate I guess
							// , "-acodec", "libvorbis"
							, c.getAbsolutePath());
			// ffmpeg -i 00101.MTS -c:v copy -c:a aac -strict experimental -b:a 128k output.mp4

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
								all=VideoProcessor.decodeTime(time);
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
									long at=VideoProcessor.decodeTime(time);
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
