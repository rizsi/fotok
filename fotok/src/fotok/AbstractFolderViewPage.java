package fotok;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.Request;

import fotok.Authenticator.Mode;
import fotok.QThumb.LabelsGenerator;
import fotok.database.GetAllProcessedEntryByPath;
import fotok.database.GetProcessedEntryByPath;
import hu.qgears.images.SizeInt;
import hu.qgears.quickjs.qpage.HtmlTemplate;
import hu.qgears.quickjs.qpage.QButton;
import hu.qgears.quickjs.qpage.QComponent;
import hu.qgears.quickjs.qpage.QDiv;
import hu.qgears.quickjs.qpage.QPage;
import hu.qgears.quickjs.utils.AbstractQPage;
import hu.qgears.quickjs.utils.ServerLoggerJs;
import hu.qgears.quickjs.utils.UtilHttpContext;

abstract public class AbstractFolderViewPage extends AbstractQPage {
	protected FotosFolder folder;
	protected Map<String, QThumb>thumbs=new TreeMap<>();
	protected Mode mode;
	protected String selectedSize="normal";
	protected String contextPath;
	// The current active viewer
	private Viewer viewer;
	// Initial file that the page was opened with
	private FotosFile file;
	ThumbsHandler thumbsHandler;
	private int index=0;
	private boolean descending;
	public AbstractFolderViewPage(Mode mode, FotosFolder uploadFolder, FotosFile file, ThumbsHandler thumbsHandler) {
		this.mode=mode;
		this.folder=uploadFolder;
		this.thumbsHandler=thumbsHandler;
		this.file=file;
	}

	@Override
	final protected void initQPage(QPage page) {
		page.setScriptsAsSeparateFile(contextPath+Fotok.qScripts);
		installEditModeButtons(page);
		page.submitToUI(new Runnable() {
			@Override
			public void run() {
				setParent(page.getCurrentTemplate());
				refresh();
				updateShares();
				installOfflineHandler();
				if(file!=null&&thumbs!=null&&file.getName()!=null&&file!=folder)
				{
					// Back button take us to the folder - useful on handheld devices
					page.historyReplaceState("folder", "./");
					page.historyPushState(file.getName(), file.getName());
					QThumb thumb=thumbs.get(file.getName());
					if(thumb!=null)
					{
						view(thumb, false);
					}
				}else
				{
				}
				page.historyPopState.addListener(e->{
					if(e.pathname.endsWith("/")&&viewer!=null)
					{
						viewer.deleteWindow(null);
					}else
					{
						String filename=e.pathname.substring(e.pathname.lastIndexOf("/")+1);
						QThumb thumb=thumbs.get(filename);
						if(thumb!=null)
						{
							view(thumb, false);
						}
					}
				});
			}
		});
	}
	private void installOfflineHandler() {
		write("page.setShowStateCallback(function(state){\n\tconsole.info(\"Show state: \"+state);\n\tif(state==2)\n\t{\n\t\tdocument.getElementById(\"alert-offline\").classList.remove(\"hidden\");\n\t\tdocument.getElementById(\"alert-disposed\").classList.add(\"hidden\");\n\t}else if(state==3)\n\t{\n\t\tdocument.getElementById(\"alert-offline\").classList.add(\"hidden\");\n\t\tdocument.getElementById(\"alert-disposed\").classList.remove(\"hidden\");\n\t}else\n\t{\n\t\tdocument.getElementById(\"alert-offline\").classList.add(\"hidden\");\n\t\tdocument.getElementById(\"alert-disposed\").classList.add(\"hidden\");\n\t}\n});\n");
	}
	@Override
	public void setRequest(Request baseRequest, HttpServletRequest request) {
		super.setRequest(baseRequest, request);
		contextPath=UtilHttpContext.getContext(baseRequest);
		System.out.println("Target: "+baseRequest.getPathInfo()+" cp: "+contextPath);
		if("desc".equals(baseRequest.getParameter("order")))
		{
			descending=true;
		}
	}
	abstract protected void installEditModeButtons(QPage page);

	abstract protected void updateShares();

	final protected void refresh() {
		try {
			Map<String, QThumb> toDelete=new HashMap<>(thumbs);
			List<FotosFile> l=folder.listFiles();
			List<GetProcessedEntryByPath> entries=folder.storage.da.commit(new GetAllProcessedEntryByPath(l.stream().map(ff->ff.getSystemPath()).collect(Collectors.toList()))).ret;
			for(int i=0;i<l.size();++i)
			{
				l.get(i).setDate(entries.get(i).date);
				System.out.println(entries.get(i).date);
			}
			Collections.sort(l, new Comparator<FotosFile>() {
				@Override
				public int compare(FotosFile o1, FotosFile o2) {
					return Long.compare(o1.getDate(), o2.getDate());
				}
			});
			if(descending)
			{
				Collections.reverse(l);
			}
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
					QThumb t=new QThumb(page, "thumb-"+String.format("%05d" , index++)+f.getPrefixedName(), folder, f, lg, contextPath, f.getSize());
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
						view.clicked.addListener(ev->view(t, true));
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
		QComponent img=null;
		QButton rotate=null;
		QButton prevImg=null;
		QButton nextImg=null;
		QButton whole;
		QDiv viewerInstance;

		public Viewer(QThumb t, boolean pushHistory) {
			super();
			this.t = t;
			if(pushHistory)
			{
				page.historyPushState(t.f.getName(), t.f.getName());
			}
			t.f.getName();
		}

		public void start() {
			whole=new QButton(page, "viewer");
			rotate=new QButton(whole, "viewer-image-rotate");
			rotate.clicked.addListener(e->{rotate();});
			QButton hideControls=new QButton(whole, "hideControls");
			QButton leave=new QButton(whole, "leaveButton");
			whole.rightClicked.addListener(e->{
				rotate.setStyle("hidden", false);
				leave.setStyle("hidden", false);
				hideControls.setStyle("hidden", false);
			});
			hideControls.clicked.addListener(e->{
				rotate.setStyle("hidden", true);
				leave.setStyle("hidden", true);
				hideControls.setStyle("hidden", true);
			});
			leave.clicked.addListener(e->{
				deleteWindow(leave);
			});
			new DomCreator() {
				@Override
				public void generateDom() {
					write("<div id=\"viewer\" style=\"top: 0; left: 0; width: 100%; height: 100%; position:fixed; color: white; display: block; z-index:1000; background-color:rgba(0,0,0,.8);\">\n<button id=\"");
					writeHtml(rotate.getId());
					write("\" class=\"hidden\" style=\"position: absolute; z-index:2; left:45%; top:10%; width:10%; height:10%;\">Rotate</button>\n<button id=\"");
					writeHtml(leave.getId());
					write("\" class=\"hidden\" style=\"position: absolute; z-index:2; left:45%; top:45%; width:10%; height:10%;\">Leave</button>\n<button id=\"");
					writeHtml(hideControls.getId());
					write("\" class=\"hidden\" style=\"position: absolute; z-index:2; left:45%; top:65%; width:10%; height:10%;\">Hide controls</button>\n</div>\n");
				}
			}.initialize(page, "documentBody");
			// img.src.setPropertyFromServer(t.f.getName());
			generateViews();
			// whole.clicked.addListener(e->deleteWindow(whole));
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
				writeJSValue("viewer-image");
				write("\", \"");
				writeJSValue(newr.getJSClass());
				write("\");\n");
				setParent(null);
			}
		}

		private Object deleteWindow(QButton dummy) {
			if(dummy!=null)
			{
				page.historyPushState("folder", "./");
			}
			whole.dispose();
			new InstantJS(page.getCurrentTemplate()) {
				@Override
				public void generate() {
					write("\tpage.setEnableScroll(true);\n");
				}
			}.generate();
			refresh();
			t.scrollIntoView();
			viewer=null;
			return null;
		}

		private void generateViews() {
			QThumb prev=thumbs.get(t.prevName);
			QThumb next=thumbs.get(t.nextName);
			new DomCreator() {
				@Override
				public void generateDom() {
					write("<div id=\"viewerInstance\" style=\"width:100%;height:100%\"></div>\n");
				}
			}.initialize(page, whole.getId());
			viewerInstance=new QDiv(whole, "viewerInstance");
			viewerInstance.getUserEvent().addListener(e->{
				String swipe=e.getString("swipe");
				if("left".equals(swipe))
				{
					stepTo(next, true);
				}else if("right".equals(swipe))
				{
					stepTo(prev, true);
				}else if("none".equals(swipe) && t.f.isFolder())
				{
					try(ResetOutputObject roo=setParent(page.getCurrentTemplate()))
					{
						write("window.location.href='");
						writeJSValue(t.f.getName());
						write("/';\n");
					}
				}
			});
			img=new QDiv(viewerInstance, generateView(t.f, "viewer-image", "viewerInstance", "viewer-image-image", false));
			prevImg=new QButton(viewerInstance, generateView(prev.f, "viewer-image-prev", "viewerInstance", null, true));
			nextImg=new QButton(viewerInstance, generateView(next.f, "viewer-image-next", "viewerInstance", null, true));
			prevImg.clicked.addListener(e->{prev();});
			nextImg.clicked.addListener(e->{next();});
			
			nextImg.getInitEvent().addListener(e->{
				try(ResetOutputObject roo=setParent(page.getCurrentTemplate()))
				{
					write("new ImageSwipe(page.components[\"");
					writeHtml(viewerInstance.getId());
					write("\"],\n\t\t\tpage.components[\"");
					writeHtml(img.getId());
					write("\"], ");
					writeSize(t.getOriginalSize());//NB
					write(",\n\t\t\tpage.components[\"");
					writeHtml(prevImg.getId());
					write("\"], ");
					writeSize(prev.getOriginalSize());//NB
					write(", \n\t\t\tpage.components[\"");
					writeHtml(nextImg.getId());
					write("\"], ");
					writeSize(next.getOriginalSize());//NB
					write(");\n");
				}
			});
			if(FotosFile.isImage(t.f))
			{
				
			}else
			{
				// rotate.setStyle(style, value);
			}
		}

		private String generateView(FotosFile f, String id, String parent, String imageId, boolean preview)
		{
			List<ImageLoaderLauncher> subImages=new ArrayList<>();
			new DomCreator() {
				@Override
				public void generateDom() {
					if(f.isFolder())
					{
						subImages.addAll(new FolderPreview(this).generatePreview(folder, (FotosFolder)f, false, contextPath, id));
					}else if(FotosFile.isImage(f))
					{
						write("\t<img id=\"");
						writeObject(id);
						write("\" src=\"");
						writeHtml(f.getName());
						write("?size=");
						writeObject(selectedSize);
						write("\" class=\"center ");
						writeObject(f.getRotation().getJSClass());
						write("\"></img>\n");
					}
					else if(FotosFile.isVideo(f))
					{
						if(preview)
						{
							write("\t<div id=\"");
							writeObject(id);
							write("\">\n\t<img src=\"");
							writeHtml(f.getName());
							write("?size=");
							writeObject(ESize.thumb);
							write("\" class=\"center ");
							writeObject(f.getRotation().getJSClass());
							write("\" width=\"80%\" height=\"80%\" style=\"position:absolute; top:10%; left:10%;\"></img>\n\t</div>\n");
						}else
						{
							write("<div id=\"");
							writeObject(id);
							write("\">\n<video  width=\"80%\" height=\"80%\" style=\"position:absolute; top:10%; left:10%;\" controls>\n  <source src=\"");
							writeHtml(f.getName());
							write("?video=html5&format=mp4\" type=\"video/mp4\">\nYour browser does not support the video tag.\n</video>\n</div>\n");
						}
					}
					else
					{
						write("<div id=\"");
						writeHtml(id);
						write("\">Unknown file type</div>\n");
					}
				}
			}.initialize(page, parent);
			for(ImageLoaderLauncher ill: subImages)
			{
				ill.launch(page.getCurrentTemplate());
			}
			return id;
		}
		private Object next() {
			return stepTo(t.nextName, true);
		}
		private Object prev() {
			return stepTo(t.prevName, true);
		}
		public Object stepTo(String name, boolean replaceState) {
			QThumb next=thumbs.get(name);
			return stepTo(next, replaceState);
		}
		public Object stepTo(QThumb next, boolean replaceState) {
			img.dispose();
			prevImg.dispose();
			nextImg.dispose();
			viewerInstance.dispose();
			t=next;
			if(replaceState)
			{
				page.historyReplaceState(t.f.getName(), t.f.getName());
			}
			generateViews();
			return null;
		}
	}

	private Object view(QThumb t, boolean pushHistory) {
		if(viewer==null)
		{
			viewer=new Viewer(t, pushHistory);
			viewer.start();
		}else
		{
			viewer.stepTo(t, pushHistory);
		}
		return null;
	}
	public void writeSize(SizeInt originalSize) {
		write("{ width:");
		writeObject(originalSize.getWidth());
		write(",height:");
		writeObject(originalSize.getHeight());
		write("}");
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
		write("</title>\n<script language=\"javascript\" type=\"text/javascript\">\n");
		new ServerLoggerJs().generateWs(this, "globalQPage.comm");
		write("</script>\n<script type=\"text/javascript\" src=\"");
		writeHtml(contextPath+Fotok.fScripts);
		write("/ArrayView.js\"></script>\n<script type=\"text/javascript\" src=\"");
		writeHtml(contextPath+Fotok.fScripts);
		write("/img-resize.js\"></script>\n<script type=\"text/javascript\" src=\"");
		writeHtml(contextPath+Fotok.fScripts);
		write("/image-serial-load.js\"></script>\n");
		additionalHeaders();
		write("<script type=\"text/javascript\">\nglobalImageSerialLoad=new ImageSerialLoad(1);\nwindow.onload=function()\n{\n");
		generateUploadInitializer();
		write("\tvar av=new ArrayView2(document.getElementById(\"content\"));\n\tav.reorganize();\n\tglobalQPage.setNewDomParent(document.getElementById(\"content\"));\n\tdocument.body.id=\"documentBody\";\n};\n</script>\n<style>\nbody, html {\n    height: 100%;\n    margin: 0;\n    padding: 0;\n}\n.thumb-img {\n    max-width: 100%;\n    max-height: 100%;\n}\n.thumb-area {\n\twidth:100%;\n\theight:100%;\n}\nimg\n{\n\timage-orientation: from-image;\n}\nimg.center\n{\n\tdisplay: block;\n\tmargin-left: auto;\n\tmargin-right: auto;\n}\nimg.rotate-90\n{\n\ttransform: rotate(90deg);\n}\nimg.rotate-180\n{\n\ttransform: rotate(180deg);\n}\nimg.rotate-270\n{\n\ttransform: rotate(270deg);\n}\ndiv.center\n{\n\ttext-align: center;\n}\n.hidden\n{\n\tdisplay: none;\n}\n.fixed-top {\n  position: fixed;\n  top: 0;\n  right: 0;\n  left: 0;\n  z-index: 1030;\n}\n</style>\n");
	}
	
	abstract protected void additionalHeaders();
	/**
	 * Override in rw mode
	 */
	abstract protected void generateUploadInitializer();		

	@Override
	final protected void writeBody() {
		User user=User.get(page.getQPageManager());
		write("<div id=\"alert-offline\" class=\"hidden fixed-top\" role=\"alert\">\n  Connection to server temporarily broken!\n</div>\n<div id=\"alert-disposed\" class=\"hidden fixed-top\" role=\"alert\">\n  Connection to server is closed. Try reloading the page!\n</div>\n\n<h1>");
		writeHtml(getTitle());
		write("</h1>\n\n");
		if(user!=null)
		{
			write("<a href=\"");
			writeHtml(contextPath);
			write("/public/login/logout\">logout ");
			writeHtml(user.getEmail());
			write("</a><br/>\n<br/>\n<br/>\n<br/>\n<br/>\n<br/>\n<br/>\n");
		}
		write("\n");
		if(this instanceof FolderViewPageRW)
		{
			write("<a href=\".\">leave edit mode</a><br/><br/><br/><br/>\n");
		}
		if(!folder.isRoot())
		{
			write("<a href=\"..\">Parent folder</a>\n<br/>\n<br/>\n<br/>\n<br/>\n<br/>\n\n");
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
