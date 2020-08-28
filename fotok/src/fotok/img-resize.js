// TODO delete - no need to resize image automatically

class ImageSwipe
{
	constructor(container, current, cSize, prev, pSize, next, nSize)
	{
		this.cSize=cSize;
		this.nSize=nSize;
		this.pSize=pSize;
		this.nTouch=0;
		this.container=container;
		this.dragging=false;
		this.diff=0;
		this.diffY=0;
		this.current=current;
		this.prev=prev;
		this.next=next;
		this.resize();
		container.dom.addEventListener("mousedown", this.mousedown.bind(this), false);
		container.dom.addEventListener("mousemove", this.mousemove.bind(this), false);
		container.dom.addEventListener("mouseup", this.mouseup.bind(this), false);
		container.dom.addEventListener("mouseleave", this.mouseup.bind(this), false);
		container.dom.addEventListener("touchstart", this.touchstart.bind(this), false);
		container.dom.addEventListener("touchmove", this.touchmove.bind(this), false);
		container.dom.addEventListener("touchcancel", this.touchcancel.bind(this), false);
		container.dom.addEventListener("touchend", this.touchend.bind(this), false);
		// disable drag
		container.dom.ondragstart = function (ev) { return false; }
		//document.body.requestFullscreen();
		window.addEventListener("orientationchange", this.orientationChange.bind(this));
		try{
			new ResizeObserver(this.mutation.bind(this)).observe(container.dom);
		}catch(e)
		{
			console.error("Resize observer add: ", e);
		}
//		this.resizeTimer();
	}
	orientationChange()
	{
		this.resize();
	}
	/** TODO usunsed */
	resizeTimer()
	{
		this.resize();
		setInterval(1000, this.resizeTimer.bind(this));
	}
	mutation()
	{
		// console.info("Size changed: "+this.container.dom.clientWidth+" "+this.container.dom.clientHeight);
		this.resize();
	}
	mousedown(ev)
	{
		// Left mouse only
		if(ev.button==0)
		{
			this.diff=0;
			this.dragging=true;
			this.x0=ev.clientX;
			this.resize();
		}
	}
	mousemove(ev)
	{
		if(this.dragging)
		{
			this.diff=ev.clientX-this.x0;
			this.resize();
		}
	}
	mouseup(ev)
	{
		if(this.dragging)
		{
			if(this.checkSwipe())
			{
				return;
			}
			this.dragging=false;
			this.diff=0;
			this.resize();
		}
	}
	touchstart(evt)
	{
	 this.zoomed=window.innerWidth!=this.container.dom.clientWidth;
	 // console.info("Zoom: "+window.devicePixelRatio+" "+window.innerHeight+" "+window.innerWidth+" "+this.container.dom.clientWidth+" "+this.container.dom.clientHeight);
     var touches = evt.changedTouches;
     this.nTouch+=touches.length;
     // console.log("touchstart: "+this.nTouch+" zoomed: "+this.zoomed+" "+this.zoomed+" "+window.innerWidth+" "+this.container.dom.clientWidth);
	 if(this.nTouch==1 && !this.zoomed)
	 {
	     for (var i = 0; i < touches.length; i++) {
	      this.x0=touches[i].pageX;
	      this.y0=touches[i].pageY;
	      // console.log("touchstart:" + i + " ... "+this.x0);
	     }
	     evt.stopPropagation();
	   this.dragging=true;
	 }else
	 {
	   this.dragging=false;
	   this.diff=0;
		this.diffY=0;
	   this.resize();
	 }
	}
	touchmove(evt)
	{
	  var touches = evt.changedTouches;
     //console.info("touchmove: "+touches.length+" "+this.x0);

	 if(this.nTouch==1 && !this.zoomed)
	 {
	  evt.preventDefault();
		for (var i = 0; i < touches.length; i++) {
     //console.info("touchmove: "+touches.length+" "+this.x0+" "+touches[i].pageX);
			this.diff=touches[i].pageX-this.x0;
		this.diffY=touches[i].pageY-this.y0;
     		//console.info("diff: "+this.diff+" "+this.diffY+" "+this.y0+" "+touches[i].pageY);
			this.resize();
		}
		evt.stopPropagation();
     }
	}
	touchcancel(evt)
	{
	     var touches = evt.changedTouches;
	    this.nTouch-=touches.length;
		this.diff=0;
		this.diffY=0;
		this.resize();
	}
	touchend(evt)
	{
		if(this.nTouch==1 && !this.zoomed)
		{
			if(this.checkSwipe())
			{
				return;
			}
		}
	    var touches = evt.changedTouches;
	    this.nTouch-=touches.length;
		this.diff=0;
		this.diffY=0;
		this.resize();
	}
	checkSwipe()
	{
		var c=this.bestFit(this.cSize);
		if(Math.abs(this.diff)>0.1*c.width)
		{
			this.container.page.sendUserJson(this.container, {swipe:(this.diff<0?"left":"right")});
			return true;
		}
		this.container.page.sendUserJson(this.container, {swipe:"none"});
		return false;
	}
	resize()
	{
		var c=this.bestFit(this.cSize);
		var p=this.bestFit(this.pSize);
		var n=this.bestFit(this.nSize);
		var imgW=900;
		try
		{
			this.prev.dom.style.position="absolute";
			this.prev.dom.style.width=p.width+"px";
			this.prev.dom.style.height=p.height+"px";
			this.prev.dom.style.top=p.top+this.diffY+"px";
			this.prev.dom.style.left=c.left+this.diff-p.width+"px";
		}catch (e){ /*At the fisrt/last element this may fail*/	}
		this.current.dom.style.position="absolute";
		var topV=(c.top+this.diffY)+"px";
		this.current.dom.style.top=topV;
		this.current.dom.style.left=c.left+this.diff+"px";
		this.current.dom.style.width=c.width+"px";
		this.current.dom.style.height=c.height+"px";
		this.current.dom.style.zIndex=1;
		try
		{
		this.next.dom.style.position="absolute";
		this.next.dom.style.top=n.top+this.diffY+"px";
		this.next.dom.style.left=c.left+c.width+this.diff+"px";
		this.next.dom.style.width=n.width+"px";
		this.next.dom.style.height=n.height+"px";
		}catch (e){ /*At the fisrt/last element this may fail*/	}
	}
	bestFit(origSize)
	{
		var tgW=this.container.dom.clientWidth;
		var tgH=this.container.dom.clientHeight;
		var ratio=tgW/tgH;
		var imgRatio=origSize.width/origSize.height;
		if(imgRatio>=ratio)
		{
			return {width:tgW, height:tgW/imgRatio, top:(tgH-tgW/imgRatio)/2, left:0};
		}else
		{
			return {width:tgH*imgRatio, height:tgH, top:0, left:(tgW-tgH*imgRatio)/2};
		}
	}
}

class ImageResize
{
	constructor(img)
	{
		if(img)
		{
			this.active=true;
			this.ooc=this.onorientationchange.bind(this);
			window.addEventListener("orientationchange", this.ooc);
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
	onorientationchange()
	{
		console.log("the orientation of the device is now " + event.target.screen.orientation.angle);
		if(!this.active)
		{
			window.removeEventListener("orientationchange", this.ooc);
		}else
		{
			console.log("the orientation of the device is now " + event.target.screen.orientation.angle);
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

