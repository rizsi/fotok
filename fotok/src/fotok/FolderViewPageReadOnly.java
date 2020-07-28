package fotok;

import fotok.Authenticator.Mode;
import hu.qgears.quickjs.qpage.QPage;

public class FolderViewPageReadOnly extends AbstractFolderViewPage {
	public FolderViewPageReadOnly(Mode mode, FotosFolder uploadFolder, ThumbsHandler thumbsHandler) {
		super(mode, uploadFolder, thumbsHandler);
	}

	@Override
	protected void installEditModeButtons(QPage page) {
	}

	@Override
	protected void updateShares() {
	}

	@Override
	protected void generateThumbLabels(FotosFile f, QThumb t) {
		write("<div class=\"center\">");
		writeHtml(f.getName());
		write("</div>\n");
	}

	@Override
	protected void setupThumbEditObjects(FotosFile f, QThumb t) {
	}

	@Override
	protected void generateUploadInitializer() {		
	}

	@Override
	protected void generateBodyPartsEdit() {
	}

	@Override
	protected void additionalHeaders() {
	}
}
