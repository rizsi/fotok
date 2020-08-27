package fotok;

import java.io.File;

import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.Resource;

import fotok.database.GetProcessedEntryByPath;

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
		if("html5".equals(Authenticator.tlRequest.get().getParameter("video")))
		{
			System.out.println("Thumbs Get video resource: "+dbPath+" "+size+" type: "+gpebp.typeName);
			if("video".equals(gpebp.typeName))
			{
				File vid=fotok.da.getVideoFile(gpebp.hash, gpebp.typeName);
				if(vid!=null)
				{
					return new PathResource(vid);
				}
			}
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
}
