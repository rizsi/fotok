package fotok;

import java.util.Enumeration;

public class DebugHttpPage extends SimpleHttpPage{

	@Override
	protected void writeBody() {
		Enumeration<String> headernames=baseRequest.getHeaderNames();
		while(headernames.hasMoreElements())
		{
			String headerName=headernames.nextElement();
			Enumeration<String> values=baseRequest.getHeaders(headerName);
			while(values.hasMoreElements())
			{
				String value=values.nextElement();
				writeHtml(headerName);
				write("=");
				writeHtml(value);
				write("</br>\n");
			}
		}
	}
}
