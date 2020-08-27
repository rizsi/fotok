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
			if(this.nAvailableThread>0)
			{
				this.startLoad(img, src);
			}else
			{
				this.wait.push({img: img, src: src});
			}
		}
	}
	startLoad(img, src)
	{
		img.addEventListener("load", this.loaded.bind(this), false);
		img.addEventListener("error", this.error.bind(this), false);
		if(img.nodeName=="image")
		{
			// image within SVG graphics
			img.setAttributeNS('http://www.w3.org/1999/xlink', 'xlink:href', src);
		}else
		{
			img.src=src;
		}
		this.nAvailableThread--;					
		// console.info("Load image: "+img.id+" "+src+" "+this.nAvailableThread);
	}
	loaded(ev)
	{
		this.nAvailableThread++;
		// console.info("Image loaded! "+ev.target.id+" "+this.nAvailableThread);
		this.startWaiting();
	}
	error(ev)
	{
		this.nAvailableThread++;
		// console.info("Image load ERROR! "+ev.target.id+" "+this.nAvailableThread);
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
			this.startLoad(img, src);
		}
	}
}
