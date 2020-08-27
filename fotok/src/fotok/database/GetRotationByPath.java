package fotok.database;

import java.sql.SQLException;

import com.jspa.commons.sql.MultiSQLTemplate;
import com.jspa.commons.sql.SQLTemplate;

public class GetRotationByPath extends MultiSQLTemplate
{
	private String path;
	public int rotateEnumOrdinal=0;

	public GetRotationByPath(String path) {
		super();
		this.path=path;
	}


	@Override
	protected void doExecute() throws SQLException {
		write("select rotate.rotateEnumOrdinal from files, rotate  where \n\tfiles.path=");
		writeSQLValue(path);
		write(" and \n\tfiles.md5sum=rotate.md5sum");
		executeAsPreparedStatementResultSet(conn, rs->{
			while(rs.next())
			{
				SQLTemplate.dumpResultSetLine(rs);
				rotateEnumOrdinal=rs.getInt(1);
			}
		});
	}
}
