package fotok;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import hu.qgears.commons.UtilFile;

public class FotosFile {
	protected FotosStorage storage;
	Path p;
	static Set<String> knowExtensions=new HashSet<>();
	{
		knowExtensions.add("jpg");
		knowExtensions.add("jpeg");
		knowExtensions.add("png");
	}
	
	public FotosFile(FotosStorage storage, Path p) {
		super();
		this.storage=storage;
		this.p=p;
	}
	public String getName() {
		return p.getFileName();
	}
	public String getPrefixedName() {
		return (isFolder()?"A":"B")+p.getFileName();
	}

	protected File getFile() {
		return new File(storage.images, p.toStringPath());
	}

	public static FotosFile create(FotosFolder parent, File f) {
		return create(parent, f.getName());
	}
	public static FotosFile create(FotosFolder parent, String name) {
		List<String> pieces=new ArrayList<>(parent.p.pieces);
		pieces.add(name);
		File f=new File(parent.getFile(), name);
		if(f.isDirectory())
		{
			return new FotosFolder(parent.storage, new Path(pieces, true));
		}
		return new FotosFile(parent.storage, new Path(pieces, false));
	}
	public boolean rename(String newName) {
		try {
			File f=getFile();
			File newFile=new File(f.getParentFile(), newName);
			if(!newFile.exists())
			{
				File inCache=getCacheFile();
				f.renameTo(newFile);
				if(inCache.exists())
				{
					UtilFile.deleteRecursive(inCache);
				}
				return true;
			}
			return false;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	public void delete() {
		try {
			File f=getFile();
			if(f.isDirectory())
			{
				UtilFile.deleteRecursive(f);
			}else
			{
				f.delete();
			}
			File inCache=getCacheFile();
			if(inCache.exists())
			{
				UtilFile.deleteRecursive(inCache);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public File getCacheFile() {
		Path np=new Path(p);
		if(np.folder)
		{
//			return null;
		}else
		{
//			String last=np.pieces.remove(np.pieces.size()-1);
//			np.pieces.add(last+"-thumb.jpg");
		}
		return new File(storage.cache, np.toStringPath());
	}
	public String getCacheFileName() {
		Path np=new Path(p);
		if(np.folder)
		{
//			np.pieces.add("thumb.jpg");
//			np.folder=false;
		}else
		{
//			String last=np.pieces.remove(np.pieces.size()-1);
//			np.pieces.add(last+"-thumb.jpg");
		}
		return "/"+np.toStringPath();
	}
	public static boolean isImage(File f) {
		String name=f.getName();
		int lastIdx=name.lastIndexOf('.');
		if(lastIdx>=0)
		{
			String ext=name.substring(lastIdx+1);
			ext=ext.toLowerCase(Locale.US);
			return knowExtensions.contains(ext);
		}
		return false;
	}
	public static boolean isImage(FotosFile f) {
		return isImage(f.getFile());
	}
	public boolean isFolder() {
		return false;
	}
	@Override
	public String toString() {
		return ""+getName()+(isFolder()?"/":"");
	}
	public String getRelativePath(FotosFolder home) {
		int n=home.p.pieces.size();
		Path p2=new Path(p);
		p2.pieces=p2.pieces.subList(n, p2.pieces.size());
		return p2.toStringPath();
	}
}
