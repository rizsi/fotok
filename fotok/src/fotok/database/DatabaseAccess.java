package fotok.database;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.sqlite.JDBC;

import com.jspa.commons.sql.ESQLImpl;
import com.jspa.commons.sql.ExtendedConnection;
import com.jspa.commons.sql.MultiSQLTemplate;
import com.jspa.commons.sql.SQLTemplate;

import fotok.ESize;
import fotok.Fotok;
import fotok.PublicAccessManager;
import fotok.formathandler.ExifData;
import fotok.formathandler.ExiftoolProcessor;
import fotok.formathandler.FilesProcessor;
import fotok.formathandler.FormatHandler;
import hu.qgears.images.SizeInt;
import rdupes.RDupesFile;

public class DatabaseAccess {
	public static final String IGNORE_FILE_NAME = ".rdupesignore";

	public int maxPathLength=256;
	private ExtendedConnection conn;
	public final FilesProcessor fp=new FilesProcessor(this);
	private PublicAccessManager publicAccessManager;

	public PublicAccessManager getPublicAccessManager() {
		synchronized (this) {
			if(publicAccessManager==null)
			{
				publicAccessManager=new PublicAccessManager(this);
			}
		}
		return publicAccessManager;
	}

	private void close() throws SQLException {
		conn.close();
	}

	public void start() throws SQLException
	{
		Properties props=new Properties();
		Connection jdbcConn=JDBC.createConnection("jdbc:sqlite:"+Fotok.clargs.sqlFile.getAbsolutePath(), props);
		conn=new ExtendedConnection(jdbcConn, ESQLImpl.sqlite, "", "");
		new CreateTables(this).execute(conn);
		jdbcConn.setAutoCommit(false);
		String version=commit(new GetProperty("version")).value;
		if(version==null)
		{
			// Upgrade from previous version of the software.
			commit(new InsertProperty("version", "1"));
		}
		if(Fotok.clargs.clearCache)
		{
			commit(new ClearCache());
		}
		// commit(new GetAllUnprocessed()).
	}


	public boolean isIgnoreFilesAllowed() {
		return true;
	}


	synchronized public <T extends MultiSQLTemplate> T commit(T sql) throws SQLException {
		SQLTemplate.commit(conn, c->{sql.execute(conn);});
		return sql;
	}
	/**
	 * A file was found in the storage. Check if we already have a processed entry and create one if not.
	 * @param hash
	 * @param ff
	 */
	public void fileFound(String hash, RDupesFile ff) {
		GetProcessedEntry gpe=new GetProcessedEntry(hash);
		try {
			commit(gpe);
			if(gpe.n>0)
			{
				// System.out.println("Already processed: "+ff.getLocalName());
				return;
			}
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			new FormatHandler(this, ff.file.toFile(), hash).run();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * Trigger event that an image with a given hash has finished processing.
	 * @param hash
	 * @param d 
	 */
	public void imageProcessed(String hash, ExifData d) {
		try {
			commit(new InsertFileProcessed(hash, "image", d.date, new SizeInt(d.width, d.height)));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void videoProcessed(String hash, ExiftoolProcessor etp ) {
		try {
			commit(new InsertFileProcessed(hash, "video", etp.date, new SizeInt(etp.width, etp.height)));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public File getPreviewImage(String hash, String type, ESize size) {
		return fp.getPreviewImage(hash, type, size);
	}
	public File getVideoFile(String hash, String typeName) {
		return fp.getVideoFile(hash, typeName);
	}
}
