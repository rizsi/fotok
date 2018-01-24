package fotok;

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.eclipse.jetty.server.handler.ResourceHandler;

import hu.qgears.commons.UtilProcess;

public class ThumbsHandler extends ResourceHandler {
	FotosStorage storage;

	public ThumbsHandler(FotosStorage storage) {
		this.storage = storage;
	}
	private Map<String, Future<Object>> processing=new HashMap<>();

	public String createThumb(ResolvedQuery ff) {
		File tgFile = ff.file.getFile();
		File cacheFile = ff.file.getCacheFile();
		// System.out.println("Cache file: "+tgFile.getAbsolutePath()+" Cache:
		// "+cacheFile.getAbsolutePath());
		if (tgFile.isDirectory()) {
		} else {
			String key=ff.file.p.toStringPath();
			if (!cacheFile.exists()) {
				if(FotosFile.isImage(cacheFile))
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
										cacheFile.getParentFile().mkdirs();
										Process p = new ProcessBuilder("convert", "-auto-orient", "-thumbnail", "320x200", tgFile.getAbsolutePath(),
												cacheFile.getAbsolutePath()).redirectError(Redirect.INHERIT).redirectOutput(Redirect.INHERIT).start();
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
			}
		}
		return ff.file.getCacheFileName();
	}
}
