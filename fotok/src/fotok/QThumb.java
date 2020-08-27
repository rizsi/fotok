package fotok;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hu.qgears.quickjs.qpage.HtmlTemplate;
import hu.qgears.quickjs.qpage.IInMemoryPost;
import hu.qgears.quickjs.qpage.QComponent;
import hu.qgears.quickjs.qpage.QPage;

public class QThumb extends QComponent {
	public interface LabelsGenerator
	{
		void generateLables(FotosFile f, HtmlTemplate parent, QThumb t);
	}
	public FotosFile f;
	public String prevName;
	public String nextName;
	FotosFolder parent;
	private String thumbId;
	LabelsGenerator labelsGenerator;
	ERotation rot=ERotation.rotation0;
	String contextPath;
	private List<ImageLoaderLauncher> imagesToLoad=new ArrayList<>(1);
	public QThumb(QPage page, String id, FotosFolder parent, FotosFile f, LabelsGenerator labelsGenerator, String contextPath) {
		super(page, id);
		this.contextPath=contextPath;
		this.f=f;
		this.parent=parent;
		this.labelsGenerator=labelsGenerator;
		if(f!=null)
		{
			rot=f.getRotation();
		}
	}
	
	@Override
	public void generateHtmlObject(HtmlTemplate parent) {
		try(ResetOutputObject roo=setParent(parent))
		{
			write("<div id=\"");
			writeHtml(id);
			write("\" style=\"width: 320px; height: 250px;\">\n\t<div id=\"view-");
			writeHtml(f.getName());
			write("\" style=\"height: 90%\">\n");
			if(FotosFile.isImage(f))
			{
				thumbId="img-thumb-"+f.getName();
				String ref=f.getName()+"?size=thumb";
				imagesToLoad.add(new ImageLoaderLauncher(thumbId, ref));
				write("<img id=\"");
				writeHtml(thumbId);
				write("\" src=\"");
				writeHtml(contextPath+Fotok.fImages+"/Image-missing.svg");
				write("\" class=\"thumb-img center ");
				writeObject(rot.getJSClass());
				write("\"></img>\n");
			}else
			{
				if(f.isFolder())
				{
					write("\t\t\t<a href=\"");
					writeHtml(f.getName());
					write("/\" class=\"thumb-img\">\n");
					imagesToLoad.addAll(new FolderPreview(this).generatePreview(QThumb.this.parent, (FotosFolder)f, true, contextPath));
					write("\t\t\t</a>\n");
				}else if(FotosFile.isVideo(f))
				{
					write("<a href=\"");
					writeHtml(f.getName());
					write("\" download=\"");
					writeHtml(f.getName());
					write("\" class=\"thumb-img\">\nVIDEO DOWNLOAD");
					writeHtml(" - "+f.getFile().length()+" bytes");
					write("</a>\n<img src=\"");
					writeHtml(f.getName());
					write("?size=thumb\" class=\"thumb-img center ");
					writeObject(rot.getJSClass());
					write("\"></img>\n");
				}
				else
				{
					String id="img-unknown-thumb-"+f.getName();
					write("<a href=\"");
					writeHtml(f.getName());
					write("\" download=\"");
					writeHtml(f.getName());
					write("\" class=\"thumb-img\">\n\t<img id=\"");
					writeHtml(id);
					write("\" src=\"");
					writeHtml(contextPath+Fotok.fImages+"/Image-missing.svg");
					write("\" class=\"thumb-img center\"></img>\n</a>\n");
				}
			}
			write("\t</div>\n\t<div style=\"height:10%\">\n");
			labelsGenerator.generateLables(f, this, this);
			write("\t</div>\n</div>\t\n");
		}
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

	public ERotation rotate() {
		if(FotosFile.isImage(f))
		{
			rot=ERotation.values()[(rot.ordinal()+1)%ERotation.values().length];
			setParent(page.getCurrentTemplate());
			write("\tpage.components[\"");
			writeJSValue(id);
			write("\"].setRotation(\"");
			writeJSValue(thumbId);
			write("\", \"");
			writeJSValue(rot.getJSClass());
			write("\");\n");
			setParent(null);
		}
		return rot;
	}

	@Override
	public void generateHtmlObject() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void doInitJSObject() {
		setParent(page.getCurrentTemplate());
		write("\tnew QThumb(page, \"");
		writeJSValue(id);
		write("\");\n");
		if(thumbId!=null)
		{
			write("\tpage.components[\"");
			writeJSValue(id);
			write("\"].setImage(document.getElementById(\"");
			writeJSValue(thumbId);
			write("\"));\n");
		}
		for(ImageLoaderLauncher ill: imagesToLoad)
		{
			ill.launch(this);
		}
		setParent(null);
	}
}
