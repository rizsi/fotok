package fotok.formathandler;

import java.util.Date;

/**
 * Parsed EXIF metadata of an image
 */
public class ExifData {
	public int width;
	public int height;
	public Date date;
	public int orientation = 1;
	@Override
	public String toString() {
		return "["+width+"x"+height+"] "+date;
	}
}
