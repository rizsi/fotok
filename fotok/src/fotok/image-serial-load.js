class ImageSerialLoad
{
	constructor(nThread)
	{
		this.nAvailableThread=nThread;
		this.wait=[];
	}
	loadImage(domId, src)
	{
		var img=document.getElementById(domId);
		if(img)
		{
			img.addEventListener("load", this.loaded.bind(this), false);
			if(this.nAvailableThread>0)
			{
				img.src=src;
				this.nAvailableThread--;					
				console.info("Load image: "+domId+" "+src);
			}else
			{
				this.wait.push({img: img, src: src});
			}
		}
	}
	loaded()
	{
		console.info("Image loaded!");
		this.nAvailableThread++;
		this.startWaiting();
	}
	startWaiting()
	{
		while(this.nAvailableThread>0 && this.wait.length>0)
		{
			var v=this.wait[0];
			this.wait=this.wait.slice(1,this.wait.length);
			var img=v.img;
			var src=v.src;
			img.src=src;
			this.nAvailableThread--;					
			console.info("Load image: "+img.id+" "+src);
		}
	}
}
