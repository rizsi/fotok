package fotok;

import hu.qgears.quickjs.qpage.QButton;
import hu.qgears.quickjs.qpage.QPage;
import hu.qgears.quickjs.utils.AbstractQPage;

public class CreateFolder extends AbstractQPage
{

	@Override
	protected void initQPage(QPage page) {
		page.setScriptsAsSeparateFile(Fotok.clargs.contextPath+Fotok.qScripts);
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
		write("<h1>Folder does not exist</h1>\n<button id=\"create\">Create folder</button>\n");
	}

}
