package fotok.database;

import java.sql.SQLException;
import java.util.Date;

import com.jspa.commons.sql.MultiSQLTemplate;

import hu.qgears.images.SizeInt;

public class InsertFileProcessed extends MultiSQLTemplate
{
	private String hash, typeName;
	private Date date;
	private SizeInt size;
	
	public InsertFileProcessed(String hash, String typeName, Date date, SizeInt size) {
		super();
		this.hash=hash;
		this.typeName=typeName;
		this.date=date;
		this.size=size;
	}


	@Override
	protected void doExecute() throws SQLException {
		write("insert into processed ( md5sum, typeName, date, width, height ) values\n\t(");
		writeSQLValue(hash);
		write(", ");
		writeSQLValue(typeName);
		write(", ");
		writeSQLValue(date);
		write(", ");
		writeSQLValue(size.getWidth());
		write(", ");
		writeSQLValue(size.getHeight());
		write(");\n");
		executeAsPreparedStatementInt(conn);
	}
}
