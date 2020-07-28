package fotok;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.Request;

import hu.qgears.quickjs.qpage.QButton;
import hu.qgears.quickjs.qpage.QPage;
import hu.qgears.quickjs.utils.AbstractQPage;
import hu.qgears.quickjs.utils.UtilHttpContext;

public class CreateFolder extends AbstractQPage
{
	String contextPath;
	@Override
	public void setRequest(Request baseRequest, HttpServletRequest request) {
		super.setRequest(baseRequest, request);
		contextPath=UtilHttpContext.getContext(baseRequest);
	}

	@Override
	protected void initQPage(QPage page) {
		page.setScriptsAsSeparateFile(contextPath+Fotok.qScripts);
		QButton create=new QButton(page, "create");
		create.clicked.addListener(ev->create());
	}

	private Object create() {
		try {
			ResolvedQuery ff=(ResolvedQuery)userData;
			ff.folder.mkdirSelf();
			new InstantJS(page.getCurrentTemplate()) {
				@Override
				public void generate() {
					write("window.location.replace(\"./?reload=true\");\n");
				}
			}.generate();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void writeBody() {
		write("<h1>Folder does not exist</h1>\n<button id=\"create\">Create folder</button>\n<br/>\n<br/>\n<br/>\n<br/>\n<br/>\n<a href=\"../\">Parent folder - ../</a>\n\n");
	}

}
