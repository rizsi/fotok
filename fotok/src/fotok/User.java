package fotok;

import hu.qgears.quickjs.qpage.QPageManager;

public class User {
	private String id;
	private String name;
	private String givenName;
	private String familyName;
	private String imageUrl;
	private String email;
	private String token;
	private String locale;
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
		return "'"+id+"' '"+name+"' '"+email+"' '"+locale+"'";
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
	public static User get(QPageManager qPageManager)
	{
		return (User) qPageManager.getUserData("user");
	}
}
