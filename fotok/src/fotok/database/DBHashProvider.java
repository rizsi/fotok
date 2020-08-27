package fotok.database;

import java.sql.SQLException;

import rdupes.LazyFileHash;
import rdupes.RDupesFile;

/**
 * In case the path, size and last modification date did not change then 
 * return hash from database instead of re-counting it.
 */
public class DBHashProvider extends LazyFileHash {

	private DatabaseAccess db;
	private long size;
	private long lastModified;
	public DBHashProvider(DatabaseAccess db, RDupesFile rDupesFile, long size, long lastModified) {
		super(rDupesFile, size);
		this.db=db;
		this.size=size;
		this.lastModified=lastModified;
	}
	@Override
	public void executeHashing() {
		try {
			GetFile gf=new GetFile(singleFile.getLocalName(), size, lastModified);
			db.commit(gf);
			if(gf.returnMd5Sum!=null)
			{
				ready(gf.returnMd5Sum);
				db.fileFound(gf.returnMd5Sum, singleFile);
				return;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		doWithHash((ff,hash,origChC, lastModified, fileSize)->{
				if(hash!=null)
				{
					try {
						db.commit(new InsertFileHash(ff.getLocalName(), hash, lastModified, fileSize));
						db.fileFound(hash, ff);
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		super.executeHashing();
	}
}
