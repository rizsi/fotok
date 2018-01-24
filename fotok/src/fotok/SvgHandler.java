package fotok;

import java.net.URL;

import hu.qgears.quickjs.utils.JSHandler;

public class SvgHandler extends JSHandler
{

	@Override
	protected URL findResource(String pathinfo) {
		switch (pathinfo) {
		case "/folder.svg":
		case "/Image-missing.svg":
			return getClass().getResource(pathinfo.substring(1));
		default:
			break;
		}
		return null;
	}

	@Override
	protected String getMimeType() {
		return "image/svg+xml";
	}
}
