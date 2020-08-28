package fotok;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import hu.qgears.quickjs.qpage.HtmlTemplate;

public class FolderPreview extends HtmlTemplate{
	
	public FolderPreview() {
		super();
	}

	public FolderPreview(HtmlTemplate parent) {
		super(parent);
	}

	public FolderPreview(Writer out) {
		super(out);
	}

	public List<ImageLoaderLauncher> generatePreview(FotosFolder home, FotosFolder ff, boolean thumbs, String contextPath, String id)
	{
		List<ImageLoaderLauncher> ret=new ArrayList<>(6);
		write("<svg ");
		writeObject(id==null?"":"id="+id);
		write(" width=\"100%\" height=\"100%\">\n  Sorry, your browser does not support inline SVG.\n  <image xlink:href=\"");
		writeHtml(contextPath+Fotok.fImages+"/folder.svg");
		write("\" x=\"0%\" y=\"0%\" height=\"100%\" width=\"100%\"/>\n");
		int n=0;
		for(FotosFile c:ff.iterateFolderSubFotos())
		{
			if(FotosFile.isImage(c))
			{
				int w=26;
				int h=30;
				int m=3;
				int x=(n%3)*(w+m)+5;
				int y=(n/3)*(h+m)+24;
				String path=c.getRelativePath(home);
				String imageId="preview-"+(thumbs?"thumb":"normal")+path;
				String refPath=path+("?size=thumb");
				ret.add(new ImageLoaderLauncher(imageId, refPath, 3));
				write("<rect x=\"");
				writeObject(x-1);
				write("%\" y=\"");
				writeObject(y-1);
				write("%\" height=\"");
				writeObject(h+2);
				write("%\" width=\"");
				writeObject(w+2);
				write("%\" fill=\"black\"/>  \n<image id=\"");
				writeHtml(imageId);
				write("\" xlink:href=\"");
				writeHtml(contextPath+Fotok.fImages+"/Image-missing.svg");
				write("\" x=\"");
				writeObject(x);
				write("%\" y=\"");
				writeObject(y);
				write("%\" height=\"");
				writeObject(h);
				write("%\" width=\"");
				writeObject(w);
				write("%\"/>\n");
				n++;
				if(n>5)
				{
					break;
				}
			}
		}
		write("</svg> \n");
		return ret;
	}
}
