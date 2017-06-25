import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.awt.BorderLayout;

public class GUI {

	private JFrame frmFakeIdentityGenerator;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI window = new GUI();
					window.frmFakeIdentityGenerator.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public GUI() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmFakeIdentityGenerator = new JFrame();
		frmFakeIdentityGenerator.setTitle("Fake Identity Generator");
		frmFakeIdentityGenerator.setBounds(100, 100, 275, 85);
		frmFakeIdentityGenerator.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JButton genButton = new JButton("Generate!");
		genButton.addActionListener(new GenButtonListener());
		frmFakeIdentityGenerator.getContentPane().setLayout(new BorderLayout(0, 0));
		frmFakeIdentityGenerator.getContentPane().add(genButton);
	}

	private class GenButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			Document doc;
			try {
				doc = Jsoup
						.connect(
								"http://it.fakenamegenerator.com/advanced.php?t=country&n%5B%5D=it&c%5B%5D=it&gen=80&age-min=19&age-max=35")
						.get();
				String fullName = doc.select(".address h3").text();
				String firstName = URLEncoder.encode(fullName.split(" ")[0], "UTF-8");
				String lastName = URLEncoder.encode(fullName.split(" ")[1], "UTF-8");
				String fullCity = doc.select(".adr").text().split("-")[1];
				int i = fullCity.lastIndexOf(" ");
				String city = URLEncoder.encode(fullCity.substring(0, i).toUpperCase().toString(), "UTF-8");
				String prov = fullCity.substring(i + 1);
				String birthday = doc.select(".dl-horizontal").get(5).select("dd").text();
				String day = birthday.split(" ")[1].split(",")[0];
				if (day.length() == 1)
					day = "0" + day;
				i = birthday.lastIndexOf(" ");
				String year = birthday.substring(i + 1);
				SimpleDateFormat inputFormat = new SimpleDateFormat("MMMM", Locale.ENGLISH);
				Calendar cal = Calendar.getInstance();
				cal.setTime(inputFormat.parse(birthday.split(" ")[0]));
				SimpleDateFormat outputFormat = new SimpleDateFormat("MM");
				String month = outputFormat.format(cal.getTime());

				// Get CF form post request
				sendPostRequest(firstName, lastName, city, prov, day, year, month);
			} catch (IOException | ParseException e1) {
				e1.printStackTrace();
			}
		}

		private void sendPostRequest(String firstName, String lastName, String city, String prov, String day,
				String year, String month) throws IOException {
			String url = "http://www.codicefiscale.com";
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			con.setRequestMethod("POST");
			con.setRequestProperty("User-Agent",
					"Mozilla/5.0 (Windows NT 6.2; WOW64; rv:54.0) Gecko/20100101 Firefox/54.0");
			con.setRequestProperty("Accept-Language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3");

			String urlParameters = "cf-cognome=" + lastName + "&cf-nome=" + firstName + "&DDN_Giorno=" + day
					+ "&DDN_Mese=" + month + "&DDN_Anno=" + year + "&cf-sesso=Maschile" + "&cf-comune=" + city
					+ "&cf-provincia=" + prov;

			// Send post request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// Get the CF
			Document doc = Jsoup.parse(response.toString());
			Element el = doc.getElementById("cf-calc");

			if (el == null)
				JOptionPane.showMessageDialog(frmFakeIdentityGenerator, "OOPS, error! Retry to generate an identity.",
						"Error", JOptionPane.ERROR_MESSAGE);
			else {
				String cf = el.val();
				String output = URLDecoder.decode(firstName, "UTF-8") + " " + URLDecoder.decode(lastName, "UTF-8") + " "
						+ day + "-" + month + "-" + year + " " + cf + " " + URLDecoder.decode(city, "UTF-8") + " "
						+ prov;
				StringSelection stringSelection = new StringSelection(output);
				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
				cb.setContents(stringSelection, null);
				JOptionPane.showMessageDialog(frmFakeIdentityGenerator,
						"Success! The identity has been saved into the clipboard.", "Success",
						JOptionPane.INFORMATION_MESSAGE);
			}
		}

	}

}
