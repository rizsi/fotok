package fotok;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;

import hu.qgears.commons.NamedThreadFactory;
import hu.qgears.quickjs.qpage.QPageTypesRegistry;
import hu.qgears.quickjs.qpage.example.QPageHandler;
import hu.qgears.quickjs.utils.DispatchHandler;
import hu.qgears.quickjs.utils.HttpSessionQPageManager;
import hu.qgears.quickjs.utils.QPageHandlerToJetty;
import hu.qgears.quickjs.utils.QPageJSHandler;
import joptsimple.annot.AnnotatedClass;
import joptsimple.annot.JOHelp;

/**
 * Executable main class that opens a Jetty web server and handles QPage based
 * web applications within it.
 */
public class Fotok extends AbstractHandler {
	public static String qScripts="/public/QPage";
	public static String fScripts="/public/js";
	public static Args clargs;
	FolderHandler fh;
	public static class Args
	{
		@JOHelp("This is the root folder of the application when absolute path is necessary to write into the generated HTML. Useful when Jetty is behind Apache2 https proxy. Example: '/fotok'")
		public String contextPath="";
		@JOHelp("Jetty http server host to bind to.")
		public String host="127.0.0.1";
		@JOHelp("Folder containing the images.")
		public File images;
		@JOHelp("Jetty http server port")
		public int port=8888;
		@JOHelp("Folder containing the thumbnails. Only redundant data is stored here. Re-generated on demand.")
		public File thumbsFolder=null;
		@JOHelp("Folder containing the public access redirects. Read and written by the program.")
		public File publicAccessFolder=null;
		@JOHelp("Must contain these files: 'apikey': Google API key. 'clientid': Client Id 'clientid.secret': Client secret")
		public File googleAuthData;
		@JOHelp("The logins configuration file. See code for documentation :-). File is periodically re-read automatically by the program. So configuration can be modified on the fly.")
		public File loginsConf;
		@JOHelp("The number of executors executing thumbnail creation. Actual thumbnailing is done by command line tool 'convert' but this number restricts the number of parallel running convert programs.")
		public int nThumbnailThread=2;
		@JOHelp("Server name. Redirect is done to this server. Useful when Jetty is behind Apache2 https proxy. Example: rizsi.com")
		public String redirectServerName;
		@JOHelp("Server scheme. Redirect is done to this scheme. Useful when Jetty is behind Apache2 https proxy. Example: https")
		public String redirectServerScheme;
		@JOHelp("Server port. Redirect is done to this port. Useful when Jetty is behind Apache2 https proxy. Example: 443")
		public Integer redirectServerPort;

		private Authenticator auth;
		private PublicAccessManager publicAccessManager;
		public Authenticator getAuth() {
			return auth;
		}
		public PublicAccessManager getPublicAccessManager() {
			synchronized (this) {
				if(publicAccessManager==null)
				{
					publicAccessManager=new PublicAccessManager(this);
				}
			}
			return publicAccessManager;
		}
		volatile private ExecutorService thumbingExecutor;
		public ExecutorService getThumbingExecutor() {
			synchronized (this) {
				if(thumbingExecutor==null)
				{
					thumbingExecutor=Executors.newFixedThreadPool(nThumbnailThread,new NamedThreadFactory("thumbnail-gen").setDaemon(true));
				}
			}
			return thumbingExecutor;
		}
	}

	public Fotok(Args clargs) {
		Fotok.clargs=clargs;
		FotosStorage storage=new FotosStorage(clargs, clargs.images, clargs.thumbsFolder);
		fh=new FolderHandler(storage);
	}

	public static void main(String[] args) throws Exception {
//		if(true)
//		{
//			for(Object key:	System.getProperties().keySet())
//			{
//				System.out.println(""+key+": "+System.getProperties().getProperty(""+key));
//			}
//			for(File f:	UtilFile.listFiles(new File(".")))
//			{
//				System.out.write((f.getName()+"\n").getBytes(StandardCharsets.UTF_8));
//			}
//			return;
//		}
		Args clargs = new Args();
		AnnotatedClass cl = new AnnotatedClass();
		cl.parseAnnotations(clargs);
		System.out.println("Foto manager demo program. Usage:\n");
		cl.printHelpOn(System.out);
		cl.parseArgs(args);
		cl.print();
		
		InetSocketAddress sa = new InetSocketAddress(clargs.host, clargs.port);
		Server server = new Server(sa);

		// Specify the Session ID Manager
		HashSessionIdManager idmanager = new HashSessionIdManager();
		server.setSessionIdManager(idmanager);

		// Sessions are bound to a context.
		ContextHandler context = new ContextHandler("/");
		server.setHandler(context);

		// Create the SessionHandler (wrapper) to handle the sessions
		HashSessionManager manager = new HashSessionManager();
		SessionHandler sessions = new SessionHandler(manager);
		sessions.addEventListener(HttpSessionQPageManager.createSessionListener());
		context.setHandler(sessions);
		QPageTypesRegistry.getInstance().registerType(new QThumb(null, null, null, null));
		DispatchHandler h=new DispatchHandler();
		Fotok fotok=new Fotok(clargs);
		h.addHandler("/fotok/", new Fotok(clargs));
		h.addHandler(qScripts, new QPageJSHandler());
		h.addHandler(fScripts, new FotosJSHandler());
		h.addHandler("/listing", new QPageHandlerToJetty(new QPageHandler(Listing.class), clargs));
		h.addHandler("/public/login", new Login(clargs).createHandler());
		h.addHandler("/public/access/", new PublicAccess(clargs, fotok));
		clargs.auth=new Authenticator(h, clargs);
		sessions.setHandler(clargs.auth);
		server.start();
		server.join();
	}

	@Override
	public void handle(String target, final Request baseRequest, HttpServletRequest request,
			final HttpServletResponse response) throws IOException, ServletException {
		fh.handle(target, baseRequest, request, response);
	}
}
