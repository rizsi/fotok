package fotok;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

public class UploadFileOutputStream extends FileOutputStream {

	private File tmp;
	private File tg;
	public static UploadFileOutputStream create(File f) throws FileNotFoundException
	{
		File tmp=new File(f.getParentFile(), f.getName()+".part");
		f.getParentFile().mkdirs();
		return new UploadFileOutputStream(tmp, f);
	}
	private UploadFileOutputStream(File tmp, File tg) throws FileNotFoundException {
		super(tmp);
		this.tmp=tmp;
		this.tg=tg;
	}
	@Override
	public void close() throws IOException {
		super.close();
		Files.move(tmp.toPath(), tg.toPath());
	}
}
