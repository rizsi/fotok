package fotok.database;

import java.sql.SQLException;

import com.jspa.commons.sql.MultiSQLTemplate;

public class ClearCache extends MultiSQLTemplate {

	@Override
	protected void doExecute() throws SQLException {
		write("delete from files;\n");
		executeAsPreparedStatementInt(conn);
		write("delete from processed;\n");
		executeAsPreparedStatementInt(conn);
	}

}
