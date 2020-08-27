package fotok.database;

import java.sql.SQLException;

import com.jspa.commons.sql.MultiSQLTemplate;

public class InsertPublicAccess extends MultiSQLTemplate
{
	private String path;
	private String key;
	
	public InsertPublicAccess(String key, String path) {
		super();
		this.path=path;
		this.key=key;
	}


	@Override
	protected void doExecute() throws SQLException {
		write("insert into publicAccess(key, path) values(");
		writeSQLValue(key);
		write(",");
		writeSQLValue(path);
		write(");\n");
		executeAsPreparedStatementInt(conn);
	}
}
