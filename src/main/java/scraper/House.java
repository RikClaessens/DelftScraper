package scraper;

public class House {
	private String address, type, numberofrooms, surface, state, price, availability;

	public House(String address, String type, String numberofrooms, String surface,
                 String state, String price, String availability) {
		this.address = address;
		this.type = type;
		this.numberofrooms = numberofrooms;
		this.surface = surface;
		this.state = state;
		this.price = price;
		this.availability = availability;
	}

	public String getAddress() {
		return address;
	}

	public String getType() {
		return type;
	}

	public String getNumberofrooms() {
		return numberofrooms;
	}

	public String getSurface() {
		return surface;
	}

	public String getState() {
		return state;
	}

	public String getPrice() {
		return price;
	}

	public String getAvailability() {
		return availability;
	}

    @Override
	public String toString() {
		return "House [address=" + address + ", type=" + type
				+ ", numberofrooms=" + numberofrooms + ", surface=" + surface
				+ ", state=" + state + ", price=" + price + ", availability="
				+ availability + "]";
	}

}
