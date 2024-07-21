package com.example.secure.share.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class CryptoService {

	private KeyPair keyPair;

	public CryptoService(Environment env) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		String salt = env.getProperty("my-secret-salt");
		SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
		sr.setSeed(salt.getBytes());
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
		kpg.initialize(new ECGenParameterSpec("secp521r1"), sr);
		keyPair = kpg.generateKeyPair();
	}

	public String getPublicKey() {
		return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
	}

	public byte[] encrypt(String clientPubKey, byte[] inputBytes) throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException, IllegalStateException, IllegalBlockSizeException,
			BadPaddingException, IOException, InvalidKeySpecException, InvalidAlgorithmParameterException {
		Key sharedKey = getSharedKey(clientPubKey);
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, sharedKey, getIvParameter());
		byte[] outputBytes = cipher.doFinal(inputBytes);
		return outputBytes;
	}

	public InputStream decrypt(String clientPubKey, byte[] inputBytes) throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException, IllegalStateException, IllegalBlockSizeException,
			BadPaddingException, IOException, InvalidKeySpecException, InvalidAlgorithmParameterException {
		Key sharedKey = getSharedKey(clientPubKey);
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, sharedKey, getIvParameter());
		byte[] outputBytes = cipher.doFinal(inputBytes);
		return new ByteArrayInputStream(outputBytes);
	}

	private Key getSharedKey(String clientPubKey) throws NoSuchAlgorithmException, InvalidKeyException,
			IllegalStateException, InvalidKeySpecException, InvalidAlgorithmParameterException {
		byte[] decodedPubKey = Base64.getDecoder().decode(clientPubKey);
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decodedPubKey);
		PublicKey clientPublicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
		KeyAgreement ka = KeyAgreement.getInstance("ECDH");
		ka.init(keyPair.getPrivate());
		ka.doPhase(clientPublicKey, true);
		byte[] sharedSecret = ka.generateSecret();
		SecretKey sharedKey = new SecretKeySpec(sharedSecret, 0, 32, "AES");
		return sharedKey;
	}

	private IvParameterSpec getIvParameter() {
		byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		return new IvParameterSpec(iv);
	}

}
