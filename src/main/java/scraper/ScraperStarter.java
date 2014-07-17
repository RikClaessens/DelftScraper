package scraper;

import java.io.IOException;

public class ScraperStarter {

	public static void main(String[] args) {
		Scraper scraper = new Scraper();
		try {
			scraper.scrape();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
