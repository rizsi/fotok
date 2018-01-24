package fotok;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import hu.qgears.quickjs.qpage.HtmlTemplate;

public class SimpleHttpPage extends HtmlTemplate implements Cloneable
{
	public AbstractHandler createHandler()
	{
		return new AbstractHandler() {
			
			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
					throws IOException, ServletException {
				try {
					SimpleHttpPage p=(SimpleHttpPage)SimpleHttpPage.this.clone();
					p.handle(target, baseRequest, request, response);
				} catch (CloneNotSupportedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
	}
	protected Request baseRequest;
	protected HttpServletResponse response;
	protected void handle(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		this.baseRequest=baseRequest;
		this.response=response;
		response.setContentType("text/html; charset=utf-8");
		
		try(final Writer wr=new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))
		{
			setWriter(wr);
			handlePage();
			baseRequest.setHandled(true);
		}
	}

	protected void handlePage() {
		write("<!DOCTYPE html>\n<html>\n<head>\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n");
		writeHeaders();
		write("</head>\n<body>\n");
		writeBody();
		write("</body>\n</html>\n");
	}

	protected void writeBody() {
		// TODO Auto-generated method stub
		
	}

	protected void writeHeaders() {
		// TODO Auto-generated method stub
		
	}
}
