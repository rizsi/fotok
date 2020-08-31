package fotok;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpCookie.SameSite;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.SessionHandler;

import com.jspa.logging.Log4Init;

import fotok.database.DatabaseAccess;
import fotok.database.RDupesListenerClient;
import hu.qgears.quickjs.qpage.QPageTypesRegistry;
import hu.qgears.quickjs.qpage.example.QPageContext;
import hu.qgears.quickjs.qpage.example.QPageHandler;
import hu.qgears.quickjs.utils.DispatchHandler;
import hu.qgears.quickjs.utils.HttpSessionQPageManager;
import hu.qgears.quickjs.utils.QPageHandlerToJetty;
import hu.qgears.quickjs.utils.QPageJSHandler;
import joptsimple.annot.AnnotatedClass;
import joptsimple.annot.JOHelp;
import joptsimple.annot.JOSimpleBoolean;
import rdupes.RDupes;

/**
 * Executable main class that opens a Jetty web server and handles QPage based
 * web applications within it.
 */
public class Fotok extends AbstractHandler {
	public static String qScripts="/public/QPage";
	public static String fScripts="/public/js";
	public static String fImages="/public/image";
	public static Args clargs;
	FolderHandler fh;
	public DatabaseAccess da=new DatabaseAccess();
	public Server server;
	public QPageContext qpc;
	public static class Args
	{
		@JOHelp("Jetty http server host to bind to.")
		public String host="127.0.0.1";
		@JOHelp("Folder containing the images.")
		public File images;
		@JOHelp("Jetty http server port")
		public int port=9093;
		@JOHelp("Folder containing the SQLite database. If does not exist on first start the program creates it but its parent folder must exist.")
		public File sqlFile=null;
		@JOHelp("Folder containing the thumbnails. Only redundant data is stored here. Re-generated on demand.")
		public File thumbsFolder=null;
		@JOHelp("Deprecated - public folders are stored in database now. Folder containing the public access redirects. Read and written by the program.")
		public File publicAccessFolder=null;
		@JOHelp("The logins configuration file. See code for documentation :-). File is periodically re-read automatically by the program. So configuration can be modified on the fly.")
		public File loginsConf;
		@JOSimpleBoolean
		@JOHelp("Delete the database file - and recreate it on startup. Useful in development.")
		public boolean deleteDatabase;
		@JOSimpleBoolean
		@JOHelp("Clear the cache - and recreate it on startup. Useful in development.")
		public boolean clearCache;
		@JOSimpleBoolean
		@JOHelp("Debug and demo only feature. All pages are publicly accessibly no login required.")
		public boolean demoAllPublic;
		@JOHelp("In case of access to not authorized resource Query is redirected to this path.")
		public String loginPath="/login/";
		private Authenticator auth;
		public Authenticator getAuth() {
			return auth;
		}
		public int getMaxChunkSize() {
			// Safe max chunk size
			return 100000;
		}
	}

	public Fotok(Args clargs) {
		Fotok.clargs=clargs;
	}

	public static void main(String[] args) throws Exception {
		Args clargs = new Args();
		AnnotatedClass cl = new AnnotatedClass();
		cl.parseAnnotations(clargs);
		System.out.println("Foto manager demo program. Usage:\n");
		cl.printHelpOn(System.out);
		cl.parseArgs(args);
		cl.print();
		new Fotok(clargs).run();
	}
	private void run() throws Exception
	{
		Log4Init.init();
		
		startFilesProcessing();
		
		InetSocketAddress sa = new InetSocketAddress(clargs.host, clargs.port);
		server = new Server(sa);
		qpc=new QPageContext(server);

		FotosStorage storage=new FotosStorage(clargs, clargs.images, clargs.thumbsFolder, da);
		fh=new FolderHandler(this, storage);

		// Specify the Session ID Manager
		DefaultSessionIdManager idmanager = new DefaultSessionIdManager(server, new SecureRandom());
		server.setSessionIdManager(idmanager);

		// Sessions are bound to a context.
		ContextHandler context = new ContextHandler("/");
		server.setHandler(context);

		context.setAttribute(Fotok.class.getName(), this);
		// Create the SessionHandler (wrapper) to handle the sessions
		SessionHandler sessions = new SessionHandler();
		sessions.setSessionCookie("com.rizsi.fotok.Fotok");
		sessions.setSameSite(SameSite.STRICT);
		sessions.getSessionCookieConfig().setSecure(false);
		sessions.getSessionCookieConfig().setPath("/");
		System.out.println("Default max age: "+sessions.getSessionCookieConfig().getMaxAge());;
		sessions.getSessionCookieConfig().setMaxAge(60*60*24);
		// sessions.setRefreshCookieAge(ageInSeconds);MaxInactiveInterval(seconds);CheckingRemoteSessionIdEncoding(remote);
		sessions.addEventListener(HttpSessionQPageManager.createSessionListener());
		context.setHandler(sessions);

		QPageTypesRegistry.getInstance().registerType(new QThumb(null, null, null, null, null, null, null));
		DispatchHandler h=new DispatchHandler();
		h.addHandler("/fotok/", this);
		h.addHandler(qScripts, new QPageJSHandler());
		h.addHandler(fScripts, new FotosJSHandler());
		h.addHandler(fImages, new SvgHandler());
		h.addHandler("", "/", new QPageHandlerToJetty(new QPageHandler(qpc, Listing.class), clargs));
		h.addHandler("/public/access/", new PublicAccess(clargs, this));
		h.addHandler("/","/debug", new DebugHttpPage().createHandler());
		h.addHandler("/","/log-handler", new LogHandler());
		clargs.auth=new Authenticator(this, h, clargs);
		sessions.setHandler(clargs.auth);
		server.start();
		server.join();
	}

	private void startFilesProcessing() throws SQLException {
		if(clargs.deleteDatabase)
		{
			clargs.sqlFile.delete();
		}
		da.start();
		List<Path> l=new ArrayList<>();
		l.add(clargs.images.toPath());
		new RDupes().setClient(new RDupesListenerClient(da)).start(1, l);
	}

	@Override
	public void handle(String target, final Request baseRequest, HttpServletRequest request,
			final HttpServletResponse response) throws IOException, ServletException {
		fh.handle(target, baseRequest, request, response);
	}
}
