package fotok;

import java.util.List;

import fotok.Fotok.Args;
import hu.qgears.quickjs.qpage.QPage;
import hu.qgears.quickjs.utils.AbstractQPage;

public class Listing extends AbstractQPage {

	private Args getArgs() {
		return (Args)userData;
	}

	@Override
	protected void initQPage(QPage page) {
		page.setScriptsAsSeparateFile(Fotok.clargs.contextPath+Fotok.qScripts);
	}

	@Override
	protected void writeBody() {
		write("<h1>Folders accessed by you</h1>\n\n<a href=\"");
		writeHtml(Fotok.clargs.contextPath);
		write("/public/login/\">login/logout</a><br/><br/>\n");
		User user=User.get(page.getQPageManager());
		if(user!=null)
		{
			List<String> myPaths=getArgs().getAuth().listAccessibles(user);
			for(String p :myPaths)
			{
				write("<a href=\"");
				writeHtml(Fotok.clargs.contextPath+p);
				write("\">");
				writeHtml(p);
				write("</a><br/>\n");
			}
		}
		write("\n");
		
	}


}
