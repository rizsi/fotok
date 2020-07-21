package fotok;

/**
 * TODO finish video feature.
 * Example command to convert video file to HTML5 capable using ffmpeg:
 * ffmpeg -i 00003.MTS -f webm -vcodec libvpx -acodec libvorbis -ab 128000 -crf 22 00003.webm
 * It is also possible to open VLC onto https:// files
 * 
 * Source for convert command: https://gist.github.com/yellowled/1439610
 * https://gist.github.com/Vestride/278e13915894821e1d6f
 * @author rizsi
 *
 */
public class VideoHandler extends SimpleHttpPage {

	@Override
	protected void writeBody() {
		write("<video width=\"1024\" height=\"768\" controls autoplay>\n  <source src=\"00003.webm\" type=\"video/webm\">\nYour browser does not support the video tag.\n</video>\n");
		super.writeBody();
	}
}
