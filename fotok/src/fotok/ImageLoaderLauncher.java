package fotok;

import hu.qgears.quickjs.qpage.HtmlTemplate;

public class ImageLoaderLauncher extends HtmlTemplate{
	private String id;
	private String ref;
	public ImageLoaderLauncher(String id, String ref) {
		super();
		this.id = id;
		this.ref = ref;
	}
	public void launch(HtmlTemplate parent) {
		setParent(parent);
		write("\tglobalImageSerialLoad.loadImage(\"");
		writeJSValue(id);
		write("\", \"");
		writeJSValue(ref);
		write("\");\n");
		setParent(null);
	}
	
}
