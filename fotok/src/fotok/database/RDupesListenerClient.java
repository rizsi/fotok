package fotok.database;

import java.util.TimerTask;

import hu.qgears.commons.UtilTimer;
import rdupes.IHashProvider;
import rdupes.RDupesClient;
import rdupes.RDupesFile;
import rdupes.RDupesPath;

public class RDupesListenerClient implements RDupesClient
{
	private DatabaseAccess db;
	
	public RDupesListenerClient(DatabaseAccess db) {
		super();
		this.db = db;
	}

	@Override
	public void fileVisited(RDupesFile f) {
		if(f.getSimpleName().endsWith(".part"))
		{
			// ignore modifications on partial upload files
		}
		// Trigger finding of the hash - find it in database or execute hashing.
		f.getHash().doWithHash((ff,hash,origChC, lastModified, fileSize)->{});
	}
	@Override
	public void fileModified(RDupesPath p) {
		if(p instanceof RDupesFile)
		{
			fileVisited((RDupesFile) p);
		}
	}
	@Override
	public IHashProvider startHash(RDupesFile f, long fileSize, long lastModified) {
		DBHashProvider ret=new DBHashProvider(db, f, fileSize, lastModified);
		// In case of a constantly changing file we do not really start hashing because the instances are all cancelled at once.
		// Without this wait it is possible that hashing executes with each uploaded chunk of an image.
		UtilTimer.javaTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				ret.executeHashing();
			}
		}, 2000);
		return ret;
	}
}
