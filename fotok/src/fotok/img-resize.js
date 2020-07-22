// TODO delete - no need to resize image automatically

class ImageResize
{
	constructor(img)
	{
		if(img)
		{
			this.img=img;
			img.onload=this.onload.bind(this);
			console.info("Image resize on: "+img+" "+img.complete+" "+img.width+" "+img.height);
			var par=img.parentNode;
			if(par)
			{
				try{
				new ResizeObserver(this.mutation.bind(this)).observe(par);
				}catch(e)
				{
					console.error("Resize observer add: ", e);
				}
			}
		}
	}
	mutation()
	{
		console.info("Mutation!");
		this.onload();
	}
	onload()
	{
		var img=this.img;
		if (!img.complete) {
			return false;
		}
		var par=img.parentNode;
		if(par)
		{
			var iw=img.naturalWidth;
			var ih=img.naturalHeight;
			var w=par.clientWidth;
			var h=par.clientHeight;
			console.info("img w/h parent w/h: "+iw+"/"+ih+" "+w+"/"+h);
			if(iw/ih>w/h)
			{
				img.style.width=""+w+"px";
				img.style.height="";
				img.style.top=(h-ih*w/iw)/2+"px";
			}else
			{
				img.style.top="0px";
				img.style.width="";
				img.style.height=""+h+"px";
			}
		}
	}
}

