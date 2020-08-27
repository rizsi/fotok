package fotok.database;

import java.sql.SQLException;

import com.jspa.commons.sql.MultiSQLTemplate;

public class SetRotationByPath extends MultiSQLTemplate
{
	private String path;
	private int rotateEnumOrdinal=0;
	private String hash;

	public SetRotationByPath(String path, int rotateEnumRodinal) {
		super();
		this.path=path;
		this.rotateEnumOrdinal=rotateEnumRodinal;
	}


	@Override
	protected void doExecute() throws SQLException {
		write("select md5sum from files where files.path=");
		writeSQLValue(path);
		write("\n");
		executeAsPreparedStatementResultSet(conn, rs->{
			while(rs.next())
			{
				hash=rs.getString(1);
			}
		});
		if(hash!=null)
		{
			write("delete from rotate where md5sum=");
			writeSQLValue(hash);
			write("\n");
			executeAsPreparedStatementInt(conn);
			write("insert into rotate(md5sum, rotateEnumOrdinal) values (");
			writeSQLValue(hash);
			write(", ");
			writeSQLValue(rotateEnumOrdinal);
			write(") \n");
			executeAsPreparedStatementInt(conn);
		}
	}
}
