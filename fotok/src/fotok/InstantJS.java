package fotok;

import java.io.Writer;

import hu.qgears.quickjs.qpage.HtmlTemplate;

abstract public class InstantJS extends HtmlTemplate{

	public InstantJS() {
		super();
	}

	public InstantJS(HtmlTemplate parent) {
		super(parent);
	}

	public InstantJS(Writer out) {
		super(out);
	}
	public abstract void generate();
}
