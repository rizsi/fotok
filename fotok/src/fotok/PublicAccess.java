package fotok;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import fotok.Authenticator.Mode;
import fotok.Fotok.Args;
import hu.qgears.commons.UtilString;

public class PublicAccess extends AbstractHandler {
	Args args;
	Fotok fotok;
	

	public PublicAccess(Args args, Fotok fotok) {
		super();
		this.args = args;
		this.fotok = fotok;
	}


	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		List<String> pieces=UtilString.split(target, "/");
		boolean folder=target.endsWith("/");
		if(pieces.size()>0)
		{
			String accessSecret=pieces.get(0);
			// System.out.println("public access: "+accessSecret);
			String path=fotok.da.getPublicAccessManager().getPath(accessSecret);
			if(path!=null&&path.length()>0)
			{
				// System.out.println("access path: "+path);
				String rewrittenpath=path+UtilString.concat(pieces.subList(1, pieces.size()), "/")+(folder?"/":"");
				if(rewrittenpath.endsWith("//"))
				{
					rewrittenpath=rewrittenpath.substring(0, rewrittenpath.length()-1);
				}
				// System.out.println("rewritten path: "+rewrittenpath);
				baseRequest.setContextPath(rewrittenpath);
				try
				{
					Authenticator.setAccessMode(baseRequest, Mode.ro);
					Authenticator.setPublicAccessMode(baseRequest);
					fotok.handle(rewrittenpath, baseRequest, request, response);
				}finally
				{
					Authenticator.setAccessMode(baseRequest, null);
					baseRequest.setContextPath(target);
				}
			}
		}
	}

}
