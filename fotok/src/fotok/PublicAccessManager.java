package fotok;

import java.io.IOException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import fotok.database.DatabaseAccess;
import fotok.database.GetAllPublicAccess;
import fotok.database.InsertPublicAccess;
import hu.qgears.commons.Pair;

public class PublicAccessManager {
	private DatabaseAccess da;
	Map<String, String> pathToSecret=new HashMap<>();
	Map<String, String> secretToPath=new HashMap<>();
	private final Random random = new SecureRandom();
	public PublicAccessManager(DatabaseAccess da) {
		this.da=da;
	}
	private void reload()
	{
		synchronized (this) {
			secretToPath.clear();
			pathToSecret.clear();
			GetAllPublicAccess gpa=new GetAllPublicAccess();
			try {
				da.commit(gpa);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for(Pair<String, String> p: gpa.accesses)
			{
				String accessSecret=p.getA();
				String path=p.getB();
				secretToPath.put(accessSecret, path);
				pathToSecret.put(path, accessSecret);
			}
		}
	}
	public String getPath(String accessSecret) throws IOException {
		synchronized (this) {
			return secretToPath.get(accessSecret);
		}
	}
	public void createShare(FotosFolder folder) {
		String path="/"+folder.p.toStringPath();
		synchronized (this) {
			String sec=pathToSecret.get(path);
			if(sec==null)
			{
				StringBuilder ret=new StringBuilder();
				for(int j=0;j<6;++j)
				{
					int i=random.nextInt();
					ret.append(String.format("%04X", i&0xFFFF));
					ret.append(String.format("%04X", (i>>16)&0xFFFF));
				}
				String rnadomName=ret.toString();
				try {
					da.commit(new InsertPublicAccess(rnadomName, path));
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				reload();
			}
		}
	}
	public String getShare(FotosFolder folder) {
		String path="/"+folder.p.toStringPath();
		synchronized (this) {
			return pathToSecret.get(path);
		}
	}

}
