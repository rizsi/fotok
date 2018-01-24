package fotok;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import javax.servlet.http.HttpSession;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import fotok.Fotok.Args;
import hu.qgears.commons.UtilFile;
import hu.qgears.quickjs.qpage.IInMemoryPost;
import hu.qgears.quickjs.qpage.QPageManager;
import hu.qgears.quickjs.utils.HttpSessionQPageManager;
import hu.qgears.quickjs.utils.InMemoryPost;

public class Login extends SimpleHttpPage{
	
	private Args args;
	
	public Login(Args args) {
		super();
		this.args = args;
	}
	@Override
	protected void writeHeaders() {
		try {
			write("    <meta name=\"google-signin-scope\" content=\"profile email\">\n    <meta name=\"google-signin-client_id\" content=\"");
			writeHtml(getClientId());
			write("\">\n    <script src=\"https://apis.google.com/js/platform.js\" async defer></script>\n");
			super.writeHeaders();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private String getClientId() throws IOException {
		Fotok.Args args=getArgs();
		String clid=UtilFile.loadAsString(new File(args.googleAuthData, "clientid"));
		return clid;
	}
	private Args getArgs() {
		return args;
	}
	@Override
	protected void handlePage() {
		if("POST".equals(baseRequest.getMethod()))
		{
			try {
				InMemoryPost post=new InMemoryPost(baseRequest);
				if(post.getParameter("id_token")!=null)
				{
					login(baseRequest.getSession(), post);
				}else if("true".equals(post.getParameter("signout")))
				{
					logout(baseRequest.getSession(), post);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else
		{
			super.handlePage();
		}
	}

	private void logout(HttpSession session, InMemoryPost post) {
		QPageManager qpage=HttpSessionQPageManager.getManager(session);
		User.set(qpage, null);
	}
	private Object login(HttpSession sess, IInMemoryPost q) {
		QPageManager qpage=HttpSessionQPageManager.getManager(sess);
		try {
			String token=q.getParameter("id_token");
			System.out.println("Try to log in: "+q.getParameter("profileEmail"));
			NetHttpTransport transport = new NetHttpTransport();
			GsonFactory mJFactory = new GsonFactory();
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, mJFactory)
				    // Specify the CLIENT_ID of the app that accesses the backend:
				    .setAudience(Collections.singletonList(getClientId()))
				    // Or, if multiple clients access the backend:
				    //.setAudience(Arrays.asList(CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3))
				    .build();

				// (Receive idTokenString by HTTPS POST)

				GoogleIdToken idToken = verifier.verify(token);
				if (idToken != null) {
				  Payload payload = idToken.getPayload();

				  // Print user identifier
				  String userId = payload.getSubject();
				  System.out.println("User ID: " + userId);

				  // Get profile information from payload
				  String email = payload.getEmail();
				  boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());
				  String name = (String) payload.get("name");
				  String pictureUrl = (String) payload.get("picture");
				  String locale = (String) payload.get("locale");
				  String familyName = (String) payload.get("family_name");
				  String givenName = (String) payload.get("given_name");
				  if(emailVerified)
				  {
					  User user=new User(userId, name, givenName, familyName, pictureUrl, email, token, locale);
					  User.set(qpage, user);
				  }else
				  {
					  System.out.println("Email is not verified. Login is rejected: "+email);
				  }
				  // Use or store profile information
				  // ...
				} else {
				  System.out.println("Invalid ID token.");
				}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	@Override
	protected void writeBody() {
		write("<h1>Fot\u00F3k\u00F6nyv at rizsi.com</h1>\nPlease log in with your Google account to access content! If you found this page accidentally then most likely you have no access rights on this page.\n    <div class=\"g-signin2\" data-onsuccess=\"onSignIn\" data-theme=\"dark\"></div>\n    <script>\n    function send(FD)\n    {\n    \t\tvar xhr = new XMLHttpRequest();\n\t\t\txhr.qpage=this;\n\t\t\txhr.responseType = \"text\";\n\t\t\txhr.onreadystatechange = function() {\n\t\t\t\tif (this.readyState == 4) {\n\t\t\t\t\tif(this.status == 200)\n\t\t\t\t\t{\n\t\t\t\t\t\tvar page=this.qpage;\n\t\t\t\t\t\teval(this.responseText);\n\t\t\t\t\t}\n\t\t\t\t\telse\n\t\t\t\t\t{\n\t\t\t\t\t\tthis.qpage.dispose(\"Server communication XHR fault. Status code: \"+this.status);\n\t\t\t\t\t}\n\t\t\t\t}\n\t\t\t}.bind(xhr);\n\t\t\txhr.open(\"POST\",'?login=true');\n\t\t\txhr.send(FD);\n    }\n      function onSignIn(googleUser) {\n      \tvar fd = new FormData();\n      \n        var profile = googleUser.getBasicProfile();\n        console.log(\"Google API login attempt: ID: \" + profile.getId());\n        console.log(\"ID: \" + profile.getId());\n        console.log('Full Name: ' + profile.getName());\n        console.log('Given Name: ' + profile.getGivenName());\n        console.log('Family Name: ' + profile.getFamilyName());\n        console.log(\"Image URL: \" + profile.getImageUrl());\n        console.log(\"Email: \" + profile.getEmail());\n        fd.append(\"id_token\", googleUser.getAuthResponse().id_token);\n        var id_token = googleUser.getAuthResponse().id_token;\n        console.log(\"ID Token: \" + id_token);\n        send(fd);\n      };\n    </script>\n    <a href=\"#\" onclick=\"signOut();\">Sign out</a><br/>\n    <a href=\"");
		writeHtml(Fotok.clargs.contextPath);
		write("/listing/\">enter</a>\n<script>\n  function signOut() {\n        var fd=new FormData();\n        fd.append(\"signout\", \"true\");\n     \tsend(fd);\n    \tvar auth2 = gapi.auth2.getAuthInstance();\n    \tauth2.signOut().then(function () {\n    });\n  }\n</script>\n    \n");
	}

}
