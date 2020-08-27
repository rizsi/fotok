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
import rdupes.RDupes;
import rdupes.RDupesFile;
import rdupes.RDupesObject;
import rdupes.RDupesPath;

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
		RDupes rd=Fotok.rdupes;
		User user=User.get(page.getQPageManager());
		write("<h1>Folders accessed by you</h1>\n\n");
		if(false)
		{
			write("RDupes: Tasks: ");
			writeObject(rd.tasks.get());
			write(" Files indexed: ");
			writeObject(rd.filesProcessed.get());
			write(" in ");
			writeObject(rd.foldersProcessed.get());
			write(" folders ongoing hash: \n");
			writeObject(rd.nBytesToHahs.get());
			write(" bytes in ");
			writeObject(rd.nFileToHash.get());
			write(" files\n<br/>\n");
		}
		write("<input type=\"file\" accept=\"image/*\" multiple>\n<br/>\n<br/>\n<br/>\n<br/>\n");
		//		visit(rd, "");
		write("\n\n<a href=\"");
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

	private void visit(RDupesObject ro, String string) {
		writeObject(string);
		writeObject(ro.getFullName());
//		if(ro instanceof RO)
		if(ro instanceof RDupesPath)
		{
			write(" - ");
			RDupesPath path=(RDupesPath) ro;
			writeObject(path.getStringInfo());
			if(ro instanceof RDupesFile)
			{
				RDupesFile f=(RDupesFile) ro;
				writeObject(f.storedHash);
			}
		}
		write("<br/>\n");
		for(RDupesObject o:ro.getChildren())
		{
			visit(o, "-"+string);
		}
	}


}
