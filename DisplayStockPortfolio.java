import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DisplayStockPortfolio {
	private List<StockInfo> stockInfoList;
    private HashMap<String,StockInfo> stockInfoMap;
	private String Filename = "./stocklist.txt";
	private boolean doWide = true;
	private boolean hasMultipleQuantity = false;
    private boolean verbose = false;

	public static void main(String[] args) {

		DisplayStockPortfolio sp = new DisplayStockPortfolio();

        for (String s: args) {
            if ( s.startsWith("-") ) {
                switch (s) {
                    case "-v":
                        sp.verbose = true;
                        break;
                    case "-w":
                        sp.doWide = !sp.doWide;
                        break;
                }
            } else {
                sp.Filename = s;
            }
        }

		File file = new File(sp.Filename);
		if ( !file.exists() ) {
			System.out.println("File="+sp.Filename+" does not exists");
			System.exit(0);
		} else if ( sp.verbose ) {
			System.out.println("File="+sp.Filename);
		}

		sp.ReadAndParseFile(file);
		sp.RequestYahoo();
	}

    public DisplayStockPortfolio() {
        stockInfoList = new ArrayList<>();
        stockInfoMap = new HashMap<>();
    }

	public void ReadAndParseFile(File file) {
		String line;
		try {
			BufferedReader binfile = new BufferedReader(new FileReader(file));
			while ( (line=binfile.readLine()) != null ) {
				if ( line.startsWith("#") ) {
					continue;
				}
				Scanner scanner = new Scanner(line);
				scanner.useDelimiter("	");
				if ( scanner.hasNext() ) {
					String ticker = deriveTicker(scanner.next());
					String origValue = scanner.next();
					String quantity1 = scanner.next();
					String quantity2 = scanner.next();
					StockInfo stockInfo = new StockInfo(ticker,
											Double.parseDouble(quantity1),
											Double.parseDouble(quantity2),
											Double.parseDouble(origValue));
                    if ( verbose && ticker != null ) {
                        System.out.println("Ticker="+ticker+" orig="+origValue+" q1="+quantity1+" q2="+quantity2);
                    }
                    // check if at least one has multiple shares
                    if ( Double.parseDouble(quantity1) > 0.0 && Double.parseDouble(quantity2) > 0.0 ) {
                        hasMultipleQuantity = true;
                    }
					//String st = stockInfo.toString();
					//System.out.println(st);

					// add to the list
                    if ( ticker != null ) {
                        stockInfoList.add(stockInfo);
                        stockInfoMap.put(ticker,stockInfo);
                    }
				}
			}
			binfile.close();
		} catch ( Exception ex ) {
			System.out.println("Could not open file for reading ["+file.getName()+"]");
			ex.printStackTrace();
			System.exit(0);
		}
	}

	// hack out the yahoo ticker from the name
	public String deriveTicker(String ticker) {
		int tickerLen = ticker.length();

		if ( ticker.charAt(0) == '.' ) {
			ticker = '^' + ticker.substring(1,tickerLen);
		}

		if ( ticker.charAt(tickerLen-1) == '=' ) {
            // ignore for now
			// ticker =~ s/^(\w+)=$/${1}USD=/ if $record =~ /EUR|GBP/;
			// ticker .= "X";
			return null;
		} else if ( ticker.equals("^SPX") ) {
			ticker = "^GSPC";
		} else if ( ticker.equals("^DJI") ) {
			// use the ETF which is around 100x less than dow
			ticker = "DIA";
		} else if ( ticker.substring(tickerLen-2,tickerLen).equals("c1") ) {
            // ignore futures prices for now
			return null;
		}

		int pos = ticker.indexOf('.');
		if ( pos > 0 ) {
			ticker = ticker.substring(0,pos);
		}
		return ticker;
	}

	public String cleanField(String field) {
		field = field.replace("\"","");
		field = field.replace("^0.00","0");
		if ( field.equals("N/A") ) {
			field = "0";
		}
		return field;
	}

    private boolean allEmpty(ArrayList<String> fields) {
        for ( int pos=1; pos < fields.size(); pos++ ) {
            String field = fields.get(pos);
            if ( !field.equals("N/A") ) {
                return false;
            }
        }
        return true;
    }

    // This would be refactored to a separate class
    public ArrayList<String> parseSimpleCsv(String line) {
        final char delimiter = ',';
        final char quote   = '"';
        boolean inQuote = false;
        ArrayList<String> fields = new ArrayList<String>();
        StringBuilder outstring = new StringBuilder();
        for ( int pos=0; pos < line.length(); pos++ ) {
            char c = line.charAt(pos);
            if ( inQuote ) {
                if ( c == quote ) {
                    inQuote = false;
                } else {
                    outstring.append(c);
                }
            } else if ( c == delimiter ) {
                fields.add(new String(outstring));
                outstring = new StringBuilder();
            } else if ( c == quote ) {
                inQuote = true;
            } else {
                outstring.append(c);
            }
        }
        if ( outstring.length() > 0 ) {
            fields.add(new String(outstring));
        }
        return fields;
    }

    // This would be refactored to a sub class of data sources
	public void RequestYahoo() {
		// build the symbol list
		StringBuilder symlist = new StringBuilder();
		for ( StockInfo sInfo: stockInfoList ) {
			String ticker = sInfo.getTicker();
            if ( ticker != null && !ticker.equals("CASH") ) {
                symlist.append(ticker + ",");
            }
		} 
		if ( symlist.length() > 1 ) {
			symlist.deleteCharAt(symlist.length()-1);
		}

		final String format = "snl1c1vkjhgpw";
		final String reqURL = "http://download.finance.yahoo.com/d/quotes.csv?s="+symlist+"&f="+format+"&e=.csv";
        if ( verbose ) {
            System.out.println(reqURL);
        }
		URL url = null;
		try {
			url = new URL(reqURL);
		} catch ( MalformedURLException me ) {
			me.printStackTrace();
			System.exit(0);
		}

        SimpleDateFormat dformat = new SimpleDateFormat("E dd MMM yyyy HH:mm:ss z");
        String now = dformat.format(new Date());
        System.out.println("File: "+Filename+" Time: ["+now+"] Source: Yahoo Finance");

        StringBuilder header = new StringBuilder();
        header.append(" Name                Last  YrHigh   YrLow  ");
        if ( doWide ) {
            header.append("  High     Low  Change     Volume  ");
        } else {
            header.append("Change  ");
        }
        if ( hasMultipleQuantity ) {
            header.append("  Sub1    Sub2   Total DayPNL");
        } else {
            header.append(" Total DayPNL");
        }
        if ( doWide ) {
            header.append("    PNL");
        }
        System.out.println(header);

        double total1 = 0;
        double total2 = 0;
        double total = 0;
        double total_change = 0;
        double total_pnl = 0;

        double cash1 = 0;
        double cash2 = 0;

        StockInfo stockInfo = (StockInfo) stockInfoMap.get("CASH");
        if ( stockInfo != null ) {
            cash1 = stockInfo.getQuantity1();
            total1 = cash1;
            cash2 = stockInfo.getQuantity2();
            total2 = cash2;
            total = cash1 + cash2;
        }

		try {
			InputStream inputStream = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
			String inputLine;

			while ( (inputLine = br.readLine()) != null ) {
				//System.out.println(inputLine);

                ArrayList<String> fields = parseSimpleCsv(inputLine);
                if ( allEmpty(fields) ) {
                    continue;
                }

				String ticker = cleanField(fields.get(0));
				String name = cleanField(fields.get(1));
				double last = Double.parseDouble(cleanField(fields.get(2)));
				double change = Double.parseDouble(cleanField(fields.get(3)));
				double volume = Double.parseDouble(cleanField(fields.get(4)));
				double yrHigh = Double.parseDouble(cleanField(fields.get(5)));
				double yrLow = Double.parseDouble(cleanField(fields.get(6)));
				double high = Double.parseDouble(cleanField(fields.get(7)));
				double low = Double.parseDouble(cleanField(fields.get(8)));
				double prev = Double.parseDouble(cleanField(fields.get(9)));

				String lformat;
				if ( last >= 10000 ) {
					lformat = " %7.1f";
				} else if ( last >= 1000 ) {
					lformat = " %7.2f";
				} else {
					lformat = " %7.3f";
				}

				String yhformat;
				if ( yrHigh >= 10000 ) {
					yhformat = " %7.1f";
				} else if ( yrHigh >= 1000 ) {
					yhformat = " %7.2f";
				} else {
					yhformat =" %7.3f";
				}

				String ylformat;
				if ( yrLow >= 10000 ) {
					ylformat = " %7.1f";
				} else if ( yrLow >= 1000 ) {
					ylformat = " %7.2f";
				} else {
					ylformat = " %7.3f";
				}

				String hformat;
				if ( high >= 10000 ) {
					hformat = " %7.1f";
				} else if ( high >= 1000 ) {
					hformat = " %7.2f";
				} else {
					hformat = " %7.3f";
				}

				String lowformat;
				if ( low >= 10000 ) {
					lowformat = " %7.1f";
				} else if ( low >= 1000 ) {
					lowformat = " %7.2f";
				} else {
					lowformat = " %7.3f";
				}

				String cformat;
				if ( change >= 10000 || change <= -1000 ) {
					cformat = " %7.1f";
				} else if ( change >= 1000 || change <= -100 ) {
					cformat = " %7.2f";
				} else {
					cformat = " %7.3f";
				}

                if ( last == 0.0 && prev != 0.0 ) {
                    last = prev;
                }

                if ( name.length() > 16 ) {
                    name = name.substring(0,16);
                }

                if ( name.equals("GCc1") ) {
                    name = "GOLD! ";
                }
                if ( name.equals("NY RBOB") ) {
                    name = "UNLEAD GAS";
                }

                stockInfo = (StockInfo) stockInfoMap.get(ticker);
                if ( stockInfo == null ) {
                    continue;
                }
                double quantity1 = stockInfo.getQuantity1();
                double quantity2 = stockInfo.getQuantity2();
                double purch = stockInfo.getOrigValue();

                double sub1 = quantity1 * last;
                double sub2 = quantity2 * last;
                double subtotal = sub1 + sub2;
                double subtotal_change = change * (quantity1 + quantity2);
                double subtotal_pnl = subtotal - purch;

                total_change += 0 + subtotal_change;

                total1 += sub1;
                total2 += sub2;
                total += subtotal;
                total_pnl += subtotal_pnl;

                if ( doWide ) {
                    if ( hasMultipleQuantity ) {
                        System.out.printf(
                            " %-16s"+lformat+yhformat+ylformat+hformat+lowformat+cformat+
                            " %10d %7.0f %7.0f %7.0f %6.0f %6.0f\n",
                            name, last, yrHigh, yrLow, high, low, change, (int)volume,
                            sub1, sub2, subtotal, subtotal_change, subtotal_pnl);
                    } else {
                        System.out.printf(
                            " %-16s"+lformat+yhformat+ylformat+hformat+lowformat+cformat+
                            " %10d %7.0f %6.0f %6.0f\n",
                            name, last, yrHigh, yrLow, high, low, change, (int)volume,
                            subtotal, subtotal_change, subtotal_pnl);
                    }
                } else {
                    if ( hasMultipleQuantity ) {
                        System.out.printf(
                            " %-16s"+lformat+yhformat+ylformat+cformat+
                            " %7.0f %7.0f %7.0f %6.0f\n",
                            name, last, yrHigh, yrLow, change,
                            sub1, sub2, subtotal, subtotal_pnl);
                    } else {
                        System.out.printf(
                            " %-16s"+lformat+yhformat+ylformat+cformat+
                            " %7.0f %6.0f\n",
                            name, last, yrHigh, yrLow, change,
                            subtotal, subtotal_pnl);
                    }
                }
			}
			inputStream.close();
		} catch ( Exception ex ) {
			ex.printStackTrace();
			System.exit(0);
		}

        if ( doWide ) {
            if ( hasMultipleQuantity ) {
                if ( cash1 > 0 || cash2 > 0 ) {
                    System.out.printf(" CASH %78d %7d %7d\n", (int)cash1, (int)cash2, (int)(cash1 + cash2));
                }
                System.out.printf(" TOTALS%77.0f %7.0f %7.0f", total1, total2, total);
                System.out.printf(" %6.0f", total_change);
                System.out.printf(" %6.0f\n", 0 + total_pnl);
            } else {
                if ( cash1 > 0 || cash2 > 0 ) {
                    System.out.printf(" CASH %76d       \n", (int)cash1);
                }
                System.out.printf(" TOTALS%75.0f",total);
                System.out.printf(" %6.0f", total_change);
                System.out.printf(" %6.0f\n", 0 + total_pnl);
            }
        } else {
            if ( hasMultipleQuantity ) {
                if ( cash1 > 0 || cash2 > 0 ) {
                    System.out.printf(" CASH %51d %7d %7d       \n", (int)cash1, (int)cash2, (int)(cash1 + cash2));
                }
                System.out.printf(" TOTALS%50.0f %7.0f %7.0f %6.0f\n",
                total1, total2, total, total_pnl);
            } else {
                if ( cash1 > 0 || cash2 > 0 ) {
                    System.out.printf(" CASH %51d       \n", (int)cash1);
                }
                System.out.printf(" TOTALS%50.0f %6.0f\n", total, total_pnl);
            }
        }
	}
}

/*
# The format is decoded: (check- http://www.gummy-stuff.org/Yahoo-data.htm)
# s = symbol  n: Company name  v = volume 
# l: last value with date/time  l1: (letter l and the number 1) just the last value
# o = open    p: previous close # p2: change percentage
# c: the change amount. Can either be used as c or c1.  c6, t5 = change and time but with ECN realtime data
    # c1 = change # t1 = time of last trade # d1 = date of last trade
# g = day low  j: 52-week low.
# h = day high k: 52-week high.
# w: 52-week range # e: EPS (Earning per share) # r: P/E (Prince/Earning) ratio # y: yield
# d1: current date
# j1: the market cap. This is string like 42.405B for $42 billion.
*/
