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
		filesHandler.setResourceBase("/");
		filesHandler.setBaseResource(Resource.newResource(storage.images));
		filesHandler.setDirectoriesListed(true);
		filesHandler.setMinAsyncContentLength(Integer.MAX_VALUE);
		filesHandler.setMinMemoryMappedContentLength(Integer.MAX_VALUE);
		
		thumbsHandler = new ThumbsHandler(storage);
		thumbsHandler.setResourceBase("/");
		thumbsHandler.setBaseResource(Resource.newResource(storage.cache));
		thumbsHandler.setDirectoriesListed(true);
		thumbsHandler.setMinAsyncContentLength(Integer.MAX_VALUE);
		thumbsHandler.setMinMemoryMappedContentLength(Integer.MAX_VALUE);
		
		this.storage=storage;
	}

	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		Path p=new Path(target);
		p.validate();
		ResolvedQuery ff=storage.resolve(p);
		ff.mode=Authenticator.getAccessMode(baseRequest);
		try
		{
			baseRequest.setPathInfo(ff.subPath);
			if(Mode.rw.equals(ff.mode)&&delegate.handle(ff.folder.getImagesFolder(), baseRequest, response, false))
			{
				return;
			}else if("/".equals(ff.subPath))
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
					size=ESize.valueOf(baseRequest.getParameter("size"));
				} catch (Exception e) {
				}
				String path=thumbsHandler.createThumb(ff.file, size);
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
		if(ff.mode==Mode.rw)
		{
			return new FolderViewPageRW(ff.mode, ff.folder, delegate);
		}else
		{
			return new FolderViewPageReadOnly(ff.mode, ff.folder);
		}
	}

}
