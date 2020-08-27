package fotok.database;

import java.sql.SQLException;

import com.jspa.commons.sql.MultiSQLTemplate;

public class GetProcessedEntryByPath extends MultiSQLTemplate
{
	private String path;
	public String typeName;
	public String hash;
	public int n=0;

	public GetProcessedEntryByPath(String path) {
		super();
		this.path=path;
	}


	@Override
	protected void doExecute() throws SQLException {
		write("select processed.typeName, processed.md5sum from files, processed  where \n\tfiles.path=");
		writeSQLValue(path);
		write(" and \n\tfiles.md5sum=processed.md5sum");
		executeAsPreparedStatementResultSet(conn, rs->{
			while(rs.next())
			{
//				SQLTemplate.dumpResultSetLine(rs);
				n++;
				typeName=rs.getString(1);
				hash=rs.getString(2);
			}
		});
	}
}
