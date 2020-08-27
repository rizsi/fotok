package fotok.database;

import java.sql.SQLException;

import com.jspa.commons.sql.MultiSQLTemplate;

public class InsertFileHash extends MultiSQLTemplate
{
	private String path;
	private String hash;
	private long lastModified;
	private long fileSize;
	
	public InsertFileHash(String path, String hash, long lastModified, long fileSize) {
		super();
		this.path=path;
		this.hash=hash;
		this.lastModified=lastModified;
		this.fileSize=fileSize;
	}


	@Override
	protected void doExecute() throws SQLException {
		System.out.println("insert: "+hash+" "+path+" "+lastModified);
		write("insert into files( md5sum, path, lastModified, fileSize) values(");
		writeSQLValue(hash);
		write(",");
		writeSQLValue(path);
		write(", ");
		writeSQLValue(lastModified);
		write(", ");
		writeSQLValue(fileSize);
		write(");\n");
		executeAsPreparedStatementInt(conn);
	}
}
