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

public class FolderHandler extends HtmlTemplate implements IQPageFactory
{
	private UploadHandlerDelegate delegate=new UploadHandlerDelegate();
	private QPageHandler dQPage;
	private ResourceHandler filesHandler;
	private QPageHandler createFolderPage;
	private ThumbsHandler thumbsHandler;
	private FotosStorage storage;

	public FolderHandler(FotosStorage storage) {
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
		
		thumbsHandler = new ThumbsHandler(storage);
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
			if(Mode.rw.equals(ff.mode)&&delegate.handle(ff.folder.getImagesFolder(), baseRequest, response, false))
			{
				return;
			}else if("/".equals(ff.subPath))
			{
//				if("true".equals(baseRequest.getParameter("video")))
//				{
//					new VideoHandler().handle(target, baseRequest, request, response);
//					return;
//				}
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
			}else
			{
				if("all".equals(baseRequest.getParameter("download")))
				{
					System.out.println("Download all!");
					new FolderAsZipHandler(ff.folder.getFile()).handle(target, baseRequest, request, response);
					return;
				}
				ESize size=null;
				try {
					String sizeParam=baseRequest.getParameter("size");
					if(sizeParam!=null)
					{
						size=ESize.valueOf(sizeParam);
					}
				} catch (Exception e) {
				}
				String path=thumbsHandler.createThumb(ff.file, size);
				if(path==null && "html5".equals(baseRequest.getParameter("video")))
				{
					if(!ff.file.getName().endsWith("webm"))
					{
						// Not webm file - we convert it into the cache
						path=thumbsHandler.convertVideo(ff.file);
					}
				}
				if(path!=null)
				{
					baseRequest.setPathInfo(path);
					thumbsHandler.handle(path, baseRequest, request, response);
				}else
				{
					baseRequest.setPathInfo(target);
					filesHandler.handle(target, baseRequest, request, response);
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
			return new FolderViewPageRW(ff.mode, ff.folder, delegate, thumbsHandler);
		}else
		{
			return new FolderViewPageReadOnly(ff.mode, ff.folder, thumbsHandler);
		}
	}

}
