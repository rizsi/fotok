package fotok;

import java.util.Date;

/**
 * Parsed EXIF metadata of an image
 */
public class ExifData {
	public int width;
	public int height;
	public Date date;
	@Override
	public String toString() {
		return "["+width+"x"+height+"] "+date;
	}
}
