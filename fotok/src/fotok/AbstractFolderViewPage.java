package fotok;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.Request;

import fotok.Authenticator.Mode;
import fotok.QThumb.LabelsGenerator;
import hu.qgears.quickjs.qpage.HtmlTemplate;
import hu.qgears.quickjs.qpage.QButton;
import hu.qgears.quickjs.qpage.QDiv;
import hu.qgears.quickjs.qpage.QPage;
import hu.qgears.quickjs.utils.AbstractQPage;
import hu.qgears.quickjs.utils.UtilHttpContext;

abstract public class AbstractFolderViewPage extends AbstractQPage {
	protected FotosFolder folder;
	protected Map<String, QThumb>thumbs=new TreeMap<>();
	protected Mode mode;
	protected String selectedSize="normal";
	protected String contextPath;
	public AbstractFolderViewPage(Mode mode, FotosFolder uploadFolder) {
		this.mode=mode;
		this.folder=uploadFolder;
	}

	@Override
	final protected void initQPage(QPage page) {
		page.setScriptsAsSeparateFile(contextPath+Fotok.qScripts);
		installEditModeButtons(page);
		page.submitToUI(new Runnable() {
			@Override
			public void run() {
				refresh();
				updateShares();
			}
		});
	}
	@Override
	public void setRequest(Request baseRequest, HttpServletRequest request) {
		super.setRequest(baseRequest, request);
		contextPath=UtilHttpContext.getContext(baseRequest);
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
					LabelsGenerator lg=new LabelsGenerator() {
						@Override
						public void generateLables(FotosFile f, HtmlTemplate parent, QThumb t) {
							Writer pre=AbstractFolderViewPage.this.getWriter();
							try
							{
								AbstractFolderViewPage.this.setWriter(parent.getWriter());
								generateThumbLabels(f, t);
							}finally
							{
								AbstractFolderViewPage.this.setWriter(pre);
							}
						}
					};
					QThumb t=new QThumb(page, "thumb-"+f.getPrefixedName(), folder, f, lg, contextPath);
					exists=t;
					setupThumbEditObjects(f, t);
					new DomCreator() {
						@Override
						public void generateDom() {
							t.generateHtmlObject(this);
						}
					}.initialize(page, "content");
					thumbs.put(f.getName(), t);
					if(FotosFile.isImage(f)||FotosFile.isVideo(f))
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
		QButton rotate=null;
		QDiv prevImg=null;
		QDiv nextImg=null;
		QButton whole;
		public Viewer(QThumb t) {
			super();
			this.t = t;
		}

		public void start() {
			whole=new QButton(page, "viewer");
			rotate=new QButton(whole, "viewer-image-rotate");
			rotate.clicked.addListener(e->{rotate();});
			new DomCreator() {
				@Override
				public void generateDom() {
					write("<div id=\"viewer\" style=\"top: 0; left: 0; width: 100%; height: 100%; position:fixed; color: white; display: block; z-index:1000; background-color:rgba(0,0,0,.8);\">\n\t<button id=\"viewer-prev\" style=\"z-index:1; position: absolute; top:0px; left:0px; width:10%; height:10%; overflow:hidden;\"></button>\n\t<button id=\"viewer-next\" style=\"z-index:1; position:absolute; top:0px; left:80%; width:10%; height:10%; overflow:hidden;\"></button>\n<button id=\"");
					writeHtml(rotate.getId());
					write("\" style=\"position: absolute; z-index:1; left:50%; top:10%; width=10%;\">Rotate</button>\n</div>\n");
				}
			}.initialize(page, "documentBody");
			// img.src.setPropertyFromServer(t.f.getName());
			generateViews();
			QButton prev=new QButton(whole, "viewer-prev");
			prev.clicked.addListener(e->prev());
			QButton next=new QButton(whole, "viewer-next");
			next.clicked.addListener(e->next());
			whole.clicked.addListener(e->deleteWindow(whole));
			new InstantJS(page.getCurrentTemplate()) {
				@Override
				public void generate() {
					write("\tpage.setEnableScroll(false);\n");
				}
			}.generate();
		}
		private void rotate() {
			ERotation newr=t.rotate();
			if(mode==Mode.rw)
			{
				t.f.setRotate(newr);
			}
			try(ResetOutputObject roo=setParent(page.getCurrentTemplate()))
			{
				write("\tpage.components[\"");
				writeJSValue(t.getId());
				write("\"].setRotation(\"");
				writeJSValue("viewer-image-image");
				write("\", \"");
				writeJSValue(newr.getJSClass());
				write("\");\n");
				setParent(null);
			}
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
			generateView(t.f, "viewer-image", "viewer", "viewer-image-image", false);
			new InstantJS(page.getCurrentTemplate()) {
				@Override
				public void generate() {
					write("new ImageResize(document.getElementById(\"viewer-image-image\"));\t\t\t\t\n");
				}
			}.generate();
			generateView(thumbs.get(t.prevName).f, "viewer-image-prev", "viewer-prev", null, true);
			generateView(thumbs.get(t.nextName).f, "viewer-image-next", "viewer-next", null, true);
			img=new QDiv(whole, "viewer-image");
			prevImg=new QDiv(whole, "viewer-image-prev");
			nextImg=new QDiv(whole, "viewer-image-next");
		}

		private void generateView(FotosFile f, String id, String parent, String imageId, boolean preview)
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
						subImages.addAll(new FolderPreview(this).generatePreview(folder, (FotosFolder)f, false, contextPath));
					}else if(FotosFile.isImage(f))
					{
						write("\t<img ");
						writeObject(imageId==null?"":"id=\""+imageId+"\"");
						write(" src=\"");
						writeHtml(f.getName());
						write("?size=");
						writeObject(selectedSize);
						write("\" style=\"max-width:100%; max-height:100%; position: relative;\" class=\"center ");
						writeObject(f.getRotation().getJSClass());
						write("\"></img>\n");
					}
					else if(FotosFile.isVideo(f))
					{
						if(preview)
						{
							write("VIDEO FILE\n");
						}else
						{
							write("<video width=\"100%\" height=\"100%\" controls>\n  <source src=\"");
							writeHtml(f.getName());
							write("?video=html5\" type=\"video/webm\">\nYour browser does not support the video tag.\n</video>\n");
						}
					}
					else
					{
						write("Unknown file type\n");
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
		writeHtml(contextPath+Fotok.fScripts);
		write("/ArrayView.js\"></script>\n<script type=\"text/javascript\" src=\"");
		writeHtml(contextPath+Fotok.fScripts);
		write("/img-resize.js\"></script>\n<script type=\"text/javascript\" src=\"");
		writeHtml(contextPath+Fotok.fScripts);
		write("/image-serial-load.js\"></script>\n");
		additionalHeaders();
		write("<script type=\"text/javascript\">\nglobalImageSerialLoad=new ImageSerialLoad(");
		writeObject(folder.storage.args.nThumbnailThread);
		write(");\nwindow.onload=function()\n{\n");
		generateUploadInitializer();
		write("\tvar av=new ArrayView2(document.getElementById(\"content\"));\n\tav.reorganize();\n\tglobalQPage.setNewDomParent(document.getElementById(\"content\"));\n\tdocument.body.id=\"documentBody\";\n};\n</script>\n<style>\nbody, html {\n    height: 100%;\n    margin: 0;\n    padding: 0;\n}\n.thumb-img {\n    max-width: 100%;\n    max-height: 100%;\n}\n.thumb-area {\n\twidth:100%;\n\theight:100%;\n}\nimg\n{\n\timage-orientation: from-image;\n}\nimg.center\n{\n\tdisplay: block;\n\tmargin-left: auto;\n\tmargin-right: auto;\n}\nimg.rotate-90\n{\n\ttransform: rotate(90deg);\n}\nimg.rotate-180\n{\n\ttransform: rotate(180deg);\n}\nimg.rotate-270\n{\n\ttransform: rotate(270deg);\n}\ndiv.center\n{\n\ttext-align: center;\n}\n</style>\n");
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
			writeHtml(contextPath);
			write("/public/login/logout\">logout ");
			writeHtml(user.getEmail());
			write("</a><br/>\n");
		}
		write("\n");
		if(this instanceof FolderViewPageRW)
		{
			write("<a href=\".\">leave edit mode</a><br/><br/><br/><br/>\n");
		}
		if(!folder.isRoot())
		{
			write("<a href=\"..\">Parent folder</a>\n");
		}
		write("<div id=\"content\">\n</div>\n<br/>\n<br/>\n<br/>\n<br/>\n<br/>\n<a href=\"");
		writeHtml(getName());
		write(".zip?download=all\" download=\"");
		writeHtml(getName());
		write(".zip\">Download all as ");
		writeHtml(getName());
		write(".zip</a><br/>\n<br/>\n<br/>\n<br/>\n<br/>\n<br/>\n<div id=\"editor-parts\">\n");
		generateBodyPartsEdit();
		if(mode==Mode.rw)
		{
			write("<a href=\"?edit=true\">Edit mode</a>\n");
			
		}
		write("</div>\n<br/>\n<br/>\n<br/>\n<br/>\n<br/>\n");
	}

	/**
	 * Override in RW mode.
	 */
	abstract protected void generateBodyPartsEdit();
}
