package fotok;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import hu.qgears.commons.UtilFile;

public class FotosFile {
	public static final String EXT_ROTATION=".rotation";
	protected FotosStorage storage;
	Path p;
	static Set<String> knowExtensions=new HashSet<>();
	static Set<String> knowVideoExtensions=new HashSet<>();
	{
		knowExtensions.add("jpg");
		knowExtensions.add("jpeg");
		knowExtensions.add("png");
		// Video file extensions that are handled by the program. See VideoHandler class
		knowVideoExtensions.add("mts");
		knowVideoExtensions.add("mov");
		knowVideoExtensions.add("webm");
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
	
	protected File getRotationFile()
	{
		return new File(storage.images, p.toStringPath()+EXT_ROTATION);
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
				for(ESize size: ESize.values())
				{
					File inCache=getCacheFile(size);
					if(inCache.exists())
					{
						UtilFile.deleteRecursive(inCache);
					}
				}
				f.renameTo(newFile);
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
			for(ESize size: ESize.values())
			{
				File inCache=getCacheFile(size);
				if(inCache.exists())
				{
					UtilFile.deleteRecursive(inCache);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * TODO delete resized cached images!
	 * @param size
	 * @return
	 */
	public File getCacheFile(ESize size) {
		Path p=getCacheFilePath(size);
		if(p==null)
		{
			return null;
		}
		return new File(storage.cache, p.toStringPath());
	}
	public File getVideoCacheFile() {
		Path p=getVideoCacheFilePath();
		if(p==null)
		{
			return null;
		}
		return new File(storage.cache, p.toStringPath());
	}
	public Path getCacheFilePath(ESize size) {
		if(size==null)
		{
			return null;
		}
		Path np=new Path(p);
		if(!p.folder)
		{
			String name=size+"-"+np.pieces.get(np.pieces.size()-1);
			np.pieces.set(np.pieces.size()-1, name);
		}
		return np;
	}
	public Path getVideoCacheFilePath() {
		Path np=new Path(p);
		return np;
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
	public static boolean isVideo(File f) {
		String name=f.getName();
		int lastIdx=name.lastIndexOf('.');
		if(lastIdx>=0)
		{
			String ext=name.substring(lastIdx+1);
			ext=ext.toLowerCase(Locale.US);
			return knowVideoExtensions.contains(ext);
		}
		return false;
	}
	public static boolean isImage(FotosFile f) {
		return isImage(f.getFile());
	}
	public boolean isFolder() {
		return false;
	}
	public static boolean isVideo(FotosFile f) {
		return isVideo(f.getFile());
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
	public void setRotate(ERotation rotate) {
		if(isImage(this))
		{
			File rf=getRotationFile();
			try {
				if(rotate==ERotation.rotation0)
				{
					rf.delete();
				}else
				{
					UtilFile.saveAsFile(rf, rotate.name());
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public ERotation getRotation() {
		try {
			File rf=getRotationFile();
			if(rf.exists())
			{
				return ERotation.valueOf(UtilFile.loadAsString(rf));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ERotation.rotation0;
	}
}
