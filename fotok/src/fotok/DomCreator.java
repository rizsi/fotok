package fotok;

import java.io.StringWriter;

import hu.qgears.quickjs.qpage.HtmlTemplate;
import hu.qgears.quickjs.qpage.QPage;

abstract public class DomCreator extends HtmlTemplate
{
	public void initialize(QPage page, String parentDomId) {
		if(page!=null)
		{
			if(page.inited)
			{
				setWriter(new StringWriter());
				generateDom();
				String dom=getWriter().toString();
				setParent(page.getCurrentTemplate());
				write("{\n\tvar dom=document.createElement(\"div\");\n\tdom.innerHTML=\"");
				writeJSValue(dom);
				write("\";\ntry\n{\n");
				if(parentDomId!=null)
				{
					write("\tdocument.getElementById(\"");
					writeJSValue(parentDomId);
					write("\").appendChild(page.getFirstRealChildNode(dom));\n");
				} else {
					write("\tpage.getNewDomParent().appendChild(page.getFirstRealChildNode(dom));\n");
				}
				write("}catch(e)\n{\n console.error(e);\n console.error(\"Adding dom to: ");
				writeHtml(""+parentDomId);
				write("\");\n} \n}\n");
				setParent(null);
			}else
			{
				setParent(page.getCurrentTemplate());
				generateDom();
				setParent(null);
			}
		}
	}

	abstract public void generateDom();
}
