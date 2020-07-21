package fotok;

import java.io.File;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.Request;

import fotok.Fotok.Args;
import hu.qgears.commons.UtilFile;
import hu.qgears.quickjs.qpage.QPage;
import hu.qgears.quickjs.utils.AbstractQPage;
import hu.qgears.quickjs.utils.UtilHttpContext;

public class Listing extends AbstractQPage {

	private Args getArgs() {
		return (Args)userData;
	}

	@Override
	protected void initQPage(QPage page) {
		page.setScriptsAsSeparateFile(contextPath+Fotok.qScripts);
	}
	String rootUrl;
	String contextPath;
	@Override
	public void setRequest(Request baseRequest, HttpServletRequest request) {
		super.setRequest(baseRequest, request);
		rootUrl=UtilHttpContext.getRootURL(baseRequest).toString();
		contextPath=UtilHttpContext.getContext(baseRequest);
	}
	@Override
	protected void writeBody() {
		User user=User.get(page.getQPageManager());
		write("<h1>Folders accessed by you</h1>\n\n<a href=\"");
		writeHtml(Fotok.clargs.loginPath);
		write("?url=");
		writeObject(rootUrl);
		write("/\">login/logout</a><br/><br/>\n");
		if(user!=null)
		{
			writeHtml(user.getEmail());
			write("<br/>\n");
			List<String> myPaths=getArgs().getAuth().listAccessibles(user);
			for(String p :myPaths)
			{
				write("<a href=\"");
				writeHtml(contextPath+p);
				write("\">");
				writeHtml(p);
				write("</a><br/>\n");
			}
		}else if(Fotok.clargs.demoAllPublic)
		{
			for(File f: UtilFile.listFiles(Fotok.clargs.images))
			{
				write("<a href=\"");
				writeHtml(contextPath+"/fotok/"+f.getName()+(f.isDirectory()?"/":""));
				write("\">");
				writeHtml(f.getName());
				write("</a><br/>\n");
			}
		}
		write("\n");
		
	}


}
