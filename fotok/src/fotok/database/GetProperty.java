package fotok.database;

import java.sql.SQLException;

import com.jspa.commons.sql.MultiSQLTemplate;

public class GetProperty extends MultiSQLTemplate
{
	public String value;
	private String key;
	
	public GetProperty(String key) {
		super();
		this.key=key;
	}


	@Override
	protected void doExecute() throws SQLException {
		write("select value from properties where key=");
		writeSQLValue(key);
		write(";\n");
		executeAsPreparedStatementResultSet(conn, rs->{
			while(rs.next())
			{
				value=rs.getString(1);
			}
		});
	}
}
