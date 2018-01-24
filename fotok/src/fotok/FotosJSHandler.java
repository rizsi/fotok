package fotok;

import java.net.URL;

import hu.qgears.quickjs.upload.UploadHandler;
import hu.qgears.quickjs.utils.JSHandler;

public class FotosJSHandler extends JSHandler
{

	@Override
	protected URL findResource(String pathinfo) {
		switch (pathinfo) {
		case "/multiupload.js":
		case "/ArrayView.js":
		case "/img-resize.js":
		case "/image-serial-load.js":
			return getClass().getResource(pathinfo.substring(1));
		case "/upload.js":
			return UploadHandler.class.getResource(pathinfo.substring(1));
		default:
			break;
		}
		return null;
	}

}
