package fotok.database;

import rdupes.IHashProvider;
import rdupes.RDupesClient;
import rdupes.RDupesFile;

public class RDupesListenerClient implements RDupesClient
{
	private DatabaseAccess db;
	
	public RDupesListenerClient(DatabaseAccess db) {
		super();
		this.db = db;
	}

	@Override
	public void fileVisited(RDupesFile f) {
		// Trigger finding of the hash - find it in database or execute hashing.
		f.getHash().doWithHash((ff,hash,origChC, lastModified, fileSize)->{});
	}
	@Override
	public IHashProvider startHash(RDupesFile f, long fileSize, long lastModified) {
		DBHashProvider ret=new DBHashProvider(db, f, fileSize, lastModified);
		ret.executeHashing();
		return ret;
	}
}
