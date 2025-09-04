import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

public class SSLTestWithECDH {
    public static void main(String[] args) {
        try {
            // Enable ECDH temporarily by overriding the disabled algorithms
            System.setProperty("jdk.tls.disabledAlgorithms", "SSLv3, TLSv1, TLSv1.1, DTLSv1.0, RC4, DES, MD5withRSA, DH keySize < 1024, EC keySize < 224, 3DES_EDE_CBC, anon, NULL");
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