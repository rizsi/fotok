package fotok;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import fotok.Authenticator.Mode;
import hu.qgears.commons.UtilEventListener;
import hu.qgears.quickjs.qpage.QButton;
import hu.qgears.quickjs.qpage.QDiv;
import hu.qgears.quickjs.qpage.QLabel;
import hu.qgears.quickjs.qpage.QPage;
import hu.qgears.quickjs.qpage.QTextEditor;
import hu.qgears.quickjs.upload.UploadHandlerDelegate;
import hu.qgears.quickjs.utils.AbstractQPage;

public class FolderViewPage extends AbstractQPage {
	private FotosFolder folder;
	private UploadHandlerDelegate delegate;
	private Map<String, QThumb>thumbs=new TreeMap<>();
	private Mode mode;
	QLabel shares;
	public FolderViewPage(Mode mode, FotosFolder uploadFolder, UploadHandlerDelegate delegate) {
		this.mode=mode;
		this.folder=uploadFolder;
		this.delegate=delegate;
	}

	@Override
	protected void initQPage(QPage page) {
		page.setScriptsAsSeparateFile(Fotok.clargs.contextPath+Fotok.qScripts);
		QButton refresh=new QButton(page, "refresh");
		refresh.clicked.addListener(new UtilEventListener<QButton>() {
			
			@Override
			public void eventHappened(QButton msg) {
				refresh();
			}
		});
		if(Mode.rw.equals(mode))
		{
			QButton newFolder=new QButton(page, "newFolder");
			newFolder.clicked.addListener(x->newFolder());
			QButton share=new QButton(page, "share");
			share.clicked.addListener(c->share());
			shares=new QLabel(page, "shares");
		}
		page.submitToUI(new Runnable() {
			
			@Override
			public void run() {
				refresh();
				updateShares();
			}
		});
	}
	private Object share() {
		folder.storage.args.getPublicAccessManager().createShare(folder);
		updateShares();
		return null;
	}

	private void updateShares() {
		if(shares!=null)
		{
			String p=folder.storage.args.getPublicAccessManager().getShare(folder);
			if(p!=null)
			{
				shares.innerhtml.setPropertyFromServer("<a href='"+folder.storage.args.contextPath+"/public/access/"+p+"/'>"+folder.storage.args.contextPath+"/public/access/"+p+"/</a>");
			}else
			{
				shares.innerhtml.setPropertyFromServer("");
			}
		}
	}

	private Object newFolder() {
		folder.mkdir("New Folder");
		refresh();
		return null;
	}

	protected void refresh() {
		try {
			Map<String, QThumb> toDelete=new HashMap<>(thumbs);
			List<FotosFile> l=folder.listFiles();
			String prevName=l.size()>0?l.get(l.size()-1).getName():"";
			QThumb prevObject=null;
			for(FotosFile f: l)
			{
				if(prevObject!=null)
				{
					prevObject.nextName=f.getName();
				}
				QThumb exists=thumbs.get(f.getName());
				toDelete.remove(f.getName());
				if(exists==null)
				{
					QThumb t=new QThumb(page, "thumb-"+f.getName(), folder, f);
					exists=t;
					if(mode==Mode.rw)
					{
						t.name=new QTextEditor(page, "name-"+f.getName());
						t.name.text.setPropertyFromServer(""+f.getName());
						t.delete=new QButton(page, "delete-"+f.getName());
						t.addChild(t.delete);
						t.delete.clicked.addListener(ev->deleteElement(t));
						t.name.enterPressed.addListener(newName->rename(t, newName));
						t.addChild(t.name);
					}
					t.l=new QDiv(page, "div-"+f.getPrefixedName());
					t.addChild(t.l);
					QButton view=new QButton(page, "view-"+f.getName());
					new DomCreator() {
						@Override
						public void generateDom() {
							write("<div id=\"div-");
							writeHtml(f.getPrefixedName());
							write("\" style=\"width: 320px; height: 250px;\">\n\t<div id=\"view-");
							writeHtml(f.getName());
							write("\">\n");
							t.generateExampleHtmlObject(this);
							write("\t</div>\n");
							if(mode==Mode.rw)
							{
								write("\t<input id=\"");
								writeHtml("name-"+f.getName());
								write("\" size=\"25\"></input><button id=\"delete-");
								writeHtml(f.getName());
								write("\">delete</button>\n");
							} else {
								write("<div class=\"center\">");
								writeHtml(f.getName());
								write("</div>\n");
							}
							write("</div>\n");
						}
					}.initialize(page, "content");
					thumbs.put(f.getName(), t);
					t.addChild(view);
					view.clicked.addListener(ev->view(t));
				}
				prevObject=exists;
				exists.prevName=prevName;
				prevName=f.getName();
			}
			if(prevObject!=null&&l.size()>0)
			{
				prevObject.nextName=l.get(0).getName();
			}
			for(QThumb t: toDelete.values())
			{
				t.dispose();
				thumbs.remove(t.f.getName());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	class Viewer
	{
		QThumb t;
		QDiv img=null;
		QDiv prevImg=null;
		QDiv nextImg=null;
		public Viewer(QThumb t) {
			super();
			this.t = t;
		}

		public void start() {
			new DomCreator() {
				@Override
				public void generateDom() {
					write("<div id=\"viewer\" style=\"top: 0%; left: 0%; width: 100%; height: 100%; position:absolute; color: white; display: block; z-index:1000; background-color:rgba(0,0,0,.8);\">\n\t<button id=\"viewer-prev\" style=\"z-index:1; position: absolute; top:0px; left:0px; width:10%; height:10%;\"></button>\n\t<button id=\"viewer-next\" style=\"z-index:1; position:absolute; top:0px; left:80%; width:10%; height:10%;\"></button>\n");
					// generateView(t.f);
					write("</div>\n");
				}
			}.initialize(page, "documentBody");
			generateViews();
			// img.src.setPropertyFromServer(t.f.getName());
			QButton d=new QButton(page, "viewer");
			QButton prev=new QButton(page, "viewer-prev");
			prev.clicked.addListener(e->prev());
			QButton next=new QButton(page, "viewer-next");
			next.clicked.addListener(e->next());
			d.clicked.addListener(e->deleteWindow(d));
			new InstantJS(page.getCurrentTemplate()) {
				@Override
				public void generate() {
					write("\tpage.setEnableScroll(false);\n");
				}
			}.generate();
		}
		private Object deleteWindow(QButton d) {
			d.dispose();
			new InstantJS(page.getCurrentTemplate()) {
				@Override
				public void generate() {
					write("\tpage.setEnableScroll(true);\n");
				}
			}.generate();
			return null;
		}

		private void generateViews() {
			generateView(t.f, "viewer-image", "viewer", "viewer-image-image");
			new InstantJS(page.getCurrentTemplate()) {
				@Override
				public void generate() {
					write("new ImageResize(document.getElementById(\"viewer-image-image\"));\t\t\t\t\n");
				}
			}.generate();
			generateView(thumbs.get(t.prevName).f, "viewer-image-prev", "viewer-prev", null);
			generateView(thumbs.get(t.nextName).f, "viewer-image-next", "viewer-next", null);
			img=new QDiv(page, "viewer-image");
			prevImg=new QDiv(page, "viewer-image-prev");
			nextImg=new QDiv(page, "viewer-image-next");
		}

		private void generateView(FotosFile f, String id, String parent, String imageId)
		{
			new DomCreator() {
				@Override
				public void generateDom() {
					write("<div id=\"");
					writeHtml(id);
					write("\" style=\"width:100%; height:100%\">\n");
					if(f.isFolder())
					{
						new FolderPreview(this).generatePreview(folder, (FotosFolder)f, false);
					}else
					{
						write("\t<img ");
						writeObject(imageId==null?"":"id=\""+imageId+"\"");
						write(" src=\"");
						writeHtml(f.getName());
						write("\" style=\"max-width:100%; max-height:100%\" class=\"center\"></img>\n");
					}
					write("\t</div>\n");
				}
			}.initialize(page, parent);
		}
		private Object next() {
			return stepTo(t.nextName);
		}
		private Object prev() {
			return stepTo(t.prevName);
		}
		public Object stepTo(String name) {
			QThumb next=thumbs.get(name);
			img.dispose();
			prevImg.dispose();
			nextImg.dispose();
			t=next;
			generateViews();
			return null;
		}

	}

	private Object view(QThumb t) {
		new Viewer(t).start();
		return null;
	}


	private Object deleteElement(QThumb t) {
		new DomCreator() {
			@Override
			public void generateDom() {
				write("<div id=\"deletequery\" style=\"top: 0%; left: 0%; width: 100%; height: 100%; position:absolute; color: white; display: block; z-index:1000; background-color:rgba(0,0,0,.8);\">\nReally delete?\n<button id=\"deletequerydelete\">Delete</button>\n<button id=\"deletequerycancel\">Cancel</button>\n</div>\n");
			}
		}.initialize(page, "documentBody");
		new InstantJS(page.getCurrentTemplate()) {
			@Override
			public void generate() {
				write("\tpage.setEnableScroll(false);\n");
			}
		}.generate();
		QDiv d=new QDiv(page, "deletequery");
		QButton del=new QButton(page, "deletequerydelete");
		QButton cancel=new QButton(page, "deletequerycancel");
		d.addChild(cancel);
		d.addChild(del);
		d.init();
		del.clicked.addListener(e->delete(t, d));
		cancel.clicked.addListener(e->deleteCancel(t, d));
		return null;
	}
	private void disposeDeleteLayer(QDiv d)
	{
		d.dispose();
		new InstantJS(page.getCurrentTemplate()) {
			@Override
			public void generate() {
				write("\tpage.setEnableScroll(true);\n");
			}
		}.generate();
	}
	private Object deleteCancel(QThumb t, QDiv d) {
		disposeDeleteLayer(d);
		return null;
	}

	private Object delete(QThumb t, QDiv d) {
		disposeDeleteLayer(d);
		t.f.delete();
		refresh();
		return null;
	}

	private Object rename(QThumb t, String newName) {
		if(!newName.equals(t.f.getName()))
		{
			if(t.f.rename(newName))
			{
				refresh();
			}else
			{
				t.name.text.setPropertyFromServer(t.f.getName());
			}
		}
		return null;
	}

	private String getTitle() {
		String folderName=folder.getName();
		return "Fotok: "+(folderName==null?"Root folder":folderName);
	}

	@Override
	protected void writeHeaders() {
		super.writeHeaders();
		write("<title>");
		writeHtml(getTitle());
		write("</title>\n<script type=\"text/javascript\" src=\"");
		writeHtml(Fotok.clargs.contextPath+Fotok.fScripts);
		write("/upload.js\"></script>\n<script type=\"text/javascript\" src=\"");
		writeHtml(Fotok.clargs.contextPath+Fotok.fScripts);
		write("/multiupload.js\"></script>\n<script type=\"text/javascript\" src=\"");
		writeHtml(Fotok.clargs.contextPath+Fotok.fScripts);
		write("/ArrayView.js\"></script>\n<script type=\"text/javascript\" src=\"");
		writeHtml(Fotok.clargs.contextPath+Fotok.fScripts);
		write("/img-resize.js\"></script>\n<script type=\"text/javascript\" src=\"");
		writeHtml(Fotok.clargs.contextPath+Fotok.fScripts);
		write("/image-serial-load.js\"></script>\n<script type=\"text/javascript\">\nglobalImageSerialLoad=new ImageSerialLoad(");
		writeObject(folder.storage.args.nThumbnailThread);
		write(");\nwindow.onload=function()\n{\n");
		if(Mode.rw.equals(mode))
		{
			write("\tvar upl=new MultiUpload(document.getElementById(\"uploadProgress\"), ");
			writeObject(delegate.getMaxChunkSize());
			write(");\n\tupl.installDrop(document.body);\n\tupl.installFileInput(document.getElementById(\"file_input\"));\n\tupl.onFileFinished=function(){globalQPage.components[\"refresh\"].onclick();};\n");
		}
		write("\tvar av=new ArrayView2(document.getElementById(\"content\"), document.getElementById(\"contentOrganized\"));\n\tav.reorganize();\n\tglobalQPage.setNewDomParent(document.getElementById(\"content\"));\n\tdocument.body.id=\"documentBody\";\n};\n</script>\n<style>\nbody, html {\n    height: 100%;\n    margin: 0;\n    padding: 0;\n}\n.dropping {\n  border: 5px solid blue;\n  width:  200px;\n  height: 100px;\n}\n.thumb-img {\n    max-width: 100%;\n    max-height: 100%;\n}\nimg\n{\n\timage-orientation: from-image;\n}\nimg.center\n{\n\tdisplay: block;\n\tmargin-left: auto;\n\tmargin-right: auto;\n}\ndiv.center\n{\n\ttext-align: center;\n}\n</style>\n");
	}
	
	@Override
	protected void writeBody() {
		User user=User.get(page.getQPageManager());
		write("<h1>");
		writeHtml(getTitle());
		write("</h1>\n\n");
		if(user!=null)
		{
			write("<a href=\"");
			writeHtml(Fotok.clargs.contextPath);
			write("/public/login/logout\">logout ");
			writeHtml(user.getEmail());
			write("</a><br/>\n");
		}
		write("\n<button id=\"refresh\" style=\"display:none;\">Refresh</button>\n");
		if(Mode.rw.equals(mode))
		{
			write("<button id=\"newFolder\">New folder...</button>\n<input type=\"file\" id=\"file_input\" multiple><br/>\n<button id=\"share\">Share...</button>\n<div id=\"shares\"></div>\n");
		}
		if(!folder.isRoot())
		{
			write("<a href=\"..\">Parent folder</a>\n");
		}
		write("<div id=\"uploadProgress\"></div>\n<div id=\"contentOrganized\"></div>\n<div id=\"content\">\n");
		// refresh();
		write("</div>\n");
	}
}
