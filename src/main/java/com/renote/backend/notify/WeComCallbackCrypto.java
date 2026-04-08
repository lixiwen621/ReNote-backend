package com.renote.backend.notify;

import com.renote.backend.common.I18nMessageException;
import com.renote.backend.common.I18nPreconditions;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public class WeComCallbackCrypto {

    private WeComCallbackCrypto() {
    }

    public static boolean verifySignature(String token, String timestamp, String nonce, String encrypted, String msgSignature) {
        if (!StringUtils.hasText(token) || !StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce)
                || !StringUtils.hasText(encrypted) || !StringUtils.hasText(msgSignature)) {
            return false;
        }
        String[] arr = new String[]{token, timestamp, nonce, encrypted};
        Arrays.sort(arr);
        String raw = String.join("", arr);
        return sha1(raw).equals(msgSignature);
    }

    public static String decrypt(String encodingAesKey, String encryptedBase64, String expectedReceiveId) throws Exception {
        byte[] aesKey = Base64.getDecoder().decode(encodingAesKey + "=");
        byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);

        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
        IvParameterSpec iv = new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16));
        cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
        byte[] original = cipher.doFinal(encrypted);
        byte[] bytes = pkcs7Unpad(original);

        // 16字节随机串 + 4字节消息长度 + 消息体 + receiveId
        ByteBuffer lengthBuffer = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 16, 20));
        int xmlLength = lengthBuffer.getInt();
        byte[] xmlBytes = Arrays.copyOfRange(bytes, 20, 20 + xmlLength);
        byte[] receiveIdBytes = Arrays.copyOfRange(bytes, 20 + xmlLength, bytes.length);
        String receiveId = new String(receiveIdBytes, StandardCharsets.UTF_8);

        I18nPreconditions.checkState(!StringUtils.hasText(expectedReceiveId) || expectedReceiveId.equals(receiveId),
                "error.wecom.receiveIdMismatch");
        return new String(xmlBytes, StandardCharsets.UTF_8);
    }

    private static byte[] pkcs7Unpad(byte[] decrypted) {
        int pad = decrypted[decrypted.length - 1] & 0xFF;
        if (pad < 1 || pad > 32) {
            return decrypted;
        }
        return Arrays.copyOfRange(decrypted, 0, decrypted.length - pad);
    }

    private static String sha1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() < 2) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (Exception ex) {
            throw I18nMessageException.of("error.wecom.sha1.failed", ex);
        }
    }
}

