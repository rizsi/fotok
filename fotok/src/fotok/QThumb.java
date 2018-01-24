package fotok;

import java.io.IOException;

import hu.qgears.quickjs.qpage.HtmlTemplate;
import hu.qgears.quickjs.qpage.IInMemoryPost;
import hu.qgears.quickjs.qpage.QButton;
import hu.qgears.quickjs.qpage.QComponent;
import hu.qgears.quickjs.qpage.QDiv;
import hu.qgears.quickjs.qpage.QPage;
import hu.qgears.quickjs.qpage.QTextEditor;

public class QThumb extends QComponent {
	public FotosFile f;
	public QTextEditor name;
	public QDiv l;
	public QButton delete;
	public String prevName;
	public String nextName;
	FotosFolder parent;
	public QThumb(QPage page, String id, FotosFolder parent, FotosFile f) {
		super(page, id);
		this.f=f;
		this.parent=parent;
		// initialize(null);
	}
	
	@Override
	public void generateExampleHtmlObject(HtmlTemplate parent) {
		setWriter(parent.getWriter());
		write("<div id=\"");
		writeHtml(id);
		write("\" style=\"width:320px; height:200px;\">\n");
		if(FotosFile.isImage(f))
		{
			write("<img id=\"img-thumb-");
			writeHtml(f.getName());
			write("\" class=\"thumb-img center\"></img>\n");
		}else
		{
			if(f.isFolder())
			{
				write("\t\t\t<a href=\"");
				writeHtml(f.getName());
				write("/\" class=\"thumb-img\">\n");
				new FolderPreview(this).generatePreview(QThumb.this.parent, (FotosFolder)f, true);
				write("\t\t\t</a>\n");
			}else
			{
				//<img src="#Hf.getName()#?thumb=1" class="thumb-img center"></img>
			}
		}
		write("\t</div>\n");
		setWriter(null);
	}
	
	@Override
	public void doInit() {
		setParent(page.getCurrentTemplate());
		write("\tnew QThumb(page, \"");
		writeObject(id);
		write("\");\n\tglobalImageSerialLoad.loadImage(\"img-thumb-");
		writeJSValue(f.getName());
		write("\", \"");
		writeJSValue(f.getName());
		write("?thumb=1\");\n");
		setParent(null);
	}

	@Override
	public void handle(HtmlTemplate parent, IInMemoryPost post) throws IOException {
		// TODO Auto-generated method stub
		
	}
}
