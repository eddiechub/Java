
public class StockInfo {
	String ticker;
	double quantity1;
	double quantity2;
	double origValue;

	public StockInfo(String ticker, double quantity1, double quantity2, double origValue) {
		this.ticker = ticker;
		this.quantity1 = quantity1;
		this.quantity2 = quantity2;
		this.origValue = origValue;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public String getTicker() {
		return ticker;
	}

	public void setQuantity1(double quantity1) {
		this.quantity1 = quantity1;
	}

	public double getQuantity1() {
		return quantity1;
	}

	public void setQuantity2(double quantity2) {
		this.quantity2 = quantity2;
	}

	public double getQuantity2() {
		return quantity2;
	}

	public void setOrigValue(double origValue) {
		this.origValue = origValue;
	}

	public double getOrigValue() {
		return origValue;
	}

	public String toString() {
		return "Ticker: "+ticker+" Quant1: "+quantity1+" Quant2: "+quantity2+" OrigAmount:"+origValue;
	}
}
