class QThumb extends QComponent
{
	addDomListeners()
	{
		this.dom.ArrayViewResizedListener=this.onresized.bind(this);
		this.clientHeight=300;
		this.clientWidth=300;
		this.imgWidth=300;
		this.imgHeight=300;
		
	}
	onresized(array, w, h)
	{
		// console.info("Thumb resized! "+this.identifier+" "+this.dom.style.width+" "+this.dom.style.height);
		this.clientHeight=Math.floor(h*.9);
		this.clientWidth=w;
		this.relayout();
	}
	setImage(imgDom)
	{
		this.imgDom=imgDom;
		try
		{
			imgDom.addEventListener("load", this.imageloaded.bind(this), false);
		}catch(ex)
		{
			console.error(ex);
		}
		this.imageloaded();
	}
	imageloaded()
	{
		this.imgWidth=this.imgDom.naturalWidth;
		this.imgHeight=this.imgDom.naturalHeight;
		this.relayout();
	}
	relayout()
	{
		if(this.imgDom)
		{
			try
			{
				// console.info("Relayout: "+this.clientWidth+"x"+this.clientHeight+" "+this.imgWidth+"x"+this.imgHeight);
				var size=Math.min(this.clientWidth, this.clientHeight);
				var isize=Math.max(this.imgWidth, this.imgHeight);
				var w=Math.floor(size/isize*this.imgWidth);
				var h=Math.floor(size/isize*this.imgHeight);
				this.imgDom.style.width=w+"px";
				this.imgDom.style.height=h+"px";
				this.imgDom.style.position="absolute";
				this.imgDom.style.top=Math.floor((this.clientHeight-h)/2)+"px";
				this.imgDom.style.left=Math.floor((this.clientWidth-w)/2)+"px";
				this.dom.style.position="absolute";
			}catch(ex)
			{
				console.error(ex);
			}
		}
	}
	scrollIntoView()
	{
		this.dom.scrollIntoView();
	}
	setRotation(id, rot)
	{
		var dom=document.getElementById(id);
		if(dom)
		{
			var cla=dom.classList;
			cla.remove("rotate-0");
			cla.remove("rotate-90");
			cla.remove("rotate-180");
			cla.remove("rotate-270");
			cla.add(rot);
		}
	}
}
