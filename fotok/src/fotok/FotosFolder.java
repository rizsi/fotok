package fotok;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import hu.qgears.commons.UtilFile;

public class FotosFolder extends FotosFile {
	class FComp implements Comparator<File>
	{
		private Map<File, String> namecache=new HashMap<File, String>();
		@Override
		public int compare(File o1, File o2) {
			String s1=getName(o1);
			String s2=getName(o2);
			return s1.compareTo(s2);
		}
		private String getName(File o1) {
			String ret=namecache.get(o1);
			if(ret==null)
			{
				ret=(o1.isDirectory()?"A":"B")+o1.getName();
				namecache.put(o1, ret);
			}
			return ret;
		}
		
	}
	class FolderIterateState
	{
		private FotosFolder parent;
		private List<File> files;
		private int at;
		public FolderIterateState(FotosFolder parent, List<File> files, int at) {
			super();
			this.parent=parent;
			this.files = files;
			this.at = at;
		}
		public File getCurrentFile() {
			if(at>=files.size())
			{
				return null;
			}
			return files.get(at);
		}
		public void next() {
			at++;
		}
	}
	class SubFotosIterator implements Iterator<FotosFile>
	{
		private Stack<FolderIterateState> stack=new Stack<>();
		public SubFotosIterator() {
			List<File> ls=UtilFile.listFiles(getFile());
			Collections.sort(ls, new FComp());
			stack.push(new FolderIterateState(FotosFolder.this, ls, 0));
		}
		@Override
		public boolean hasNext() {
			findNextDelegate();
			if(stack.isEmpty())
			{
				return false;
			}
			return true;
		}
		private void findNextDelegate() {
			while(stack.size()>0)
			{
				FolderIterateState fis=stack.peek();
				File f=fis.getCurrentFile();
				if(f==null)
				{
					stack.pop();
					if(stack.size()>0)
					{
						stack.peek().next();
					}
				}else if(f.isDirectory())
				{
					List<File> ls=UtilFile.listFiles(f);
					Collections.sort(ls, new FComp());
					FolderIterateState subState=new FolderIterateState((FotosFolder)FotosFile.create(fis.parent,f), ls, 0);
					stack.push(subState);
				}else if(!FotosFile.isImage(f))
				{
					fis.next();
				}
				else
				{
					return;
				}
			}
		}
		@Override
		public FotosFile next() {
			FolderIterateState s=stack.peek();
			File f=s.getCurrentFile();
			step();
			return FotosFile.create(s.parent, f);
		}
		private void step() {
			if(stack.size()>0)
			{
				stack.peek().next();
			}
			findNextDelegate();
		}
	}
	class SubFotosIterable implements Iterable<FotosFile>
	{

		@Override
		public Iterator<FotosFile> iterator() {
			return new SubFotosIterator();
		}
		
	}
	private boolean root;
	public FotosFolder(FotosStorage storage, Path p) {
		super(storage, p);
	}

	public Iterable<FotosFile> iterateFolderSubFotos()
	{
		return new SubFotosIterable();
	}
	public List<FotosFile> listFiles() {
		List<File> ls=UtilFile.listFiles(getFile());
		Collections.sort(ls, new FComp());
		List<FotosFile> ret=new ArrayList<>();
		for(File f: ls)
		{
			if(!f.getName().endsWith(".part"))
			{
				ret.add(FotosFile.create(this, f));
			}
		}
		return ret;
	}

	public FotosFolder setRoot(boolean root) {
		this.root = root;
		return this;
	}
	public boolean isRoot() {
		return root;
	}

	public boolean exists() {
		return getFile().exists();
	}
	public void mkdir(String string) {
		File f=new File(getFile(), "New Folder");
		f.mkdirs();
	}

	public File getImagesFolder() {
		return getFile();
	}
	public boolean isFolder() {
		return true;
	}

	public void mkdirSelf() {
		getFile().mkdirs();
	}
}
