package fotok;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hu.qgears.commons.UtilString;

public class Path {
	public List<String> pieces;
	public boolean folder;
	
	public Path(List<String> pieces, boolean folder) {
		super();
		this.pieces = pieces;
		this.folder = folder;
	}
	public Path(String path)
	{
		pieces=UtilString.split(path, "/");
		folder=path.endsWith("/");
	}
	public Path(Path p) {
		pieces=new ArrayList<>(p.pieces);
		folder=p.folder;
	}
	public Path(Path p, String relpath) {
		pieces=new ArrayList<>(p.pieces);
		pieces.addAll(UtilString.split(relpath, "/"));
		folder=relpath.endsWith("/");
	}
	public String toStringPath() {
		return UtilString.concat(pieces, "/")+(folder?"/":"");
	}
	public Path remove(int i) {
		pieces.remove(i);
		return this;
	}
	public Path removeLast() {
		if (pieces.size()>0) {
			pieces.remove(pieces.size()-1);
		}
		return this;
	}
	public boolean eq(int i, String path) {
		return (pieces.size()>i)&&pieces.get(i).equals(path) ;
	}
	public Path setFolder(boolean b) {
		folder=b;
		return this;
	}
	@Override
	public String toString() {
		return "PATH: "+pieces+" "+(folder?"folder":"file");
	}
	public String getFileName() {
		if(pieces.size()==0)
		{
			return null;
		}
		return pieces.get(pieces.size()-1);
	}
	public void validate() throws IOException {
		for(String p: pieces)
		{
			if(p.equals(".."))
			{
				throw new IOException("Invlaid path: "+this);
			}
		}
	}
	public Path getFolderOnly() {
		if(folder)
		{
			return this;
		}else
		{
			return new Path(pieces.subList(0, pieces.size()-1), true);
		}
	}
}
