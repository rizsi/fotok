package fotok;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hu.qgears.quickjs.qpage.HtmlTemplate;
import hu.qgears.quickjs.qpage.IInMemoryPost;
import hu.qgears.quickjs.qpage.QComponent;
import hu.qgears.quickjs.qpage.QDiv;
import hu.qgears.quickjs.qpage.QPage;

public class QThumb extends QComponent {
	public FotosFile f;
	public QDiv l;
	public String prevName;
	public String nextName;
	FotosFolder parent;
	private List<ImageLoaderLauncher> imagesToLoad=new ArrayList<>(1);
	public QThumb(QPage page, String id, FotosFolder parent, FotosFile f) {
		super(page, id);
		this.f=f;
		this.parent=parent;
	}
	
	@Override
	public void generateExampleHtmlObject(HtmlTemplate parent) {
		setWriter(parent.getWriter());
		write("<div id=\"");
		writeHtml(id);
		write("\" style=\"width:100%; height:100%;\">\n");
		if(FotosFile.isImage(f))
		{
			String id="img-thumb-"+f.getName();
			String ref=f.getName()+"?size=thumb";
			imagesToLoad.add(new ImageLoaderLauncher(id, ref));
			write("<img id=\"");
			writeHtml(id);
			write("\" src=\"");
			writeHtml(Fotok.clargs.contextPath+Fotok.fImages+"/Image-missing.svg");
			write("\" class=\"thumb-img center\"></img>\n");
		}else
		{
			if(f.isFolder())
			{
				write("\t\t\t<a href=\"");
				writeHtml(f.getName());
				write("/\" class=\"thumb-img\">\n");
				imagesToLoad.addAll(new FolderPreview(this).generatePreview(QThumb.this.parent, (FotosFolder)f, true));
				write("\t\t\t</a>\n");
			}else
			{
				String id="img-unknown-thumb-"+f.getName();
				write("<a href=\"");
				writeHtml(f.getName());
				write("\" download=\"");
				writeHtml(f.getName());
				write("\" class=\"thumb-img\">\n\t<img id=\"");
				writeHtml(id);
				write("\" src=\"");
				writeHtml(Fotok.clargs.contextPath+Fotok.fImages+"/Image-missing.svg");
				write("\" class=\"thumb-img center\"></img>\n</a>\n");
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
		write("\");\n");
		for(ImageLoaderLauncher ill: imagesToLoad)
		{
			ill.launch(this);
		}
		//	globalImageSerialLoad.loadImage("img-thumb-#JSf.getName()#", "#JSf.getName()#?thumb=1");
		setParent(null);
	}

	@Override
	public void handle(HtmlTemplate parent, IInMemoryPost post) throws IOException {
		// TODO Auto-generated method stub
		
	}

	public void scrollIntoView() {
		setParent(page.getCurrentTemplate());
		write("\tpage.components[\"");
		writeJSValue(id);
		write("\"].scrollIntoView();\n");
		setParent(null);
	}
}
