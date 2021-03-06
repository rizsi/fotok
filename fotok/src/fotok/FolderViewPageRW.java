package fotok;

import java.io.IOException;

import fotok.Authenticator.Mode;
import hu.qgears.quickjs.qpage.QButton;
import hu.qgears.quickjs.qpage.QDiv;
import hu.qgears.quickjs.qpage.QLabel;
import hu.qgears.quickjs.qpage.QPage;
import hu.qgears.quickjs.qpage.QTextEditor;
import hu.qgears.quickjs.upload.UploadHandlerDelegate;

public class FolderViewPageRW extends AbstractFolderViewPage {
	private UploadHandlerDelegate delegate;
	QLabel shares;
	QLabel processing;
	public FolderViewPageRW(Mode mode, FotosFolder uploadFolder, UploadHandlerDelegate delegate) {
		super(mode, uploadFolder);
		if(mode!=Mode.rw)
		{
			throw new RuntimeException();
		}
		this.delegate=delegate;
	}

	@Override
	protected void installEditModeButtons(QPage page) {
		QButton newFolder=new QButton(page, "newFolder");
		newFolder.clicked.addListener(x->newFolder());
		QButton share=new QButton(page, "share");
		share.clicked.addListener(c->share());
		shares=new QLabel(page, "shares");
		QButton processFolder=new QButton(page, "processFolder");
		processFolder.clicked.addListener(x->processFolder());
		processing=new QLabel(page, "processFolderProgress");
		processing.innerhtml.setPropertyFromServer("");
	}
	private Object processFolder() {
		processing.innerhtml.setPropertyFromServer("Finding all files...");
		new Thread("Process folder")
		{
			public void run() {
				int n=0;
				for(FotosFile f: folder.iterateFolderSubFotos())
				{
					n++;
				}
				int i=0;
				int nn=n;
				{
					int ii=i;
					page.submitToUI(new Runnable() {
						@Override
						public void run() {
							processing.innerhtml.setPropertyFromServer("Processed: "+ii+"/"+nn);
						}
					});
				}
				ThumbsHandler th=new ThumbsHandler(folder.storage);
				for(FotosFile f: folder.iterateFolderSubFotos())
				{
					for(ESize s: ESize.values())
					{
						try {
							th.createThumb(f, s);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					i++;
					int ii=i;
					page.submitToUI(new Runnable() {
						@Override
						public void run() {
							processing.innerhtml.setPropertyFromServer("Processed: "+ii+"/"+nn);
						}
					});
				}
			};
		}
		.start();
		return null;
	}

	private Object share() {
		folder.storage.args.getPublicAccessManager().createShare(folder);
		updateShares();
		return null;
	}

	@Override
	protected void updateShares() {
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
	}
	@Override
	protected void setupThumbEditObjects(FotosFile f, QThumb t) {
		QTextEditor name=new QTextEditor(page, "name-"+f.getName());
		name.text.setPropertyFromServer(""+f.getName());
		QButton delete=new QButton(page, "delete-"+f.getName());
		t.addChild(delete);
		delete.clicked.addListener(ev->deleteElement(t));
		name.enterPressed.addListener(newName->rename(t,name, newName));
		t.addChild(name);
	}

	@Override
	protected void additionalHeaders() {
		write("<script type=\"text/javascript\" src=\"");
		writeHtml(Fotok.clargs.contextPath+Fotok.fScripts);
		write("/upload.js\"></script>\n<script type=\"text/javascript\" src=\"");
		writeHtml(Fotok.clargs.contextPath+Fotok.fScripts);
		write("/multiupload.js\"></script>\n<style>\n.dropping {\n  border: 5px solid blue;\n  width:  200px;\n  height: 100px;\n}\n</style>\n");
	}
	
	@Override
	protected void generateUploadInitializer() {
		write("\tvar upl=new MultiUpload(document.getElementById(\"uploadProgress\"), ");
		writeObject(delegate.getMaxChunkSize());
		write(");\n\tupl.installDrop(document.body);\n\tupl.installFileInput(document.getElementById(\"file_input\"));\n\tupl.onFileFinished=function(){globalQPage.components[\"refresh\"].onclick();};\n");
	}
	
	@Override
	protected void generateBodyPartsEdit() {
		write("<button id=\"newFolder\">New folder...</button>\n<input type=\"file\" id=\"file_input\" multiple><br/>\n<button id=\"share\">Share...</button>\n<button id=\"processFolder\">Process...</button><div id=\"processFolderProgress\"></div>\n<div id=\"shares\"></div>\n<div id=\"uploadProgress\"></div>\n");
	}
}
