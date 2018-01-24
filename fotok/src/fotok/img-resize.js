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
				var config = { attributes: true, childList: true };
		
				MutationObserver = window.MutationObserver || window.WebKitMutationObserver;
				var observer = new MutationObserver(this.mutation.bind(this));
				// define what element should be observed by the observer
				// and what types of mutations trigger the callback
				observer.observe(par, config);
			}
		}
	}
	mutation()
	{
		console.info("Mutation!");
	}
	onload()
	{
		var img=this.img;
		var par=img.parentNode;
		if(par)
		{
			var iw=img.naturalWidth;
			var ih=img.naturealHeight;
			var w=par.clientWidth;
			var h=par.clientHeight;
			if(iw/ih>w/h)
			{
				img.style.width=""+w+"px";
				img.style.height="";
			}else
			{
				img.style.width="";
				img.style.height=""+h+"px";
			}
		}
	}
}

