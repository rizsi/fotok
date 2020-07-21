package fotok;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fotok.Fotok.Args;
import hu.qgears.commons.UtilFile;
import hu.qgears.commons.UtilString;
import hu.qgears.quickjs.qpage.QPageManager;
import hu.qgears.quickjs.utils.HttpSessionQPageManager;

public class Authenticator extends AbstractHandler {
	AbstractHandler delegate;
	Args clargs;
	String prevContent;
	Logger log=LoggerFactory.getLogger(getClass());
	enum Mode
	{
		ro,
		rw,
	}
	public class Access
	{
		Mode m;
		String email;
		String accessed;
		public Access(Mode m, String email, String accessed) {
			super();
			this.m = m;
			this.email = email;
			this.accessed = accessed;
		}
		@Override
		public String toString() {
			return ""+m+" "+email+" "+accessed;
		}
		public boolean matchUser(User user) {
			return email.equals(user.getEmail());
		}
	}
	private List<Access> accessList=new ArrayList<>();

	public Authenticator(AbstractHandler delegate, Args clargs) {
		this.delegate=delegate;
		this.clargs = clargs;
		if(clargs.demoAllPublic)
		{
			return;
		}
		reloadConfig();
		Timer t = new Timer(true);
		t.schedule(new TimerTask() {

			@Override
			public void run() {
				reloadConfig();
			}
		}, 10000, 10000);
	}

	private void reloadConfig() {
		synchronized (this) {
			try {
				String f=UtilFile.loadAsString(clargs.loginsConf);
				if(prevContent==null||!prevContent.equals(f))
				{
					System.out.println("Reload access file!");
					accessList.clear();
					prevContent=f;
					List<String> lines=UtilString.split(f, "\r\n");
					for(String line: lines)
					{
						List<String> pieces=UtilString.split(line, "\t ");
						if(pieces.size()==3)
						{
							if(!pieces.get(0).startsWith("#"))
							{
								try {
									Mode m=Mode.valueOf(pieces.get(0));
									String email=pieces.get(1);
									String accessed=pieces.get(2);
									if(!accessed.endsWith("/")){
										System.err.println("Accessed must end with / : '"+accessed+"' "+line);
									}else
									{
										Access a=new Access(m, email, accessed);
										accessList.add(a);
										System.out.println("Access: "+a);
									}
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}					
						}
					}
				}
			} catch (IOException e) {
				accessList.clear();
				e.printStackTrace();
			}
		}
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		authenticateUser(baseRequest);
		List<String> pieces=UtilString.split(target, "/");
		User user=User.get(HttpSessionQPageManager.getManager(baseRequest.getSession()));
		System.out.println("Query: "+System.currentTimeMillis()+" "+target);
		if(pieces.size()>0&&(pieces.get(0).equals("public")))
		{
			delegate.handle(target, baseRequest, request, response);
		}else
		{
			if(user==null&&!clargs.demoAllPublic)
			{
				response.sendRedirect(Fotok.clargs.contextPath+"/public/login/");
				baseRequest.setHandled(true);
			}else
			{
				Mode mode;
				if(clargs.demoAllPublic)
				{
					mode=Mode.rw;
				}else
				{
					mode=getMode(user, target);
				}
				if(mode==null)
				{
					response.sendRedirect(Fotok.clargs.contextPath+"/listing/");
					baseRequest.setHandled(true);
				}else
				{
					setAccessMode(baseRequest, mode);
					delegate.handle(target, baseRequest, request, response);
				}
			}
		}
	}
	private void authenticateUser(Request baseRequest) {
		{
			String username=validateSingleAndGet(baseRequest, "X-User");
			String groups=validateSingleAndGet(baseRequest, "X-Groups");
//			System.out.println("username: "+username+" "+groups);
			// List<String> groupsList=UtilString.split(groups, " ");
			QPageManager qPageManager=HttpSessionQPageManager.getManager(baseRequest.getSession());
			User user=User.get(qPageManager);
			if((user==null && username!=null) || (user!=null&& !user.getEmail().equals(username)))
			{
				user=new User(username, username, "", "", "", username, "", "");
				User.set(qPageManager, user);
				user.setGroups(groups);
				log.info("Session user set: "+user);
			}
			if(user!=null)
			{
				user.setGroups(groups);
			}
		}
	}

	private String validateSingleAndGet(Request baseRequest, String string) {
		Enumeration<String> usernames=baseRequest.getHeaders(string);
		if(usernames.hasMoreElements())
		{
			String ret=usernames.nextElement();
			if(usernames.hasMoreElements())
			{
				throw new IllegalArgumentException("Multiple "+string+" headers are not allowed");
			}
			return ret;
		}
		return null;
	}

	public static void setAccessMode(Request req, Mode mode)
	{
		req.setAttribute("accessMode", mode);
	}
	public static Mode getAccessMode(Request req)
	{
		return (Mode)req.getAttribute("accessMode");
	}

	private Mode getMode(User user, String target) {
		Mode ret=null;
		synchronized (this) {
			for(Access a: accessList)
			{
				if(a.matchUser(user))
				{
					if(target.startsWith(a.accessed))
					{
						ret=a.m;
						if(ret==Mode.rw)
						{
							return ret;
						}
					}
				}
			}
		}
		return ret;
	}

	public List<String> listAccessibles(User user) {
		List<String> paths=new ArrayList<>();
		synchronized (this) {
			for(Access a: accessList)
			{
				if(a.matchUser(user))
				{
					paths.add(a.accessed);
				}
			}
		}
		return paths;
	}

}
