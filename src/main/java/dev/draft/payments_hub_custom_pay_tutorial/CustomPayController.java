package dev.draft.payments_hub_custom_pay_tutorial;

import okhttp3.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
public class CustomPayController {

    private static final OkHttpClient httpClient = new OkHttpClient();

    @CrossOrigin
    @PostMapping(path = "/pay")
    public static String pay(@org.springframework.web.bind.annotation.RequestBody JSONObject inputBody) throws NoSuchAlgorithmException, InvalidKeyException {

        // 1. Extract user input
        String cardNumber = inputBody.get("cardNumber").toString();
        String CVV = inputBody.get("CVV").toString();
        String cardExpiryDate = inputBody.get("cardExpiryDate").toString();
        String amount = inputBody.get("amount").toString();

        // 2. Define other payment-related parameters
        String cardEntryMethod = "X";                               // To denote that the card was keyed in manually
        String industryType = "E";                                  // To denote an e-commerce transaction
        boolean capture = true;                                     // To authorize and capture payment in the same go

        String epiId = "";                           // From your Payments Hub account
        String epiKey = "";         // From your Payments Hub account
        String baseUrl = "https://epi.epxuap.com";                  // From Payments Hub docs
        String endpoint = "/sale";                                  // From Payments Hub docs

        // 3. Define request headers and body content
        String contentType = "application/json";
        Double transactionId = Math.random();
        String orderNumber = String.valueOf(Math.random());
        Double batchId = Math.random();

        JSONObject bodyJSON = new JSONObject();

        bodyJSON.put("account", cardNumber);
        bodyJSON.put("cvv2", CVV);
        bodyJSON.put("expirationDate", cardExpiryDate);
        bodyJSON.put("amount", Float.valueOf(amount));
        bodyJSON.put("transaction", transactionId);
        bodyJSON.put("orderNumber", orderNumber);
        bodyJSON.put("capture", capture);
        bodyJSON.put("industryType", industryType);
        bodyJSON.put("cardEntryMethod", cardEntryMethod);
        bodyJSON.put("batchID", batchId);


        // 4. Create signature from epiKey, endpoint, and body
        String signature = createSignature(endpoint, bodyJSON.toJSONString(), epiKey);

        // 5. Prepare body to send with request
        RequestBody body = RequestBody.create(
                bodyJSON.toJSONString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        // 6. Build the request with correct headers and body content
        Request request = new Request.Builder()
                .url(baseUrl + endpoint)
                .addHeader("Content-Type", contentType)
                .addHeader("epi-Id", epiId)
                .addHeader("epi-signature", signature)
                .post(body)
                .build();

        // 7. Send the request and handle the response
        try (Response response = httpClient.newCall(request).execute()) {

            // Handling failure
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            // Handling success
            JSONParser jsonParser = new JSONParser();
            JSONObject responseBodyJSON = (JSONObject) jsonParser.parse(response.body().string());

            // Sending the payment status back to the caller
            return ((JSONObject) responseBodyJSON.get("data")).get("text").toString();
        } catch (Exception e) {

            // Handling errors
            e.printStackTrace();
            return e.getMessage();
        }

    }

    // Create signature using the HMAC-SHA-256 algorithm from endpoint + payload and epiKey
    public static String createSignature(String endpoint, String payload, String epiKey) throws NoSuchAlgorithmException, InvalidKeyException {
        String algorithm = "HmacSHA256";
        SecretKeySpec secretKeySpec = new SecretKeySpec(epiKey.getBytes(), algorithm);
        Mac mac = Mac.getInstance(algorithm);
        mac.init(secretKeySpec);
        String data = endpoint + payload;
        return bytesToHex(mac.doFinal(data.getBytes()));
    }

    // Utility function for parsing signature
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
