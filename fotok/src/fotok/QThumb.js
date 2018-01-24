class QThumb extends QComponent
{
	addDomListeners()
	{
		//this.dom.addEventListener("click", this.oninput.bind(this), false);
	}
	oninput()
	{
		//var fd=this.page.createFormData(this);
		// this.page.send(fd);
	}
	scrollIntoView()
	{
		this.dom.scrollIntoView();
	}
}
