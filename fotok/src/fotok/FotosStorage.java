package fotok;

import java.io.File;

import org.eclipse.jetty.server.Request;

import fotok.Fotok.Args;

public class FotosStorage {
	public File images;
	public File cache;
	public FotosFolder root;
	public final Args args;
	public FotosStorage(Args args, File images, File cache) {
		super();
		this.args=args;
		this.images = images;
		this.cache = cache;
		root=new FotosFolder(this, new Path("/")).setRoot(true);
	}
	public ResolvedQuery resolve(Path p, Request baseRequest) {
		FotosFolder resolved=root;
		FotosFile f;
		String localName;
		if(p.folder)
		{
			if(p.pieces.size()==0)
			{
				resolved=root;
			}else
			{
				resolved=new FotosFolder(this, p);
			}
			localName="/";
			f=resolved;
		}else
		{
			resolved=new FotosFolder(this, p.getFolderOnly());
			localName="/"+p.getFileName();
			f=FotosFile.create(resolved, p.getFileName());
		}
		ResolvedQuery ret=new ResolvedQuery(resolved, f, localName);
		if("true".equals(baseRequest.getParameter("edit")))
		{
			ret.editModeAsked=true;
		}
		return ret;
	}
}
