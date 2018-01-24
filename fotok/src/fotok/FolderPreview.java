package fotok;

import java.io.Writer;

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

	public void generatePreview(FotosFolder home, FotosFolder ff, boolean thumbs)
	{
		write("<svg height=\"100%\" width=\"100%\">\n  Sorry, your browser does not support inline SVG.\n  <rect width=\"100%\" height=\"100%\" fill=\"lightgreen\"/>  \n");
		int n=0;
		for(FotosFile c:ff.iterateFolderSubFotos())
		{
			if(FotosFile.isImage(c))
			{
				write("<image xlink:href=\"");
				writeHtml(c.getRelativePath(home));
				writeObject(thumbs?"?thumb=1":"");
				write("\" x=\"");
				writeObject((n%3)*33);
				write("%\" y=\"");
				writeObject((n/3)*50);
				write("%\" height=\"50%\" width=\"33%\"/>\n");
				n++;
				if(n>6)
				{
					break;
				}
			}
		}
		write("</svg> \n");
	}
}
