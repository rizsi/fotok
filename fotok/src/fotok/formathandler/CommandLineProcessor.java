package fotok.formathandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

public class CommandLineProcessor {
	protected List<String> commandParts;
	protected void addCommandParts(String... parts) {
		for(String s: parts)
		{
			commandParts.add(s);
		}
	}
	protected void processLines(InputStream is, Consumer<String> lineConsumer) throws IOException
	{
		BufferedReader br=new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
		String line;
		while((line=br.readLine())!=null)
		{
			lineConsumer.accept(line);
		}
	}
}
