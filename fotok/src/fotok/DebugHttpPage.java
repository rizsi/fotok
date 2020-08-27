package fotok;

import java.util.Enumeration;

public class DebugHttpPage extends SimpleHttpPage
{

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
		Fotok f=Authenticator.tlFotok.get();
		write("Images queue length: ");
		writeObject(f.da.fp.imageTasks.size());
		write("</br>\nVideos queue length: ");
		writeObject(f.da.fp.videoTasks.size());
		write("</br>\nProcessed counter: ");
		writeObject(f.da.fp.processedCounter);
		write("</br>\n");
		;
	}
}
