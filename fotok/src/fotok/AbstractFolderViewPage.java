package fotok;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import fotok.Authenticator.Mode;
import hu.qgears.commons.UtilEventListener;
import hu.qgears.quickjs.qpage.QButton;
import hu.qgears.quickjs.qpage.QDiv;
import hu.qgears.quickjs.qpage.QPage;
import hu.qgears.quickjs.utils.AbstractQPage;

abstract public class AbstractFolderViewPage extends AbstractQPage {
	protected FotosFolder folder;
	protected Map<String, QThumb>thumbs=new TreeMap<>();
	protected Mode mode;
	protected String selectedSize="normal";
	public AbstractFolderViewPage(Mode mode, FotosFolder uploadFolder) {
		this.mode=mode;
		this.folder=uploadFolder;
	}

	@Override
	final protected void initQPage(QPage page) {
		page.setScriptsAsSeparateFile(Fotok.clargs.contextPath+Fotok.qScripts);
		QButton refresh=new QButton(page, "refresh");
		refresh.clicked.addListener(new UtilEventListener<QButton>() {
			
			@Override
			public void eventHappened(QButton msg) {
				refresh();
			}
		});
		installEditModeButtons(page);
		page.submitToUI(new Runnable() {
			
			@Override
			public void run() {
				refresh();
				updateShares();
			}
		});
	}
	abstract protected void installEditModeButtons(QPage page);

	abstract protected void updateShares();

	final protected void refresh() {
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
					setupThumbEditObjects(f, t);
					t.l=new QDiv(page, "div-"+f.getPrefixedName());
					t.addChild(t.l);
					new DomCreator() {
						@Override
						public void generateDom() {
							write("<div id=\"div-");
							writeHtml(f.getPrefixedName());
							write("\" style=\"width: 320px; height: 250px;\">\n\t<div id=\"view-");
							writeHtml(f.getName());
							write("\" style=\"height: 90%\">\n");
							t.generateExampleHtmlObject(this);
							write("\t</div>\n");
							Writer pre=AbstractFolderViewPage.this.getWriter();
							try
							{
								AbstractFolderViewPage.this.setWriter(getWriter());
								write("\t<div style=\"height:10%\">\n");
								generateThumbLabels(f, t);
								write("\t</div>\n");
							}finally
							{
								AbstractFolderViewPage.this.setWriter(pre);
							}
							write("</div>\n");
						}
					}.initialize(page, "content");
					thumbs.put(f.getName(), t);
					if(FotosFile.isImage(f))
					{
						QButton view=new QButton(page, "view-"+f.getName());
						t.addChild(view);
						view.clicked.addListener(ev->view(t));
					}
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
	abstract protected void generateThumbLabels(FotosFile f, QThumb t);

	abstract protected void setupThumbEditObjects(FotosFile f, QThumb t);
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
					write("<div id=\"viewer\" style=\"top: 0%; left: 0%; width: 100%; height: 100%; position:absolute; color: white; display: block; z-index:1000; background-color:rgba(0,0,0,.8);\">\n\t<button id=\"viewer-prev\" style=\"z-index:1; position: absolute; top:0px; left:0px; width:10%; height:10%;\"></button>\n\t<button id=\"viewer-next\" style=\"z-index:1; position:absolute; top:0px; left:80%; width:10%; height:10%;\"></button>\n</div>\n");
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
			t.scrollIntoView();
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
			List<ImageLoaderLauncher> subImages=new ArrayList<>();
			new DomCreator() {
				@Override
				public void generateDom() {
					write("<div id=\"");
					writeHtml(id);
					write("\" style=\"width:100%; height:100%\">\n");
					if(f.isFolder())
					{
						subImages.addAll(new FolderPreview(this).generatePreview(folder, (FotosFolder)f, false));
					}else
					{
						write("\t<img ");
						writeObject(imageId==null?"":"id=\""+imageId+"\"");
						write(" src=\"");
						writeHtml(f.getName());
						write("?size=");
						writeObject(selectedSize);
						write("\" style=\"max-width:100%; max-height:100%\" class=\"center\"></img>\n");
					}
					write("\t</div>\n");
				}
			}.initialize(page, parent);
			for(ImageLoaderLauncher ill: subImages)
			{
				ill.launch(page.getCurrentTemplate());
			}
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
	private String getName()
	{
		String folderName=folder.getName();
		return (folderName==null?"RootFolder":folderName);
	}
	private String getTitle() {
		String folderName=folder.getName();
		return "Fotok: "+(folderName==null?"Root folder":folderName);
	}

	@Override
	final protected void writeHeaders() {
		super.writeHeaders();
		write("<title>");
		writeHtml(getTitle());
		write("</title>\n<script type=\"text/javascript\" src=\"");
		writeHtml(Fotok.clargs.contextPath+Fotok.fScripts);
		write("/ArrayView.js\"></script>\n<script type=\"text/javascript\" src=\"");
		writeHtml(Fotok.clargs.contextPath+Fotok.fScripts);
		write("/img-resize.js\"></script>\n<script type=\"text/javascript\" src=\"");
		writeHtml(Fotok.clargs.contextPath+Fotok.fScripts);
		write("/image-serial-load.js\"></script>\n");
		additionalHeaders();
		write("<script type=\"text/javascript\">\nglobalImageSerialLoad=new ImageSerialLoad(");
		writeObject(folder.storage.args.nThumbnailThread);
		write(");\nwindow.onload=function()\n{\n");
		generateUploadInitializer();
		write("\tvar av=new ArrayView2(document.getElementById(\"content\"));\n\tav.reorganize();\n\tglobalQPage.setNewDomParent(document.getElementById(\"content\"));\n\tdocument.body.id=\"documentBody\";\n};\n</script>\n<style>\nbody, html {\n    height: 100%;\n    margin: 0;\n    padding: 0;\n}\n.thumb-img {\n    max-width: 100%;\n    max-height: 100%;\n}\nimg\n{\n\timage-orientation: from-image;\n}\nimg.center\n{\n\tdisplay: block;\n\tmargin-left: auto;\n\tmargin-right: auto;\n}\ndiv.center\n{\n\ttext-align: center;\n}\n</style>\n");
	}
	
	abstract protected void additionalHeaders();
	/**
	 * Override in rw mode
	 */
	abstract protected void generateUploadInitializer();		

	@Override
	final protected void writeBody() {
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
		write("<a href=\"");
		writeHtml(getName());
		write(".zip?download=all\" download=\"");
		writeHtml(getName());
		write(".zip\">Download all as ");
		writeHtml(getName());
		write(".zip</a><br/>\n\n<button id=\"refresh\" style=\"display:none;\">Refresh</button>\n");
		generateBodyPartsEdit();
		if(!folder.isRoot())
		{
			write("<a href=\"..\">Parent folder</a>\n");
		}
		write("<div id=\"content\">\n</div>\n");
	}

	/**
	 * Override in RW mode.
	 */
	abstract protected void generateBodyPartsEdit();
}
