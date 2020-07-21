package fotok;

import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jetty.server.Request;

import hu.qgears.commons.UtilString;
import hu.qgears.quickjs.qpage.QPageManager;
import hu.qgears.quickjs.utils.HttpSessionQPageManager;

public class User {
	private String id;
	private String name;
	private String givenName;
	private String familyName;
	private String imageUrl;
	private String email;
	private String token;
	private String locale;
	private Set<String> groups=new TreeSet<>();
	public User(String id, String name, String givenName, String familyName, String imageUrl, String email,
			String token, String locale) {
		super();
		this.id = id;
		this.name = name;
		this.givenName = givenName;
		this.familyName = familyName;
		this.imageUrl = imageUrl;
		this.email = email;
		this.token = token;
		this.locale = locale;
	}
	@Override
	public String toString() {
		return "'"+id+"' '"+name+"' '"+email+"' '"+locale+"' "+getGroupsAsString();
	}
	public String getId() {
		return id;
	}
	public String getName() {
		return name;
	}
	public String getGivenName() {
		return givenName;
	}
	public String getFamilyName() {
		return familyName;
	}
	public String getImageUrl() {
		return imageUrl;
	}
	public String getEmail() {
		return email;
	}
	public String getToken() {
		return token;
	}
	public String getLocale() {
		return locale;
	}
	public static void set(QPageManager qPageManager, User user) {
		 qPageManager.setUserData("user", user);
	}
	public static User get(Request request)
	{
		return get(HttpSessionQPageManager.getManager(request.getSession()));
	}
	public static User get(QPageManager qPageManager)
	{
		return (User) qPageManager.getUserData("user");
	}
	public void setGroups(String groups) {
		this.groups = new TreeSet<>(UtilString.split(groups, " "));
	}
	public String getGroupsAsString() {
		return UtilString.concat(groups, " ");
	}
	public boolean hasRole(String name)
	{
		return groups.contains(name);
	}
}
