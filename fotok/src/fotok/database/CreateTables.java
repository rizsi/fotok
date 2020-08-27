package fotok.database;

import java.sql.SQLException;

import com.jspa.commons.sql.MultiSQLTemplate;

public class CreateTables extends MultiSQLTemplate
{
	private DatabaseAccess da;
	
	public CreateTables(DatabaseAccess da) {
		super();
		this.da = da;
	}

	@Override
	protected void doExecute() throws SQLException {
		write("CREATE TABLE IF NOT EXISTS files (\n\tmd5sum VARCHAR(32) NOT NULL,\n\tpath VARCHAR(");
		writeObject(da.maxPathLength);
		write("),\n\tlastModified INTEGER,\n\tfileSize INTEGER\n\t);\n");
		executeCreateTable();
		write("CREATE TABLE IF NOT EXISTS processed (\n\tmd5sum VARCHAR(32) NOT NULL,\n\t-- Type name of file. null means unknown type. Known types: img, vid\n\ttypeName VARCHAR(");
		writeObject(da.maxPathLength);
		write("),\n\t-- Original date of creation (from image metadata) stored as epoch millis\n\tdate INTEGER,\n\t-- width of image in pixels\n\twidth INTEGER,\n\t-- height of image in pixels\n\theight INTEGER\n\t);\n");
		executeCreateTable();
		write("CREATE TABLE IF NOT EXISTS rotate (\n\tmd5sum VARCHAR(32) NOT NULL,\n\trotateEnumOrdinal INTEGER\n\t);\n");
		executeCreateTable();
		write("CREATE TABLE IF NOT EXISTS publicAccess (\n\tkey VARCHAR(24) NOT NULL,\n\tpath VARCHAR(");
		writeObject(da.maxPathLength);
		write(")\n\t);\n");
		executeCreateTable();
		write("CREATE TABLE IF NOT EXISTS properties (\n\tkey VARCHAR(24) NOT NULL,\n\tvalue VARCHAR(");
		writeObject(da.maxPathLength);
		write(")\n\t);\n");
		executeCreateTable();
	}
	private void executeCreateTable() throws SQLException {
		executeAsStatementInt(conn);
	}
}
