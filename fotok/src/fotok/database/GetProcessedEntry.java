package fotok.database;

import java.sql.SQLException;

import com.jspa.commons.sql.MultiSQLTemplate;
import com.jspa.commons.sql.SQLTemplate;

public class GetProcessedEntry extends MultiSQLTemplate
{
	private String md5sum;
	public int n=0;

	public GetProcessedEntry(String md5sum) {
		super();
		this.md5sum=md5sum;
	}


	@Override
	protected void doExecute() throws SQLException {
		write("select * from processed where md5sum=");
		writeSQLValue(md5sum);
		executeAsPreparedStatementResultSet(conn, rs->{
			while(rs.next())
			{
				SQLTemplate.dumpResultSetLine(rs);
//				returnMd5Sum=rs.getString(1);
				n++;
			}
		});
	}
}
