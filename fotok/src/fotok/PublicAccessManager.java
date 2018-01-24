package fotok;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import fotok.Fotok.Args;
import hu.qgears.commons.UtilFile;
import hu.qgears.commons.UtilString;

public class PublicAccessManager {
	Args args;
	Map<String, String> pathToSecret=new HashMap<>();
	Map<String, String> secretToPath=new HashMap<>();
	private final Random random = new SecureRandom();
	public PublicAccessManager(Args args) {
		this.args=args;
		reload();
	}
	private void reload()
	{
		synchronized (this) {
			secretToPath.clear();
			pathToSecret.clear();
			for(File f: UtilFile.listFiles(args.publicAccessFolder))
			{
				if(f.isFile())
				{
					String accessSecret=f.getName();
					try {
						String fc = UtilFile.loadAsString(f);
						List<String> lines=UtilString.split(fc, "\r\n");
						String path=lines.get(0);
						secretToPath.put(accessSecret, path);
						pathToSecret.put(path, accessSecret);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
	public String getPath(String accessSecret) throws IOException {
		File f=new File(args.publicAccessFolder, accessSecret);
		if(f.isFile())
		{
			String fc=UtilFile.loadAsString(f);
			List<String> lines=UtilString.split(fc, "\r\n");
			String path=lines.get(0);
			return path;
		}
		return null;
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
				File f=new File(args.publicAccessFolder, rnadomName);
				try {
					UtilFile.saveAsFile(f, path);
				} catch (IOException e) {
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
