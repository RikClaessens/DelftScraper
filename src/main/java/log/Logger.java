package log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {
	
	private static Logger instance = new Logger();
	
	private Logger() {
	}
	
	public static Logger getInstance() {
		return instance;
	}
	
	public void log(String file, String txt) {
		try {
			File logFile = new File(file);
			BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
			writer.write(txt + "\n");
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}