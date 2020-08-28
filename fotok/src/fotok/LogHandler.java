package fotok;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONArray;

public class LogHandler extends AbstractHandler {
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
//		if(target.equals("/log-handler"))
		{
			String ct=request.getHeader("Content-Type");
			if("application/json;charset=UTF-8".equals(ct))
			{
				try(ServletInputStream sis=baseRequest.getInputStream())
				{
					StringBuilder sb=new StringBuilder();
					Reader r=new InputStreamReader(sis, StandardCharsets.UTF_8);
					int v;
					while((v=r.read())>=0)
					{
//						System.out.print((char) v);
						sb.append((char)v);
					}
					JSONArray arr=new JSONArray(sb.toString());
					for(int i=0;i<arr.length();++i)
					{
						System.out.println(arr.get(i));
					}
				}
				response.setStatus(HttpServletResponse.SC_OK);
			}else
			{
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Content-Type header: "+ct);
			}
			baseRequest.setHandled(true);
		}
	}
}