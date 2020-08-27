package fotok.formathandler;

import java.io.File;
import java.nio.file.Files;
import java.util.Stack;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import fotok.ESize;
import fotok.Fotok;
import fotok.VideoProcessor;
import fotok.database.DatabaseAccess;
import hu.qgears.commons.ProgressCounter;
import hu.qgears.commons.ProgressCounter.AbstractProgressCounterHost;
import hu.qgears.commons.ProgressCounterSubTask;
import hu.qgears.commons.UtilTimer;
import hu.qgears.images.SizeInt;

/**
 * Queue to process files.
 */
public class FilesProcessor {
	private DatabaseAccess da;
	private Object syncObject=new Object();
	LinkedBlockingQueue<Runnable> imageTasks=new LinkedBlockingQueue<>();
	LinkedBlockingQueue<Runnable> videoTasks=new LinkedBlockingQueue<>();
	public static final int maxSize=320;
	private Thread processorThread=new Thread("Image/video processor")
	{
		public void run() {
			while(true)
			{
				Runnable task=null;
				synchronized (syncObject) {
					task=imageTasks.poll();
					if(task==null)
					{
						task=videoTasks.poll();
					}
					if(task==null)
					{
						try {
							syncObject.wait(10000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				if(task!=null)
				{
					try {
						task.run();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
	};
	public FilesProcessor(DatabaseAccess da)
	{
		this.da=da;
		processorThread.setDaemon(true);
		processorThread.start();
	}
	public File getPreviewImage(String hash, String type, ESize size)
	{
		File root=new File(Fotok.clargs.thumbsFolder,type);
		File h=getHashFolder(root, hash);
		switch (type) {
		case "video":
			return new File(h, hash+".thumbs.jpg");
		case "image":
			return new File(h, hash+"."+size+".jpg");
		default:
			return null;
		}
	}
	private File getHashFolder(File root, String hash)
	{
		// return new File(root, hash.substring(0,1)+"/"+hash.substring(1,2));
		return root;
	}
	public void queueImage(String hash, File file, ExifData d)
	{
		synchronized (syncObject) {
			imageTasks.add(new Runnable() {
				@Override
				public void run() {
					try {
						for (ESize esize: ESize.values()) {
							int maxSize=esize.reqSize();
							File root=new File(Fotok.clargs.thumbsFolder,"image");
							File f=new File(getHashFolder(root, hash), hash+"."+esize+".jpg");
							f.getParentFile().mkdirs();
							if(d.width>maxSize||d.height>maxSize)
							{
								System.out.println("Original size: "+d.height+" "+d.height+" to "+maxSize);
								SizeInt size=thumbSize(d.width, d.height, maxSize);
								ExifParser.createResizedImage(file, size, f, d.orientation);
							}else
							{
								Files.copy(f.toPath(), f.toPath());
							}
						}
						da.imageProcessed(hash, d);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			syncObject.notifyAll();
		}
	}
	public static SizeInt thumbSize(int width, int height, int maxSize_) {
		if(width>height)
		{
			return new SizeInt(maxSize_, height*maxSize_/width);
		}else 
		{
			return new SizeInt(width*maxSize_/height, maxSize_);
		}
	}
	public void queueVideo(String hash, File file, ExiftoolProcessor etp) {
		UtilTimer.javaTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				synchronized (syncObject) {
					videoTasks.add(new Runnable() {
						@Override
						public void run() {
							try {
								File root=new File(Fotok.clargs.thumbsFolder,"video");
								File f=getHashFolder(root, hash);
								f.mkdirs();
								VideoProcessor vp=new VideoProcessor(file, f, hash);
								ProgressCounter pc=new ProgressCounter(new AbstractProgressCounterHost() {
									@Override
									public void progressStatusUpdate(Stack<ProgressCounterSubTask> tasks) {
										System.out.println("State: "+tasks);
									}
								}, "transcode video");
								pc.setCurrent();
								vp.run(12, thumbSize(etp.width, etp.height, maxSize));
								da.videoProcessed(hash, etp);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					});
					syncObject.notifyAll();
				}
			}
		}, 1000);
	}
}
