import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.SSLConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;

import static io.restassured.RestAssured.given;

public class Mesto1Test {

    static {
        // 1. Глобальное отключение SSL через HttpsURLConnection
        disableSSLCertificateVerification();

        // 2. Системные свойства для принудительного использования протоколов TLS
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");
        System.setProperty("jdk.tls.client.protocols", "TLSv1.2,TLSv1.3");
        System.setProperty("com.sun.net.ssl.checkRevocation", "false");
    }

    private static void disableSSLCertificateVerification() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                    }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String bearerToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJfaWQiOiI2OWU2MjhlYWE1YzcxMjAwM2QwZWEyZmQiLCJpYXQiOjE3ODA3NDE1MDUsImV4cCI6MTc4MTM0NjMwNX0.oicDfivsM8V8DUxBysMDq92MuIoNbyRfSP54IMps2Ik";

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "https://qa-mesto.praktikum-services.ru";

        // Явное отключение проверки SSL для RestAssured (работает даже при другом HTTP-клиенте)
        RestAssured.useRelaxedHTTPSValidation();

        // Конфигурация таймаутов
        RestAssured.config = RestAssured.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.socket.timeout", 30000)
                        .setParam("http.connection.timeout", 30000)
                        .setParam("http.connection.request.timeout", 30000))
                .sslConfig(new SSLConfig().relaxedHTTPSValidation()); // дублируем для надёжности
    }

    @Test
    @DisplayName("Add a new photo")
    @Description("This test is for adding a new photo to Mesto.")
    void addNewPhoto() {
        given()
                .header("Content-type", "application/json")
                .auth().oauth2(bearerToken)
                .body("{\"name\":\"Москва\",\"link\":\"https://code.s3.yandex.net/qa-automation-engineer/java/files/paid-track/sprint1/photoSelenium.jpg\"}")
                .post("/api/cards")
                .then().statusCode(201);
    }

    @Test
    @DisplayName("Like the first photo")
    @Description("This test is for liking the first photo on Mesto.")
    public void likeTheFirstPhoto() {
        String photoId = getTheFirstPhotoId();
        likePhotoById(photoId);
        deleteLikePhotoById(photoId);
    }

    @Step("Take the first photo from the list")
    private String getTheFirstPhotoId() {
        return given()
                .auth().oauth2(bearerToken)
                .get("/api/cards")
                .then().extract().body().path("data[0]._id");
    }

    @Step("Like a photo by id")
    private void likePhotoById(String photoId) {
        given()
                .auth().oauth2(bearerToken)
                .put("/api/cards/{photoId}/likes", photoId)
                .then().assertThat().statusCode(200);
    }

    @Step("Delete like from the photo by id")
    private void deleteLikePhotoById(String photoId) {
        given()
                .auth().oauth2(bearerToken)
                .delete("/api/cards/{photoId}/likes", photoId)
                .then().assertThat().statusCode(200);
    }
}