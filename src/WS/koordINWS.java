package WS;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

public class koordINWS {
	/** Called when the activity is first created. */
	private static String SOAP_ACTION = "http://WS/addKoord";
	private static String NAMESPACE = "http://WS/";
	private static String METHOD_NAME = "addKoord";

	// emulator!!! private static String URL =
	// "http://10.0.2.2:8080/repServer/koordINWS?wsdl";
	private static String URL = "http://193.226.17.132:8080/repServer/koordINWS?wsdl";

	public boolean addKoord(Double longi, Double lat, Double alt, Double accur,
			String data) {
		// Initialize soap request + add parameters
		SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);

		// Use this to add parameters
		request.addProperty("arg0", longi);
		request.addProperty("arg1", lat);
		request.addProperty("arg2", alt);
		request.addProperty("arg3", accur);
		request.addProperty("arg4", data);

		// Declare the version of the SOAP request
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
				SoapEnvelope.VER11);

		envelope.setOutputSoapObject(request);

		try {
			HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);

			// this is the actual part that will call the webservice
			androidHttpTransport.call(SOAP_ACTION, envelope);
			// return true;
			// Get the SoapResult from the envelope body.
			SoapObject result = (SoapObject) envelope.bodyIn;

			if (result != null) {
				// Get the first property and change the label text
				if (result.getProperty(0).toString().equals("true")) {
					return true;
				} else if (result.getProperty(0).toString().equals("false")) {
					return false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}