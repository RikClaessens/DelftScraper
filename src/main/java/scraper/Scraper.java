package scraper;

import log.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Formatter;

public class Scraper {

	private File logFileBehome = new File("scraped_behome.txt");
	private File logFileWRD = new File("scraped_wrd.txt");
	private File logFilePararius = new File("scraped_pararius.txt");
	private File logFileOudeDelft = new File("scraped_oude_delft.txt");
	private ArrayList<Integer> foundIDs;
	private Formatter formatter = new Formatter();
	ArrayList<House> houses;

	public void scrape() throws IOException {
		scrapeBeHome();
		scrapeWoonRuimteDelft();
		scrapePararius();
		scrapeOudeDelft();
	}


	public void scrapeBeHome() throws IOException {
		System.out.println("******************\nBeHome");
		houses = new ArrayList<House>();
		foundIDs = parseString(getFoundIDs(logFileBehome));
		Document doc = Jsoup.connect("http://www.behome.nl/aanbod/zoeken.php?lang=").get();
		// let's find the iframe
		Elements elements = doc.select("iframe");
		Element iframe = elements.first();

		// now load the iframe

		String src = iframe.absUrl("src");
		src = src.replace("plaats=", "plaats=delft").replace("prijs=", "prijs=<1001");
		URL iframeUrl = new URL(src);
		doc = Jsoup.parse(iframeUrl, 15000);

		elements = doc.select("tr");

		for (Element element : elements) {
			String click = element.attr("onclick");
			if (click == "") continue;
			Elements houseproperties = element.select("td");

			click = click.replace("javascript:window.open('aanbod.php?lang=&huisid=", "").replace("','_self')", "");
			if (tryParseInt(click)) {
				int id = Integer.parseInt(click);
				if (!idFound(id)) {
					String[] properties = new String[9];
					int propid = 0;
					for (Element property : houseproperties) {
						properties[propid] = property.html();
						propid++;
					}
					addBehomeHouse(properties);

					Logger.getInstance().log(logFileBehome.getAbsolutePath(), Integer.toString(id));
				}
			}

		}
		printHouses();
	}

	public void printHouses() {
		if (!houses.isEmpty()) {
			String format = "%30s %15s %10s %15s %15s";
			formatter = new Formatter();
			System.out.println(formatter.format(format, "Straatnaam", "Type", "M2", "Huurprijs", "Status"));
			for (int i = 0; i < 125; i++) {
				System.out.print("_");
			}
			System.out.println();
			for (House house : houses) {
				formatter = new Formatter();
		        System.out.println(formatter.format(format, house.getAddress(), house.getType(), house.getSurface(), house.getPrice(), house.getAvailability()));
			}
		} else {
			System.out.println("No new houses found :(");
		}
		System.out.println();
	}

	public void scrapeWoonRuimteDelft() throws IOException {
		System.out.println("******************\nWoonRuimteDelft");
		houses = new ArrayList<House>();
		foundIDs = parseString(getFoundIDs(logFileWRD));

		int cur_page = 0;
		boolean more_pages = true;

		while(more_pages) {
			System.out.print(".");
			Document doc = Jsoup.connect("http://www.woonruimtedelft.nl/index.php?cur_page=" + Integer.toString(cur_page) + "&action=searchresults&pclass%5B%5D=2&sortby=listingsdb_last_modified&sorttype=DESC").get();

			Element resultBlock = doc.select("div.hresultblok").first();
			Elements results = resultBlock.select("a");

			for (Element e : results) {
				String link = e.attr("href");
				link = link.substring(0, link.indexOf(".html"));
				char c = link.charAt(link.length() - 1);
				String id_string = "";
				while (c != '-') {
					id_string = link.charAt(link.length() - 1) + id_string;
					link = link.substring(0, link.length() - 1);
					c = link.charAt(link.length() - 1);
				}

				if (tryParseInt(id_string)) {
					int id = Integer.parseInt(id_string);
					if (!idFound(id)) {
						foundIDs.add(id);
						Logger.getInstance().log(logFileWRD.getAbsolutePath(), id_string);
						Element grandparent = e.parent().parent().parent();
						addWRDHouse(grandparent);
					}
				}
			}

			if (results.size() == 0) {
				more_pages = false;
				break;
			}
			cur_page++;
		}
		System.out.println();
		printHouses();
	}

	public void scrapePararius() throws IOException {
		System.out.println("******************\nPararius");
		houses = new ArrayList<House>();
		foundIDs = parseString(getFoundIDs(logFilePararius));

		int cur_page = 1;
		boolean more_pages = true;

		while(more_pages) {
			System.out.print(".");
			more_pages = false;
			Document doc = Jsoup.connect("http://www.pararius.nl/huurwoningen/Delft/400-1000/page-" + Integer.toString(cur_page) + "/").timeout(15000).get();

			Elements houses_elements = doc.select("#content").first().select("ul.products-list");

			for (Element e : houses_elements) {
				if (addParariusHouse(e)) {
					more_pages = true;
				}
			}
			cur_page++;
		}
		System.out.println();
		printHouses();
	}

	public void scrapeOudeDelft() throws IOException {
		System.out.println("******************\nOude Delft");
		foundIDs = parseString(getFoundIDs(logFileOudeDelft));
		houses = new ArrayList<House>();
		int cur_page = 0;
		boolean more_pages = true;
		while (more_pages) {
			System.out.print(".");
			Document doc = Jsoup.connect("http://www.oudedelft.com/en/aanbod" + (cur_page > 0 ? "-" + Integer.toString(cur_page * 10) : "") + ".htm").timeout(15000).get();

			Elements elements = doc.select("div.details");
			for (Element element : elements) {
				addOudeDelftHouse(element.select("div.text").get(0));
			}
			more_pages = elements.size() > 0;
			cur_page++;
		}
		System.out.println();
		printHouses();
	}

	public void addOudeDelftHouse(Element element) {
		String address = element.select("h3").select("a").html().toString();
		String type = "";
		String numberofrooms = "";
		String surface = element.select("p").get(0).toString();
		surface = surface.substring(surface.indexOf("-") + 2, surface.indexOf("<sup>"));
		String state = "";
		String price = element.select("p").get(2).toString();
		price = price.substring(price.indexOf("Price:") + 9);
		price = price.substring(0, price.indexOf("</span"));
		String availability = ""	;
        String url = element.select("h3").select("a").attr("href");
		House house = new House(address, type, numberofrooms, surface, state, price, availability);
		String id_string = url.replace("http://www.oudedelft.com/en/aanbod/", "");
		id_string = id_string.substring(0, id_string.indexOf("/"));
		if (tryParseInt(id_string)) {
			int id = Integer.parseInt(id_string);
			if (!idFound(id)) {
				houses.add(house);
				foundIDs.add(id);
				Logger.getInstance().log(logFileOudeDelft.getAbsolutePath(), Integer.toString(id));
			}
		}
	}

	public void addBehomeHouse(String[] properties) {
		String address, type, numberofrooms, surface, state, price, availability;
		int c = 1;
		address = properties.length > c ? properties[c] : "";
		c++;
		type = properties.length > c ? properties[c] : "";
		c++;
		numberofrooms = properties.length > c ? properties[c] : "";
		c++;
		surface = properties.length > c ? properties[c] : "";
		c++;
		state = properties.length > c ? properties[c] : "";
		c++;
		price = properties.length > c ? properties[c].replace("&#x20ac; ", "") : "";
		c++;
		availability = properties.length > c ? properties[c] : "";
		c++;
		houses.add(new House(address, type, numberofrooms, surface, state, price, availability));
	}

	public void addWRDHouse(Element grandparent) {
		String house = grandparent.select("div.hright").select("a").html().split("\n")[0];
		String[] props = house.split(":");
		String address = "";
		String type = "";
		String numberofrooms = "";
		String surface = grandparent.toString().substring(grandparent.toString().indexOf("<!- Oppervlakte -> ") + 19, grandparent.toString().length() - 1).split("\n")[0];
		String state = "";
		String price = grandparent.select("li.prijs").html().substring(1);
		String availability = grandparent.select("span.status").html();
		if (props.length > 1 ) {
			type = props[0].split(" ")[0];
			address = props[1];
		} else {
			address = props[0];
		}
		houses.add(new House(address, type, numberofrooms, surface, state, price, availability));
	}

	public boolean addParariusHouse(Element e) {
		String[] house_array = e.select("a").get(1).html().split(" - ")[0].split(" ");
		String address = "";
		for (int i = 1; i < house_array.length; i++) {
			address += house_array[i] + " ";
		}
		if (address.startsWith("class=")) {
			if (address.indexOf("<span") == -1)
				return false;
			address = address.substring(address.indexOf(">") + 1, address.indexOf("<span") - 1);
		}

		String type = e.select("a").get(1).html().split(" - ")[0].split(" ")[0];
		if (type.equalsIgnoreCase("<strong")) type = "";
		String numberofrooms = "";
		String surface = e.select("strong.price").html().substring(e.select("strong.price").html().indexOf("<span>") + 7);
		//  slaapkamer - 58 m<sup>2</sup> - Gestoffeerd</span>
		if (surface.charAt(0) == '<')  {
			surface = "";
		}
		else {
			surface = surface.substring(surface.indexOf("-") + 2, surface.indexOf("<"));
		}
		String state = "";
		String price = e.select("strong.price").html().replace("\n", "").replace(" <!--Prijs--></b>", "");
		if (price.contains("Vanaf <b style=\"font-size:13px; padding-left:5px;\">")) {
			price = price.replace("Vanaf <b style=\"font-size:13px; padding-left:5px;\">", "");
		} else {
			price = price.substring(4, (price.indexOf("<span>") != -1 ? price.indexOf("<span>") - 1 : price.length()) );
		}
		price = price.replace(" </b>", "");
		if (price.startsWith("<strong")) price = price.substring(22);
		price = trimToNumber(price);
		String availability = "";
		if (e.select("strong.title").select("a").size() == 0) {
			return false;
		}
		String id = trimNumberStart(e.select("strong.title").select("a").get(0).attr("href"));
		Logger.getInstance().log(logFilePararius.getAbsolutePath(), id);
		House house = new House(address, type, numberofrooms, surface, state, price, availability);
		if (!idFound(Integer.parseInt(id))) {
			houses.add(house);
		}
		return true;
	}

	public String[] getFoundIDs(File file) {
		ArrayList<String> ids = new ArrayList<String>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();
			while (line != null) {
				ids.add(line);
				line = reader.readLine();
			}
			reader.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Logger.getInstance().log(file.getAbsolutePath(), "");
		}

		String[] idArray = new String[ids.size()];
		for (int i = 0; i < ids.size(); i++) {
			idArray[i] = ids.get(i);
		}
		return idArray;
	}

	public ArrayList<Integer> parseString(String[] numbers) {
		ArrayList<Integer> parsedNumbers = new ArrayList<Integer>();
		for (int i = 0; i < numbers.length; i++) {
			if (tryParseInt(numbers[i]))
				parsedNumbers.add(Integer.parseInt(numbers[i]));
		}
		return parsedNumbers;
	}

	public boolean tryParseInt(String value) {
		try {
			Integer.parseInt(value);
			return true;
		} catch(NumberFormatException nfe) {
			return false;
		}
	}

	public boolean idFound(int id) {
		for (int foundID : foundIDs) {
			if (foundID == id)
				return true;
		}
		return false;
	}

	public String trimToNumber(String s) {
		int i = 0;
		String c = Character.toString(s.charAt(i));
		while (!tryParseInt(c)) {
			s = s.substring(1);
			c = Character.toString(s.charAt(i));
		}
		return s;

	}

	public String trimNumberStart(String s) {
		s = s.substring(0, s.length() - 1);
		String trim = "";
		int i = 0;
		String c = Character.toString(s.charAt(s.length() - 1));
		while (tryParseInt(c)) {
			s = s.substring(0, s.length() - 1);
			trim = c + trim;
			c = Character.toString(s.charAt(s.length() - 1));
		}
		return trim;
	}
}
