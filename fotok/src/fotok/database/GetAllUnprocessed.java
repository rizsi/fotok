package fotok.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.jspa.commons.sql.MultiSQLTemplate;

import hu.qgears.commons.Pair;

/**
 * TODO remove
 */
public class GetAllUnprocessed extends MultiSQLTemplate
{
	public List<Pair<String, String>> pathToHash=new ArrayList<>();
	@Override
	protected void doExecute() throws SQLException {
		write("SELECT  path, hash FROM    files WHERE NOT EXISTS\n    (SELECT hash\n     FROM processed\n     WHERE processed.hash = files.hash\n    )\n");
	}
}
