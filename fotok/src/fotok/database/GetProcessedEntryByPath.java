package fotok.database;

import java.sql.SQLException;

import com.jspa.commons.sql.MultiSQLTemplate;

import hu.qgears.images.SizeInt;

public class GetProcessedEntryByPath extends MultiSQLTemplate
{
	private String path;
	public String typeName;
	public String hash;
	public int width=1;
	public int height=1;
	public int n=0;

	public GetProcessedEntryByPath(String path) {
		super();
		this.path=path;
	}


	@Override
	protected void doExecute() throws SQLException {
		write("select processed.typeName, processed.md5sum, processed.width, processed.height from files, processed  where \n\tfiles.path=");
		writeSQLValue(path);
		write(" and \n\tfiles.md5sum=processed.md5sum");
		executeAsPreparedStatementResultSet(conn, rs->{
			while(rs.next())
			{
//				SQLTemplate.dumpResultSetLine(rs);
				n++;
				typeName=rs.getString(1);
				hash=rs.getString(2);
				width=rs.getInt(3);
				height=rs.getInt(4);
			}
		});
	}


	public SizeInt getSize() {
		return new SizeInt(width, height);
	}
}
