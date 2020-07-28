class ArrayView2
{
	constructor(src)
	{
		this.src=src;
		this.columns=5;
		// Options for the observer (which mutations to observe)
		var config = { attributes: true, childList: true, subtree:false };
	
		MutationObserver = window.MutationObserver || window.WebKitMutationObserver;
		var observer = new MutationObserver(this.mutation.bind(this));
		// define what element should be observed by the observer
		// and what types of mutations trigger the callback
		observer.observe(src, config);
		this.index=0;
		this.width=320;
		this.height=250;
		this.src.style.position="relative";
		this.src.style.top="0px";
		this.src.style.left="0px";
		window.addEventListener("resize", this.windowResized.bind(this), false);
	}
	windowResized()
	{
		// console.info("Window resized: "+this.src.clientWidth);
		this.reorganize();
	}
	mutation(mutations, observer)
	{
		observer.takeRecords();
		this.reorganize();
		// console.log(mutations, observer);
	}
	reorganize()
	{
		this.width=Math.floor(this.src.clientWidth/this.columns);
		this.height=this.width;
		this.index=0;
		var arr=[];
		for(var i=0; i<this.src.childNodes.length; ++i)
		{
			var c=this.src.childNodes[i];
			if(c.nodeType==1)
			{
				arr.push(c);
			}
		}
		arr.sort(function(a, b) { return a.id > b.id; });
		for(var i in arr)
		{
			var c=arr[i];
			if(c.nodeType==1)
			{
				var domHolder=c;
				domHolder.style.position="absolute";
				domHolder.style.width=this.width+"px";
				domHolder.style.height=this.height+"px";
				domHolder.style.left=(this.index%this.columns)*this.width+"px";
				domHolder.style.top=Math.floor(this.index/this.columns)*this.height+"px";
				if(domHolder.ArrayViewResizedListener)
				{
					try
					{
						domHolder.ArrayViewResizedListener(this, this.width, this.height);
					} catch(ex)
					{
						console.error(ex);
					}
				}
				this.index++;
			}
		}
		this.src.style.height=(this.index+this.columns-1)/this.columns*this.height+"px";
	}
}

