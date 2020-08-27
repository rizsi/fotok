package fotok;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;

import fotok.Authenticator.Mode;
import hu.qgears.quickjs.qpage.HtmlTemplate;
import hu.qgears.quickjs.qpage.example.IQPageFactory;
import hu.qgears.quickjs.qpage.example.QPageHandler;
import hu.qgears.quickjs.upload.UploadHandlerDelegate;
import hu.qgears.quickjs.utils.AbstractQPage;

/**
 * Serves Images Folder listing (array of thumbnails) and also 
 * serves the resources (raw image, video files, processed resized thumbnails) themselves
 * through delegated handler.
 */
public class FolderHandler extends HtmlTemplate implements IQPageFactory
{
	private UploadHandlerDelegate delegate=new UploadHandlerDelegate();
	private QPageHandler dQPage;
	private ResourceHandler filesHandler;
	private QPageHandler createFolderPage;
	private ThumbsHandler thumbsHandler;
	private FotosStorage storage;

	public FolderHandler(Fotok fotok, FotosStorage storage) {
		dQPage=new QPageHandler(this);
		createFolderPage=new QPageHandler(CreateFolder.class);
		filesHandler = new ResourceHandler();
//		MimeTypes mt=new MimeTypes();
//		mt.addMimeMapping("MTS", "video/mts");
		filesHandler.setResourceBase("/");
		filesHandler.setBaseResource(Resource.newResource(storage.images));
		filesHandler.setDirectoriesListed(true);
//		filesHandler.setMimeTypes(mt);
		filesHandler.setMinAsyncContentLength(Integer.MAX_VALUE);
		filesHandler.setMinMemoryMappedContentLength(Integer.MAX_VALUE);
		
		thumbsHandler = new ThumbsHandler(fotok, storage);
		thumbsHandler.setResourceBase("/");
		thumbsHandler.setBaseResource(Resource.newResource(storage.cache));
		thumbsHandler.setDirectoriesListed(true);
		thumbsHandler.setMinAsyncContentLength(Integer.MAX_VALUE);
		thumbsHandler.setMinMemoryMappedContentLength(Integer.MAX_VALUE);

		try {
			filesHandler.start();
			thumbsHandler.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.storage=storage;
	}

	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		Path p=new Path(target);
		p.validate();
		ResolvedQuery ff=storage.resolve(p, baseRequest);
		ff.mode=Authenticator.getAccessMode(baseRequest);
		try
		{
			baseRequest.setPathInfo(ff.subPath);
			String sizeParam=baseRequest.getParameter("size");
			String videoParam=baseRequest.getParameter("video");
			ESize size=null;
			try {
				if(sizeParam!=null)
				{
					size=ESize.valueOf(sizeParam);
					Authenticator.tlRequest.get().setAttribute("size", size);
				}
			} catch (Exception e) {
			}
			if(Mode.rw.equals(ff.mode)&&delegate.handle(ff.folder.getImagesFolder(), baseRequest, response, false))
			{
				// Upload delegate access in case of RW mode of folder
				return;
			} else if ("all".equals(baseRequest.getParameter("download")))
			{
					System.out.println("Download all!");
					new FolderAsZipHandler(ff.folder.getFile()).handle(target, baseRequest, request, response);
					return;
			} else if ("html5".equals(videoParam))
			{
				// Handle video file
				baseRequest.setPathInfo(target);
				thumbsHandler.handle(target, baseRequest, request, response);
			} else if (size!=null)
			{
				// Handle image file - resized or original size
				if(size!=ESize.original)
				{
					baseRequest.setPathInfo(target);
					thumbsHandler.handle(target, baseRequest, request, response);
				}else
				{
					baseRequest.setPathInfo(target);
					filesHandler.handle(target, baseRequest, request, response);
				}
			}else
			{
				if(ff.folder.exists())
				{
					dQPage.handle(ff.subPath, baseRequest, request, response, ff);
				}else
				{
					if(ff.mode==Mode.rw)
					{
						createFolderPage.handle(ff.subPath, baseRequest, request, response, ff);
					}
				}
			}
		}finally
		{
			baseRequest.setPathInfo(target);
		}
	}

	@Override
	public AbstractQPage createPage(Object request) throws Exception {
		ResolvedQuery ff=(ResolvedQuery) request;
		if(ff.mode==Mode.rw && ff.isEditModeAsked())
		{
			return new FolderViewPageRW(ff.mode, ff.folder, ff.file, delegate, thumbsHandler);
		}else
		{
			return new FolderViewPageReadOnly(ff.mode, ff.folder, ff.file, thumbsHandler);
		}
	}

}
