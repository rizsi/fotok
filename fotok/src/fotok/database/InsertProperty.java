package fotok.database;

import java.sql.SQLException;

import com.jspa.commons.sql.MultiSQLTemplate;

public class InsertProperty extends MultiSQLTemplate
{
	private String value;
	private String key;
	
	public InsertProperty(String key, String value) {
		super();
		this.value=value;
		this.key=key;
	}


	@Override
	protected void doExecute() throws SQLException {
		write("delete from properties where key=");
		writeSQLValue(key);
		write(";\n");
		executeAsPreparedStatementInt(conn);
		write("insert into properties(key, value) values(");
		writeSQLValue(key);
		write(",");
		writeSQLValue(value);
		write(");\n");
		executeAsPreparedStatementInt(conn);
	}
}
