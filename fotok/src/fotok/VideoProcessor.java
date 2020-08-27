package fotok;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import fotok.formathandler.CommandLineProcessor;
import fotok.formathandler.ExiftoolProcessor;
import fotok.formathandler.FilesProcessor;
import hu.qgears.commons.Pair;
import hu.qgears.commons.ProgressCounter;
import hu.qgears.commons.ProgressCounterSubTask;
import hu.qgears.commons.UtilFile;
import hu.qgears.commons.UtilString;
import hu.qgears.images.SizeInt;

/**
 * Transcode videos into a format that is handled by web browsers.
 * 
 * Good to know:
 *  * On Ubuntu 20.04 iHD_drv_video.so did not work $ sudo apt remove intel-media-va-driver solves this issue
 *  * Through i965_drv_video.so works on my laptop and also on Intel NUC mini computer.
 * 
 * TODO planned features:
 *  * GPU assisted transcode
 *  * Quick thumb images https://stackoverflow.com/questions/35880407/get-a-picture-of-the-frame-of-a-video-at-every-second
 */
public class VideoProcessor extends CommandLineProcessor
{
	private File source;
	private File targetFolder;
	private String baseName;
	private FFprobeProcessor probe;
	private ExiftoolProcessor exifTool;
	private class FFprobeProcessor implements Consumer<String>
	{
		/**
		 * 1/100th of second
		 */
		public long durationDecades=-1;
		public String codec;
		public String audioCodec;
		public int width=-1;
		public int height=-1;
		@Override
		public void accept(String l) {
			String t=l.trim();
			if(t.startsWith("Duration"))
			{
				String time=UtilString.split(t, " ,").get(1);
				durationDecades=decodeTime(time);
			} else if(t.startsWith("Stream"))
			{
				{
				int idxVideo=t.indexOf("Video: ");
				if(idxVideo>=0)
				{
					String video0=t.substring(idxVideo+"Video: ".length());
					String video=filterZarojels(video0);
					List<String> parts=UtilString.split(video, ",");
					List<String> codecParts=UtilString.split(parts.get(0), " ");
					codec=codecParts.get(0);
					List<String> resolutionParts=UtilString.split(parts.get(2), " x");
					width=Integer.parseInt(resolutionParts.get(0));
					height=Integer.parseInt(resolutionParts.get(1));
				}
				}
				{
					int idxAudio=t.indexOf("Audio: ");
					if(idxAudio>=0)
					{
						String audio=t.substring(idxAudio+"Audio: ".length());
						List<String> parts=UtilString.split(audio, ",");
						List<String> codecParts=UtilString.split(parts.get(0), " ");
						audioCodec=codecParts.get(0);
					}
				}
			}
		}
		/**
		 * Remove everything that is in ()
		 * @param video0
		 * @return
		 */
		private String filterZarojels(String video0) {
			int level=0;
			StringBuilder ret=new StringBuilder();
			for(char ch: video0.toCharArray())
			{
				if(ch=='(')
				{
					level++;
				}else if(ch==')')
				{
					level--;
				}
				else
				{
					if(level==0)
					{
						ret.append(ch);
					}
				}
			}
			return ret.toString();
		}
		
	}
	public VideoProcessor(File source, File targetFolder, String baseName) {
		super();
		this.source = source;
		this.targetFolder = targetFolder;
		this.baseName = baseName;
	}
	private boolean probed=false;
	private void probeFile() throws InterruptedException, IOException
	{
		if(probed)
		{
			return;
		}
		{
			ProcessBuilder pb=new ProcessBuilder("ffprobe", source.getAbsolutePath());
			pb.redirectOutput(Redirect.INHERIT);
			Process p=pb.start();
			p.waitFor(10000, TimeUnit.MILLISECONDS);
			probe=new FFprobeProcessor();
			processLines(p.getErrorStream(), probe);
		}
		{
			ProcessBuilder pb=new ProcessBuilder("exiftool", source.getAbsolutePath());
			pb.redirectError(Redirect.INHERIT);
			Process p=pb.start();
			p.waitFor(10000, TimeUnit.MILLISECONDS);
			exifTool=new ExiftoolProcessor();
			processLines(p.getInputStream(), exifTool);
		}
		System.out.println("Duration: "+probe.durationDecades);
		System.out.println("Data: "+probe.codec+" "+probe.width+" "+probe.height);
		System.out.println("Date: "+exifTool.date);
		TimeZone tz=TimeZone.getDefault();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
		System.out.println("TZ: "+df.format(exifTool.date)+" "+tz);
		probed=true;
	}
	private List<Pair<SizeInt, File>> ret;
	public List<Pair<SizeInt, File>> run(int nPreviewImage, SizeInt size) throws Exception {
		probeFile();
		ret=new ArrayList<>();
		int maxHeights[]=new int[] {320,480,640,1080};
		List<Integer> targetSizes=new ArrayList<>();
		for(int maxHeight:maxHeights)
		{
			if(probe.height>maxHeight)
			{
				targetSizes.add(maxHeight);
			}
		}
		targetSizes.add(null);	// No resize version only transcode so that it works in browser
		for(Integer tg: targetSizes)
		{
			try(ProgressCounterSubTask st=ProgressCounter.getCurrent().subTask("Size: "+tg, 1.0/targetSizes.size()))
			{
				Pair<SizeInt, File> created=createWithSizeWebEnabled(st, tg);
				ret.add(created);
			}
		}
		createPreviewImages(nPreviewImage, size);
		return ret;
	}
	private void createPreviewImages(int nPreviewImage, SizeInt size) throws IOException, InterruptedException
	{
		File source=ret.get(0).getB();
		probeFile();
		commandParts=new ArrayList<>();
		addCommandParts("ffmpeg", "-i", source.getAbsolutePath(), "-y");
		long decadesPerImage=probe.durationDecades/(nPreviewImage-1);
		double d=100.0/decadesPerImage;
		addCommandParts("-r", String.format(Locale.US, "%1$,.2f", d));
		addCommandParts("-vf", "scale="+size.getWidth()+":"+size.getHeight());
		File thumbsfolder=new File(targetFolder, baseName+"_thumbparts");
		thumbsfolder.mkdirs();
		addCommandParts(thumbsfolder.getAbsolutePath()+"/"+"%03d.jpg");
		ProcessBuilder pb=new ProcessBuilder(commandParts);
		pb.redirectError(Redirect.INHERIT);
		pb.redirectOutput(Redirect.INHERIT);
		Process p=pb.start();
		if(!p.waitFor(60000, TimeUnit.MILLISECONDS))
		{
			throw new IOException("Create preview images takes too much time: "+source);
		}
		List<File> created=UtilFile.listFiles(thumbsfolder);
		while(created.size()>12)
		{
			created.remove(created.size()-1);
		}
		commandParts=null;
		commandParts=new ArrayList<>();
		commandParts.add("montage");
		for(File f: created)
		{
			commandParts.add(f.getAbsolutePath());
		}
		addCommandParts("-tile", "4x3", "-geometry", "+0+0");
		addCommandParts(targetFolder.getAbsolutePath()+"/"+baseName+".thumbs.jpg");
		// System.out.println("Montage command: "+UtilString.concat(commandParts, " "));
		pb=new ProcessBuilder(commandParts);
		pb.redirectError(Redirect.INHERIT);
		pb.redirectOutput(Redirect.INHERIT);
		p=pb.start();
		if(!p.waitFor(2000, TimeUnit.MILLISECONDS))
		{
			throw new IOException("Create preview images takes too much time: "+source);
		}
		commandParts=null;
	}
	private Pair<SizeInt, File> createWithSizeWebEnabled(ProgressCounterSubTask st, Integer height) throws IOException, InterruptedException {
		commandParts=new ArrayList<>();
		boolean reencode=(!"h264".equals(probe.codec))||(height!=null);
		addCommandParts("ffmpeg");
		if(reencode)
		{
			configureHwAccel();
		}
		configureInputFile(source);
		SizeInt size;
		if(!reencode)
		{
			addCommandParts("-c:v", "copy");
			size=new SizeInt(probe.width, probe.height);
		}else
		{
			size=FilesProcessor.thumbSize(probe.width, probe.height, height);
			addCommandParts(
					"-filter_hw_device", "foo"
					,"-vf", 
					//"format=nv12|vaapi"+
					//"|vaapi,hwupload"
					//",vaapi"
					//"hwupload"
					//"-vf",
					"hwdownload,format=nv12,"+
					"scale=w="+size.getWidth()+":h="+size.getHeight()+""+
					",hwupload"
					
				//"deinterlace_vaapi,"+
//					"'scale_vaapi=w="+w+":h="+height+",hwdownload,format=nv12'",
					,"-c:v", "h264_vaapi");
		}
		if("aac".equals(probe.audioCodec))
		{
			addCommandParts("-c:a", "copy");
		}else
		{
			addCommandParts("-c:a", "aac"
					,"-b:a","128k"); // Sound bitrate I guess
		}
		File output=new File(targetFolder, baseName+"."+height+".mp4");
		commandParts.add(output.getAbsolutePath());
		executeConversionProcess(st);
		return new Pair<SizeInt, File>(size, output);
	}
	private void executeConversionProcess(ProgressCounterSubTask st) throws IOException, InterruptedException {
		System.out.println("Command: "+UtilString.concat(commandParts, " "));
		ProcessBuilder pb=new ProcessBuilder(commandParts);
		pb.redirectOutput(Redirect.INHERIT);
		Process p;;
		BufferedReader br;
		String l;
		p=pb.start();
		br=new BufferedReader(new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8));
		long all=probe.durationDecades;
		while((l=br.readLine())!=null)
		{
			// System.err.println(l);
			String t=l.trim();
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
					System.err.println("Unknown line: "+t);
				}
			}else
			{
				System.err.println("Unknown line: "+l);
			}
		}
		int v=p.waitFor();
		if(v==0)
		{
			// TODO
//			java.nio.file.Path cp=cacheFile.toPath();
//			Files.move(c.toPath(), cp);
		}
	}
	private void configureHwAccel()
	{
		// See: https://trac.ffmpeg.org/wiki/Hardware/VAAPI
		// Improved command that uses intel hw acceleration
		// $ ffmpeg -vaapi_device /dev/dri/renderD128 -hwaccel vaapi -hwaccel_output_format vaapi -i 00000.MTS -f null -
		addCommandParts("-init_hw_device", "vaapi=foo:/dev/dri/renderD128"
				// "-vaapi_device", "/dev/dri/renderD128" 
		,"-hwaccel", "vaapi"
		,"-hwaccel_output_format", "vaapi"
		,"-hwaccel_device", "foo");
		
		// ffmpeg -init_hw_device vaapi=foo:/dev/dri/renderD128 
		// -hwaccel vaapi -hwaccel_output_format vaapi -hwaccel_device foo
		// -i input.mp4 -filter_hw_device foo -vf 'format=nv12|vaapi,hwupload' -c:v h264_vaapi output.mp4

	}
	private void configureInputFile(File source2) {

		addCommandParts("-i",source2.getAbsolutePath());
		addCommandParts("-y"); // Overwrite output without asking if exists - execution hangs without this in case target exists
		addCommandParts("-f", "mp4"); // MP4 format is compatible with most web browsers
		addCommandParts("-strict", "experimental"); // Dunno but I found it online somewhere :-)
	}
	/**
	 * Decode time
	 * @param time
	 * @return 1/100 seconds
	 */
	public static long decodeTime(String time) {
		List<String> parts=UtilString.split(time, ".:");
		long hr=Long.parseLong(parts.get(0));
		long min=Long.parseLong(parts.get(1));
		long sec=Long.parseLong(parts.get(2));
		long decade=Long.parseLong(parts.get(3));
		return decade+100l*(sec+60l*(min+60l*(hr)));
	}
}
