package fotok;

import java.io.File;
import java.io.FileOutputStream;

import fotok.Authenticator.Mode;
import hu.qgears.commons.UtilEventListener;
import hu.qgears.quickjs.qpage.QButton;
import hu.qgears.quickjs.qpage.QDiv;
import hu.qgears.quickjs.qpage.QFileUpload;
import hu.qgears.quickjs.qpage.QLabel;
import hu.qgears.quickjs.qpage.QPage;
import hu.qgears.quickjs.qpage.QTextEditor;

public class FolderViewPageRW extends AbstractFolderViewPage {
	QLabel shares;
	public FolderViewPageRW(ResolvedQuery query, Mode mode, FotosFolder uploadFolder, FotosFile file, ThumbsHandler thumbsHandler) {
		super(query, mode, uploadFolder, file, thumbsHandler);
		this.thumbsHandler=thumbsHandler;
		if(mode!=Mode.rw)
		{
			throw new RuntimeException();
		}
	}

	@Override
	protected void installEditModeButtons(QPage page) {
	}

	private Object share() {
		folder.storage.da.getPublicAccessManager().createShare(folder);
		updateShares();
		return null;
	}

	@Override
	protected void updateShares() {
		if(shares!=null)
		{
			String p=folder.storage.da.getPublicAccessManager().getShare(folder);
			if(p!=null)
			{
				shares.innerhtml.setPropertyFromServer("<a href='"+contextPath+"/public/access/"+p+"/'>"+contextPath+"/public/access/"+p+"/</a>");
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

	private Object rename(QThumb t, QTextEditor name, String newName) {
		if(!newName.equals(t.f.getName()))
		{
			if(t.f.rename(newName))
			{
				refresh();
			}else
			{
				name.text.setPropertyFromServer(t.f.getName());
			}
		}
		return null;
	}
	@Override
	protected void generateThumbLabels(FotosFile f, QThumb t) {
		write("\t<input id=\"");
		writeHtml("name-"+f.getName());
		write("\" size=\"25\"></input><button id=\"delete-");
		writeHtml(f.getName());
		write("\">delete</button>\n");
		if(FotosFile.isImage(f))
		{
			write("\t<button id=\"rotate-");
			writeHtml(f.getName());
			write("\">rotate</button>\n");
		}
	}
	@Override
	protected void setupThumbEditObjects(FotosFile f, QThumb t) {
		QTextEditor name=new QTextEditor(page, "name-"+f.getName());
		name.text.setPropertyFromServer(""+f.getName());
		QButton delete=new QButton(page, "delete-"+f.getName());
		t.addChild(delete);
		delete.clicked.addListener(ev->deleteElement(t));
		if(FotosFile.isImage(f))
		{
			QButton rotate=new QButton(page, "rotate-"+f.getName());
			t.addChild(rotate);
			rotate.clicked.addListener(ev->rotate(t));
		}
		name.enterPressed.addListener(newName->rename(t,name, newName));
		t.addChild(name);
	}

	private Object rotate(QThumb t) {
		t.f.setRotate(t.rotate());
		return null;
	}

	@Override
	protected void additionalHeaders() {
		write("<style>\n.dropping {\n  border: 5px solid blue;\n  width:  200px;\n  height: 100px;\n}\n</style>\n");
	}
	
	@Override
	protected void generateUploadInitializer() {
	}
	private long prevAt=0;
	@Override
	protected void generateBodyPartsEdit() {
		QLabel uploadProgress=new QLabel(page, "uploadProgress");
		QFileUpload fileUpload=new QFileUpload(page);
		fileUpload.setOutputStreamCreator(fu->{
			return new FileOutputStream(new File(folder.getFile(), fu.getFileName()));
		});
		fileUpload.statusUpdated.addListener(fu->{
			if(fu.getAt()<prevAt||fu.getAt()==fu.getFileSize()||fu.getAt()>prevAt+100000)
			{
				prevAt=fu.getAt();
				uploadProgress.innerhtml.setPropertyFromServer(""+fu.getFileName()+" "+fu.getAt()+"/"+fu.getFileSize());
			}
		});
		fileUpload.installDropListener("document.body");
		QButton refresh=new QButton(page, "refresh");
		refresh.clicked.addListener(new UtilEventListener<QButton>() {
			
			@Override
			public void eventHappened(QButton msg) {
				refresh();
			}
		});
		QButton newFolder=new QButton(page, "newFolder");
		newFolder.clicked.addListener(x->newFolder());
		QButton share=new QButton(page, "share");
		share.clicked.addListener(c->share());
		shares=new QLabel(page, "shares");
		write("<button id=\"refresh\" style=\"display:none;\">Refresh</button>\n<button id=\"newFolder\">New folder...</button>\n<br/><br/><br/><br/>\n");
		fileUpload.generateHtmlObject(this);
		write("<br/><br/><br/><br/>\n<button id=\"share\">Share...</button>\n<div id=\"shares\"></div>\n<div id=\"uploadProgress\"></div>\n\n");
	}
}
