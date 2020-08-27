package fotok.database;

import java.sql.SQLException;

import com.jspa.commons.sql.MultiSQLTemplate;

public class GetFile extends MultiSQLTemplate
{
	private String path;
	private long fileSize;
	private long lastModified;
	
	public String returnMd5Sum;
	private int n=0;

	public GetFile(String path, long fileSize, long lastModified) {
		super();
		this.path = path;
		this.lastModified=lastModified;
		this.fileSize=fileSize;
	}


	@Override
	protected void doExecute() throws SQLException {
		write("delete from files where path=");
		writeSQLValue(path);
		write(" and (fileSize<>");
		writeSQLValue(fileSize);
		write(" or lastModified<>");
		writeSQLValue(lastModified);
		write(")\n");
		int nDeleted=executeAsPreparedStatementInt(conn);
		write("select md5sum, lastModified, fileSize from files where path=");
		writeSQLValue(path);
		write(" and fileSize=");
		writeSQLValue(fileSize);
		write(" and lastModified=");
		writeSQLValue(lastModified);
		write("\n");
		executeAsPreparedStatementResultSet(conn, rs->{
			while(rs.next())
			{
				returnMd5Sum=rs.getString(1);
				n++;
			}
		});
		if(n>1)
		{
			// Multiple matches means invalid database state!
			System.err.println("Invalid database state - multiple matches for file: "+path+" "+fileSize+" "+lastModified+" "+returnMd5Sum);
			returnMd5Sum=null;
			write("delete from files where path=");
			writeSQLValue(path);
			write("\n");
			nDeleted=executeAsPreparedStatementInt(conn);
			System.err.println("Deleted path from database: "+path+" "+nDeleted);
		}
	}
}
