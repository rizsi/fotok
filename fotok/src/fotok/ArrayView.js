class ArrayView
{
	constructor(src, target)
	{
		this.src=src;
		this.target=target;
		// Options for the observer (which mutations to observe)
		var config = { attributes: true, childList: true };
	
		MutationObserver = window.MutationObserver || window.WebKitMutationObserver;
		var observer = new MutationObserver(this.mutation.bind(this));
		// define what element should be observed by the observer
		// and what types of mutations trigger the callback
		observer.observe(src, config);
		this.index=0;
		this.width=320;
		this.height=250;
		this.target.style.position="relative";
		this.target.style.top="0px";
		this.target.style.left="0px";
		
	}
	mutation(mutations, observer)
	{
		observer.takeRecords();
		this.reorganize();
		// console.log(mutations, observer);
	}
	reorganize()
	{
		// this.index=0;
		while(this.src.childNodes.length>0)
		{
			var c=this.src.childNodes[0];
			// console.info("arrayview: "+c);
			this.src.removeChild(c);
			if(c.nodeType==1)
			{
				var domHolder=document.createElement("div");
				 domHolder.appendChild(c);
				 this.target.appendChild(domHolder);
				var domHolder=c;
				domHolder.style.position="absolute";
				domHolder.style.width=this.width+"px";
				domHolder.style.height=this.height+"px";
				domHolder.style.left=(this.index%5)*this.width+"px";
				domHolder.style.top=Math.floor(this.index/5)*this.height+"px";
				this.index++;
			}
		}
	}
}

class ArrayView2
{
	constructor(src, target)
	{
		this.src=src;
		this.target=target;
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
		
	}
	mutation(mutations, observer)
	{
		observer.takeRecords();
		this.reorganize();
		// console.log(mutations, observer);
	}
	reorganize()
	{
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
			// console.info("arrayview: "+c);
			// this.src.removeChild(c);
			if(c.nodeType==1)
			{
				// var domHolder=document.createElement("div");
				// domHolder.appendChild(c);
				// this.target.appendChild(domHolder);
				var domHolder=c;
				domHolder.style.position="absolute";
				domHolder.style.width=this.width+"px";
				domHolder.style.height=this.height+"px";
				domHolder.style.left=(this.index%5)*this.width+"px";
				domHolder.style.top=Math.floor(this.index/5)*this.height+"px";
				this.index++;
			}
		}
	}
}

