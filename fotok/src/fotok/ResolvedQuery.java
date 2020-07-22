package fotok;

import fotok.Authenticator.Mode;

public class ResolvedQuery {
	public FotosFolder folder;
	public String subPath;
	public FotosFile file;
	public Mode mode;
	public boolean editModeAsked;
	public ResolvedQuery(FotosFolder folder, FotosFile file, String subPath) {
		super();
		this.folder = folder;
		this.subPath = subPath;
		this.file=file;
	}
	public boolean isEditModeAsked() {
		return editModeAsked;
	}
}
