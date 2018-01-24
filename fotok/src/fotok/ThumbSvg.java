package fotok;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import hu.qgears.quickjs.qpage.HtmlTemplate;

public class ThumbSvg extends HtmlTemplate{
	private List<File> files=new ArrayList<>();
	
	public ThumbSvg(List<File> files) {
		super(new StringWriter());
		this.files = files;
	}

	public String generate()
	{
		write("<!DOCTYPE html>\n<html>\n<body>\n\n<svg height=\"320\" width=\"200\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink= \"http://www.w3.org/1999/xlink\">\n");
		for(int i=0;i<files.size()&& i<10;++i)
		{
			File f=files.get(i);
			write("  <image xlink:href=\"");
			writeHtml(f.getAbsolutePath());
			write("\" x=\"0\" y=\"");
			writeObject(i*50);
			write("\" height=\"50px\" width=\"50px\"/>\n");
		}
		write("</svg> \n \n</body>\n</html>\n");
		return getWriter().toString();
	}
}
