package fotok;

import hu.qgears.quickjs.qpage.HtmlTemplate;

public class ImageLoaderLauncher extends HtmlTemplate{
	private String id;
	private String ref;
	private int priority;
	public ImageLoaderLauncher(String id, String ref, int priority) {
		super();
		this.id = id;
		this.ref = ref;
		this.priority = priority;
	}
	public void launch(HtmlTemplate parent) {
		setParent(parent);
		write("\tglobalImageSerialLoad.loadImage(\"");
		writeJSValue(id);
		write("\", \"");
		writeJSValue(ref);
		write("\", ");
		writeObject(priority);
		write(");\n");
		setParent(null);
	}
}
