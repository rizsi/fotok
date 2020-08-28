package fotok.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.jspa.commons.sql.MultiSQLTemplate;

public class GetAllProcessedEntryByPath extends MultiSQLTemplate
{
	private List<String> path;
	public List<GetProcessedEntryByPath> ret=new ArrayList<>();

	public GetAllProcessedEntryByPath(List<String> path) {
		super();
		this.path=path;
	}


	@Override
	protected void doExecute() throws SQLException {
		for(String s: path)
		{
			GetProcessedEntryByPath g=new GetProcessedEntryByPath(s);
			g.execute(conn);
			ret.add(g);
		}
	}
}
