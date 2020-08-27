package fotok.database;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.sqlite.JDBC;

import com.jspa.commons.sql.MultiSQLTemplate;
import com.jspa.commons.sql.SQLTemplate;

import fotok.formathandler.ExifData;
import fotok.formathandler.ExiftoolProcessor;
import fotok.formathandler.FilesProcessor;
import fotok.formathandler.FormatHandler;
import hu.qgears.images.SizeInt;
import rdupes.RDupes;
import rdupes.RDupesFile;

public class DatabaseAccess {
	public static final String IGNORE_FILE_NAME = ".rdupesignore";

	public int maxPathLength=256;
	private Connection conn;
	public final FilesProcessor fp=new FilesProcessor(this);

	public static void main(String[] args) throws Exception {
		Path p=new File("/home/rizsi/tmp/fotok/images").toPath();
		List<Path> initialPath=new ArrayList<>();
		initialPath.add(p);
		DatabaseAccess da=new DatabaseAccess();
		try
		{
			da.start();
			new RDupes().setClient(new RDupesListenerClient(da)).start(1, initialPath);
			while(true)
			{
				Thread.sleep(1000);
			}
		}finally
		{
			da.close();
		}
	}
	private void close() throws SQLException {
		conn.close();
	}

	private void start() throws SQLException
	{
		Properties props=new Properties();
		conn=JDBC.createConnection("jdbc:sqlite:/home/rizsi/tmp/fotok/db.sqlite", props);
		new CreateTables(this).execute(conn);
		conn.setAutoCommit(false);
	}


	public boolean isIgnoreFilesAllowed() {
		return true;
	}


	synchronized public void commit(MultiSQLTemplate sql) throws SQLException {
		SQLTemplate.commit(conn, c->{sql.execute(conn);});
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
				System.out.println("Already processed: "+ff.getLocalName());
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
}
