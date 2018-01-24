package fotok;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import hu.qgears.commons.UtilFile;

public class FolderAsZipHandler extends AbstractHandler
{
	private File folder;
	private byte[] buffer=new byte[UtilFile.defaultBufferSize.get()];
	public static class ZipArgs {
		public String addPrefix;

		public ZipArgs(String addPrefix) {
			super();
			this.addPrefix = addPrefix;
		}
		
	}
	
	public FolderAsZipHandler(File folder) {
		super();
		this.folder = folder;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		response.setContentType("application/zip");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);
		try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream(), StandardCharsets.UTF_8)) {
			ZipArgs args=new ZipArgs("");
			handleFolder(folder, zos, args);
		}
	}

	private void handleFolder(File f, ZipOutputStream zos, ZipArgs args) throws IOException {
		if(f.isFile())
		{
			zos.putNextEntry(new ZipEntry(args.addPrefix+"/"+f.getName()));
			doStream(new FileInputStream(f), zos, false, buffer);
		}else
		{
			for(File g:UtilFile.listAllFiles(f))
			{
				ZipArgs subargs=new ZipArgs(args.addPrefix.length()==0?f.getName():(args.addPrefix+"/"+f.getName()));
				handleFolder(g, zos, subargs);
			}
		}
		
	}
	/**
	 * Copy all data from input stream to the output stream using this thread.
	 * @param source
	 * @param target
	 * @param closeOutput target is closed after input was consumed if true
	 * @param bufferSize size of the buffer used when copying
	 * @throws IOException
	 */
	public static void doStream(final InputStream source,
			final OutputStream target, boolean closeOutput, byte[] buffer) throws IOException {
		try
		{
			try {
				int n;
				while ((n = source.read(buffer)) > -1) {
					target.write(buffer, 0, n);
					target.flush();
				}
			} finally {
				if(source!=null)
				{
					source.close();
				}
			}
		}finally
		{
			if(closeOutput&&target!=null)
			{
				target.close();
			}
		}
	}


}
