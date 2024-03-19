package org.openmrs.module.kenyaemrIL.dmi;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.codehaus.jackson.JsonNode;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.metadata.ILMetadata;
import org.openmrs.ui.framework.SimpleObject;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class dmiUtils {
	
	public static String UNIQUE_PATIENT_NUMBER = "05ee9cf4-7242-4a17-b4d4-00f707265c8a";
	
	//OAuth variables
	private static final Pattern pat = Pattern.compile(".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*");

	private String strClientId = ""; // clientId

	private String strClientSecret = ""; // client secret

	private String strScope = ""; // scope

	private String strTokenUrl = ""; // Token URL

	// Trust all certs
	static {
		TrustManager[] trustAllCerts = new TrustManager[]{
				new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}

					@Override
					public void checkClientTrusted(X509Certificate[] arg0, String arg1)
							throws CertificateException {
					}

					@Override
					public void checkServerTrusted(X509Certificate[] arg0, String arg1)
							throws CertificateException {
					}
				}
		};

		SSLContext sc = null;
		try {
			sc = SSLContext.getInstance("SSL");
		} catch (NoSuchAlgorithmException e) {
			System.out.println(e.getMessage());
		}
		try {
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
		} catch (KeyManagementException e) {
			System.out.println(e.getMessage());
		}
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// Optional
		// Create all-trusting host name verifier
		HostnameVerifier validHosts = new HostnameVerifier() {
			@Override
			public boolean verify(String arg0, SSLSession arg1) {
				return true;
			}
		};
		// All hosts will be valid
		HttpsURLConnection.setDefaultHostnameVerifier(validHosts);

	}
	/**
	 * Format gender
	 * @param input
	 * @return
	 */
	public static String formatGender(String input) {
		String in = input.trim().toLowerCase();
		if(in.equalsIgnoreCase("m"))
			return("Male");
		if(in.equalsIgnoreCase("f"))
			return("Female");
		return("Male");
	}


	
	public static SimpleDateFormat getSimpleDateFormat(String pattern) {
		return new SimpleDateFormat(pattern);
	}
	
	/**
	 * Creates a node factory
	 * 
	 * @return
	 */
	public static JsonNodeFactory getJsonNodeFactory() {
		final JsonNodeFactory factory = JsonNodeFactory.instance;
		return factory;
	}
	
	/**
	 * Extracts the request body and return it as string
	 * 
	 * @param reader
	 * @return
	 */
	public static String fetchRequestBody(BufferedReader reader) {
		String requestBodyJsonStr = "";
		try {
			
			BufferedReader br = new BufferedReader(reader);
			String output = "";
			while ((output = reader.readLine()) != null) {
				requestBodyJsonStr += output;
			}
		}
		catch (IOException e) {
			
			System.out.println("IOException: " + e.getMessage());
			
		}
		return requestBodyJsonStr;
	}
	

	/**
	 * Initialize the OAuth variables
	 *
	 * @return true on success or false on failure
	 */
	public boolean initAuthVars() {

		GlobalProperty globalTokenUrl = Context.getAdministrationService().getGlobalPropertyObject(ILMetadata.GP_DMI_SERVER_TOKEN_URL);
		strTokenUrl = globalTokenUrl.getPropertyValue();

		GlobalProperty globalClientSecret = Context.getAdministrationService().getGlobalPropertyObject(ILMetadata.GP_DMI_SERVER_CLIENT_SECRET);
		strClientSecret = globalClientSecret.getPropertyValue();

		GlobalProperty globalClientId = Context.getAdministrationService().getGlobalPropertyObject(ILMetadata.GP_DMI_SERVER_CLIENT_ID);
		strClientId = globalClientId.getPropertyValue();

		if (strTokenUrl == null || strClientSecret == null || strClientId == null) {
			System.err.println("Get oauth data: Please set OAuth credentials");
			return (false);
		}
		return (true);
	}

	/**
	 * Get the Token
	 *
	 * @return the token as a string and null on failure
	 */
	private String getClientCredentials() throws IOException, NoSuchAlgorithmException, KeyManagementException{

		System.out.println("Generating credentials ==>");

		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} };


		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());


		GlobalProperty globalTokenUrl = Context.getAdministrationService().getGlobalPropertyObject(ILMetadata.GP_DMI_SERVER_TOKEN_URL);
		strTokenUrl = globalTokenUrl.getPropertyValue();

		GlobalProperty globalClientSecret = Context.getAdministrationService().getGlobalPropertyObject(ILMetadata.GP_DMI_SERVER_CLIENT_SECRET);
		strClientSecret = globalClientSecret.getPropertyValue();
		//System.out.println("strClientSecret==>"+strClientSecret);
		GlobalProperty globalClientId = Context.getAdministrationService().getGlobalPropertyObject(ILMetadata.GP_DMI_SERVER_CLIENT_ID);
		strClientId = globalClientId.getPropertyValue();
		//System.out.println("strClientId==>"+strClientId);
		String auth = strClientId + ":" + strClientSecret;
		//System.out.println("Auth==>"+auth);
		BufferedReader reader = null;
		HttpsURLConnection connection = null;
		String returnValue = "";
		try {
			//System.out.println("Inside try catch");
			StringBuilder parameters = new StringBuilder();
			parameters.append("grant_type=" + URLEncoder.encode("client_credentials", "UTF-8"));
			parameters.append("&");
			parameters.append("client_id=" + URLEncoder.encode(strClientId, "UTF-8"));
			parameters.append("&");
			parameters.append("client_secret=" + URLEncoder.encode(strClientSecret, "UTF-8"));
			URL url = new URL(strTokenUrl);
			//System.out.println("String url==>"+url);
			connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("Accept", "application/json");
			connection.setConnectTimeout(10000); // set timeout to 10 seconds
			//System.out.println("String connection==>"+connection);
			//System.out.println("String parameters==>"+parameters);
			PrintStream os = new PrintStream(connection.getOutputStream());
			//System.out.println("Parameters==>"+parameters);
			os.print(parameters);
			os.close();
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line = null;
			StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
			while ((line = reader.readLine()) != null) {
				out.append(line);
			}
			String response = out.toString();
			//System.out.println("Return generated string token ==>"+response);
			Matcher matcher = pat.matcher(response);
			if (matcher.matches() && matcher.groupCount() > 0) {
				returnValue = matcher.group(1);
				System.out.println("Return token successfully ==>");
			} else {
				System.out.println("Return token value missing==>");
				System.err.println("OAUTH Error : Token pattern mismatch");
			}

		}
		catch (Exception e) {
			System.err.println("OAUTH - Error : " + e.getMessage());
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (IOException e) {}
			}
			connection.disconnect();
		}
		return returnValue;
	}

	/**
	 * Checks if the current token is valid and not expired
	 *
	 * @return true if valid and false if invalid
	 */
	private boolean isValidToken() {
		String currentToken = Context.getAdministrationService().getGlobalProperty(ILMetadata.GP_DMI_SERVER_TOKEN);
		org.codehaus.jackson.map.ObjectMapper mapper = new org.codehaus.jackson.map.ObjectMapper();
		try {
			org.codehaus.jackson.node.ObjectNode jsonNode = (org.codehaus.jackson.node.ObjectNode) mapper.readTree(currentToken);
			if (jsonNode != null) {
				long expiresSeconds = jsonNode.get("expires_in").getLongValue();
				String token = jsonNode.get("access_token").getTextValue();
				if(token != null && token.length() > 0)
				{
					String[] chunks = token.split("\\.");
					Base64.Decoder decoder = Base64.getUrlDecoder();

					String header = new String(decoder.decode(chunks[0]));
					String payload = new String(decoder.decode(chunks[1]));

					org.codehaus.jackson.node.ObjectNode payloadNode = (org.codehaus.jackson.node.ObjectNode) mapper.readTree(payload);
					long expiryTime = payloadNode.get("exp").getLongValue();

					long currentTime = System.currentTimeMillis()/1000;

					// check if expired
					if (currentTime < expiryTime) {
						return(true);
					} else {
						return(false);
					}
				}
				return(false);
			} else {
				return(false);
			}
		} catch(Exception e) {
			return(false);
		}
	}

	/**
	 * Gets the OAUTH2 token
	 *
	 * @return String the token or empty on failure
	 */
	public String getToken() throws IOException, NoSuchAlgorithmException, KeyManagementException {
		//check if current token is valid
		System.out.println("Generating token ==>");
		if(isValidToken()) {
			return(Context.getAdministrationService().getGlobalProperty(ILMetadata.GP_DMI_SERVER_TOKEN));

		} else {
			// Init the auth vars
			boolean varsOk = initAuthVars();
			System.out.println("If vars are okay ==>"+varsOk);
			if (varsOk) {
				//Get the OAuth Token
				String credentials = getClientCredentials();
				//System.out.println("Credentials ==>"+credentials);
				//Save on global and return token
				if (credentials != null) {
					Context.getAdministrationService().setGlobalProperty(ILMetadata.GP_DMI_SERVER_TOKEN, credentials);
					return(credentials);
				}
			}
		}
		return(null);
	}

	public static SimpleObject sendPOST(String params) throws IOException, NoSuchAlgorithmException, KeyManagementException {
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} };


		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		String stringResponse= "";
		System.out.println("Running send function");
		GlobalProperty globalPostUrl = Context.getAdministrationService().getGlobalPropertyObject(ILMetadata.GP_DMI_SERVER_POST_END_POINT);
		String strPostUrl = globalPostUrl.getPropertyValue();
		//System.out.println("Retrieving post url ==>"+strPostUrl);
		URL url = new URL(strPostUrl);

		HttpsURLConnection con =(HttpsURLConnection) url.openConnection();
		con.setRequestMethod("POST");

		dmiUtils dmiUtils = new dmiUtils();
		String authToken = dmiUtils.getToken();
		//System.out.println("Retrieving token ==>"+authToken);
		//System.out.println("Params to send ==>"+params);

		con.setRequestProperty("Authorization", "Bearer " + authToken);
		con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		con.setRequestProperty("Accept", "application/json");
		con.setConnectTimeout(10000); // set timeout to 10 seconds

		con.setDoOutput(true);
		OutputStream os = con.getOutputStream();
		os.write(params.getBytes());
		os.flush();
		os.close();

		int responseCode = con.getResponseCode();
		System.out.println("Response code ==>"+responseCode);
//		System.out.println("Response message ==>"+con.getResponseMessage());
//		System.out.println("Response error message ==>"+con.getErrorStream());
//		System.out.println("Response con message ==>"+con.toString());
		SimpleObject responseObj = null;

		if (responseCode == HttpURLConnection.HTTP_OK) { //success
			BufferedReader in = null;
			in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			stringResponse = response.toString();
			System.out.println("DMI Push Response --"+stringResponse);
			responseObj.put("message", responseCode);

			return(responseObj);

		} else {
			if (con != null && con.getErrorStream() != null) {
				BufferedReader in = null;
				// BufferedReader in = new BufferedReader(new InputStreamReader(
				// 		con.getErrorStream()));
				in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();

				stringResponse = response.toString();
			}

			responseObj = new SimpleObject();
			responseObj.put("status", responseCode);
			responseObj.put("message", stringResponse);
			return(responseObj);
		}
		//return responseObj;
	}
	/**
	 * Processes DMI Server response
	 *
	 * @param stringResponse success status
	 * @return SimpleObject the processed data
	 */
	public static SimpleObject processDmiResponse(String stringResponse) throws IOException {
		org.codehaus.jackson.map.ObjectMapper mapper = new org.codehaus.jackson.map.ObjectMapper();
		JsonNode jsonNode = null;
		String message = "";
		SimpleObject responseObj = new SimpleObject();

		try {
			jsonNode = mapper.readTree(stringResponse);
			if (jsonNode != null) {
				message = jsonNode.get("message").getTextValue();
				responseObj.put("clientNumber", message);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return responseObj;
	}



}
