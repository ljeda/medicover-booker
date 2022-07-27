package ljeda.medicover;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ljeda.medicover.log.Logger;

public class AppointmentBot 
{
	public static void main( String[] args)
	{
		HttpURLConnection.setFollowRedirects(true);
		URL url = null;
		try {
			url = new URL("https://mol.medicover.pl/Users/Account/Logger.logOn");
		} catch (MalformedURLException e) {
			handleCriticalError(e, "URL creation error", 1);
		}
		HttpURLConnection con = null;
		try {
			con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);
			con.disconnect();
			con = (HttpURLConnection) url.openConnection();
			String cookiesHeader = con.getHeaderField("Set-Cookie");
			List<HttpCookie> cookies = HttpCookie.parse(cookiesHeader);
			CookieManager cookieManager = new CookieManager();
			cookies.forEach(cookie -> cookieManager.getCookieStore().add(null, cookie));
			con.setRequestProperty("Cookie", StringUtils.join(cookieManager.getCookieStore().getCookies(), ";"));
		} catch (ProtocolException e) {
			handleCriticalError(e, "URL connection protocol error", 3);
		} catch (IOException e) {
			handleCriticalError(e, "URL connection opening error", 2);
		}
		try {
			// this in fact executes the request
			int status = con.getResponseCode();
			Logger.log("" + status);
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			StringBuffer content = new StringBuffer();
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				content.append(inputLine + "\n");
			}
			in.close();
			Logger.log(content.toString());
		} catch (IOException e) {
			handleCriticalError(e, "URL connection execution error", 4);
		}
	}

	private static void handleCriticalError(Exception e, String message, int exitCode) {
		Logger.log(message);
		Logger.log("Exception message: " + e.getMessage());
		e.printStackTrace();
		System.exit(exitCode);
	}
}
