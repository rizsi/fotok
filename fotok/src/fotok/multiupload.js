class DecorFileUpload extends FileUpload
{
	constructor(parent, f, cs, dom)
	{
		super(f,cs);
		this.parent=parent;
		this.dom=document.createElement("div");
		this.span=document.createElement("span");
		this.span.innerHTML=f.name;
		this.dom.appendChild(this.span);

		this.stopButton=document.createElement("button");
		this.dom.appendChild(this.stopButton);
		//this.stopButton.style.display="none";
		this.stopButton.innerHTML="Stop upload";
		this.stopButton.addEventListener("click", this.stopClicked.bind(this), false);


		this.restartButton=document.createElement("button");
		this.dom.appendChild(this.restartButton);
		this.restartButton.style.display="none";
		this.restartButton.innerHTML="Restart upload";
		this.restartButton.addEventListener("click", this.restartClicked.bind(this), false);
		dom.appendChild(this.dom);
		this.ready=false;
	}
	restartClicked()
	{
		this.start();
		this.restartButton.style.display="none";
		this.stopButton.style.display="inline";
	}
	stopClicked()
	{
		this.stop();
		this.stopButton.style.display="none";
		this.restartButton.style.display="inline";
	}
	progress(file, bytes)
	{
		this.span.innerHTML=file.name+" - "+bytes+"/"+file.size+" bytes";
	}
	finished(file, bytes)
	{
		this.span.innerHTML=file.name+" - "+bytes+"/"+file.size+" bytes FINISHED";
		this.ready=true;
		this.parent.fileFinished(this);
		this.stopButton.style.display="none";
		console.info("FINISHED: "+file.name);
	}
	error(file, bytes)
	{
		this.span.innerHTML=file.name+" - "+bytes+"/"+file.size+" bytes "+" ERROR";
		this.stopButton.style.display="none";
		this.restartButton.style.display="inline"
	}
	isReady()
	{
		return this.ready;
	}
}

class MultiUpload
{
	constructor(progressDiv, chunkSize)
	{
		this.progressDiv=progressDiv;
		this.chunkSize=chunkSize;
		this.uploads=[];
		this.running=false;
	}
	installDrop(hostDiv)
	{
		// console.info("Drop zone: "+hostDiv);
		hostDiv.addEventListener("drop", this.drop.bind(this), false);
		hostDiv.addEventListener("dragover", this.dragover.bind(this), false);
		hostDiv.addEventListener("dragend", this.dragend.bind(this), false);
	}
	installFileInput(fileInput)
	{
		// console.info("File input: "+fileInput);
		fileInput.addEventListener("change", this.fileInputChange.bind(this), false);
	}
	fileInputChange(ev)
	{
		var files=ev.target.files;
		console.info("file input change: "+files);
		this.launchUpload(files);
	}
	fileFinished(f)
	{
		for(var key in this.uploads)
		{
			var o=this.uploads[key];
			if(!o.isReady())
			{
				o.start();
				this.running=true;
				this.onFileFinished(f);
				return;
			}
		}
		this.running=false;
		this.onFileFinished(f);
	}
	onFileFinished(f)
	{
	}
	launchUpload(files)
	{
		for (var i=0; i < files.length; i++) {
			var f=files[i];
			var skip=false;
			for(var key in this.uploads)
			{
				var o=this.uploads[key];
				if(o.file.name==f.name)
				{
					console.info("Already running: "+f.name);
					skip=true;
				}
			}
			if(!skip)
			{
				var upl=new DecorFileUpload(this, f, this.chunkSize, this.progressDiv);
				this.uploads.push(upl);
		    	console.log("... file[" + i + "].name = " + f.name+" "+f.size);
		    	if(!this.running)
		    	{
		    		upl.start();
		    		this.running=true;
		    	}
		    }
		}  
	}
	drop(ev)
	{
		  console.log("Drop");
		  ev.preventDefault();
		  // If dropped items aren't files, reject them
		  var dt = ev.dataTransfer;
		  if (dt.files) {
		  	this.launchUpload(dt.files);
		  }
	}
	dragover(ev)
	{
		// Prevent default select and drag behavior
		ev.preventDefault();
		console.info("drag over: "+ev.target);
		// ev.target.className="dropping";
	}
	dragend(ev)
	{
//		  console.log("dragEnd");
		  // Remove all of the drag data
//		  var dt = ev.dataTransfer;
//		  if (dt.items) {
		    // Use DataTransferItemList interface to remove the drag data
//		    for (var i = 0; i < dt.items.length; i++) {
//		      dt.items.remove(i);
//		    }
//		  } else {
//		    // Use DataTransfer interface to remove the drag data
//		    ev.dataTransfer.clearData();
//		  }
	}
}
