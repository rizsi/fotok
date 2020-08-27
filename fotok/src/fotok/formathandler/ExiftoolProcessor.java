package fotok.formathandler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import hu.qgears.commons.Pair;
import hu.qgears.commons.UtilString;

public class ExiftoolProcessor implements Consumer<String>
{
	private SimpleDateFormat exifDateFormat1=new SimpleDateFormat("yyyy:MM:dd HH:mm:ssX");
	private SimpleDateFormat exifDateFormat2=new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
	private SimpleDateFormat[] exifPossibleDateFormats=new SimpleDateFormat[] {exifDateFormat1, exifDateFormat2};
	public int width=1;
	public int height=1;
	public Date date;
	public String mimeType;
	@Override
	public void accept(String l) {
		String t=l.trim();
		Pair<String, String> keyValue=parseLine(l);
		if(keyValue!=null)
		{
			switch (keyValue.getA()) {
			case "Image Width":
				width=Integer.parseInt(keyValue.getB());
				break;
			case "Image Height":
				height=Integer.parseInt(keyValue.getB());
				break;
			case "MIME Type":
				mimeType=keyValue.getB();
				break;
			case "Date/Time Original":
			{
				List<String> pieces=UtilString.split(keyValue.getB(), " ");
				String d=pieces.get(0);
				String time=pieces.get(1);
				for(int i=0;i<exifPossibleDateFormats.length;++i)
				{
					SimpleDateFormat df=exifPossibleDateFormats[i];
					try {
						date=df.parse(d+" "+time);
					} catch (ParseException e) {
						if(i==exifPossibleDateFormats.length-1)
						{
							throw new RuntimeException("Can't parse timestamp output from exiftool", e);
						}
					}
				}
				if(pieces.size()>2 && "DST".equals(pieces.get(2)))
				{
					// Add an hour for DST
					date.setTime(date.getTime()+1000*60*60);
				}
				break;
			}
			default:
				break;
			}
		}
	}
	private Pair<String, String> parseLine(String line) {
		int idx=line.indexOf(":");
		if(idx>0)
		{
			String key=line.substring(0,idx).strip();
			if(line.length()>=idx+2)
			{
				String value=line.substring(idx+2);
				return new Pair<String, String>(key, value);
			}
		}
		return null;
	}
	@Override
	public String toString() {
		return "Mime: "+mimeType+" Date: "+date;
	}
}
