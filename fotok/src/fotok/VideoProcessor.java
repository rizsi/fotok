package fotok;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import hu.qgears.commons.ProgressCounter;
import hu.qgears.commons.ProgressCounter.AbstractProgressCounterHost;
import hu.qgears.commons.ProgressCounterSubTask;
import hu.qgears.commons.UtilString;

/**
 * Transcode videos into a format that is handled by web browsers.
 * TODO planned features:
 *  * GPU assisted transcode
 *  * Quick thumb images https://stackoverflow.com/questions/35880407/get-a-picture-of-the-frame-of-a-video-at-every-second
 */
public class VideoProcessor {
	private File source;
	private File targetFolder;
	private String baseName;
	private FFprobeProcessor probe;
	private List<String> commandParts;
	private class FFprobeProcessor implements Consumer<String>
	{
		public long duration=-1;
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
				duration=decodeTime(time);
			} else if(t.startsWith("Stream"))
			{
				{
				int idxVideo=t.indexOf("Video: ");
				if(idxVideo>=0)
				{
					String video=t.substring(idxVideo+"Video: ".length());
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
		
	}
	public VideoProcessor(File source, File targetFolder, String baseName) {
		super();
		this.source = source;
		this.targetFolder = targetFolder;
		this.baseName = baseName;
	}
	public static void main(String[] args) throws Exception {
		File src=new File("/tmp/00101.MTS");
		File targetFolder=new File("/tmp/out");
		targetFolder.mkdirs();
		String baseName="video";
		VideoProcessor vp=new VideoProcessor(src, targetFolder, baseName);
		ProgressCounter pc=new ProgressCounter(new AbstractProgressCounterHost() {
			@Override
			public void progressStatusUpdate(Stack<ProgressCounterSubTask> tasks) {
				System.out.println("State: "+tasks);
			}
		}, "transcode video");
		pc.setCurrent();
		vp.run();
	}
	private void run() throws Exception {
		ProcessBuilder pb=new ProcessBuilder("ffprobe", source.getAbsolutePath());
		pb.redirectOutput(Redirect.INHERIT);
		Process p=pb.start();
		p.waitFor(10000, TimeUnit.MILLISECONDS);
		probe=new FFprobeProcessor();
		processLines(p.getErrorStream(), probe);
		System.out.println("Duration: "+probe.duration);
		System.out.println("Data: "+probe.codec+" "+probe.width+" "+probe.height);
		int maxHeights[]=new int[] {320,480,640,1080};
		List<Integer> targetSizes=new ArrayList<>();
		targetSizes.add(null);
		for(int maxHeight:maxHeights)
		{
			if(probe.height>maxHeight)
			{
				targetSizes.add(maxHeight);
			}
		}
		Collections.reverse(targetSizes);
		for(Integer tg: targetSizes)
		{
			try(ProgressCounterSubTask st=ProgressCounter.getCurrent().subTask("Size: "+tg, 1.0/targetSizes.size()))
			{
				createWithSizeWebEnabled(st, tg);
			}
			break;
		}
	}
	private void createWithSizeWebEnabled(ProgressCounterSubTask st, Integer height) throws IOException, InterruptedException {
		commandParts=new ArrayList<>();
		boolean reencode=(!"h264".equals(probe.codec))||(height!=null);
		addCommandParts("ffmpeg");
		if(reencode)
		{
			configureHwAccel();
		}
		configureInputFile(source);
		if(!reencode)
		{
			addCommandParts("-c:v", "copy");
		}else
		{
			int w=probe.width*height/probe.height;
			addCommandParts(
					"-filter_hw_device", "foo"
					,"-vf", 
					//"format=nv12|vaapi"+
					//"|vaapi,hwupload"
					//",vaapi"
					//"hwupload"
					//"-vf",
					"hwdownload,format=nv12,"+
					"scale=w="+w+":h="+height+""+
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
		commandParts.add(new File(targetFolder, baseName+"."+height+".mp4").getAbsolutePath());
		executeConversionProcess(st);
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
		long all=probe.duration;
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
	private void addCommandParts(String... parts) {
		for(String s: parts)
		{
			commandParts.add(s);
		}
	}
	private void processLines(InputStream is, Consumer<String> lineConsumer) throws IOException
	{
		BufferedReader br=new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
		String line;
		while((line=br.readLine())!=null)
		{
			lineConsumer.accept(line);
		}
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
