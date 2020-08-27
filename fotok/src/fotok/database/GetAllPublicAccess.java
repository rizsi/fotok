package fotok.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.jspa.commons.sql.MultiSQLTemplate;

import hu.qgears.commons.Pair;

public class GetAllPublicAccess extends MultiSQLTemplate
{
	public List<Pair<String, String>> accesses=new ArrayList<>();
	public GetAllPublicAccess() {
		super();
	}

	@Override
	protected void doExecute() throws SQLException {
		write("select key, path from publicAccess\n");
		executeAsPreparedStatementResultSet(conn, rs->{
			while(rs.next())
			{
				accesses.add(new Pair<String, String>(rs.getString(1), rs.getString(2)));
			}
		});
	}
}
