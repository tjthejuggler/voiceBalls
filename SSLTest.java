import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

public class SSLTest {
    public static void main(String[] args) {
        try {
            System.setProperty("javax.net.debug", "ssl:handshake");
            URL url = new URL("https://maven.picovoice.ai/ai/picovoice/cheetah-android/2.0.1/cheetah-android-2.0.1.pom");
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            System.out.println("SSL Connection successful! Response code: " + connection.getResponseCode());
        } catch (Exception e) {
            System.out.println("SSL Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}