package com.example.crypto;

import android.content.Context;
import android.util.Log;

import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.cert.bc.BcX509v3CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.crypto.params.RSAKeyParameters;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.spongycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.bc.BcRSAContentSignerBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by Tobias on 27.04.16.
 */
public final class CryptoUtils {

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    private static final String KS_FILENAME = "keystore.bks";
    private static final String KS_ALIAS_CERT = "cert";
    private static final String KS_TYPE_BOUNCYCASTLE = "BKS";

    private CryptoUtils() {
        throw new IllegalStateException("No instances.");
    }

    public static Key encrypt(String path, Algo algo) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException, NoSuchProviderException, RSAException, ArrayIndexOutOfBoundsException {
        Key key;
        KeyWrapper keyWrapper = generateKey(algo);

        Cipher cipher = Cipher.getInstance(algo.getName(), BouncyCastleProvider.PROVIDER_NAME);

        if (keyWrapper.hasSecretKey()) {
            key = keyWrapper.getSecretKey();
            cipher.init(Cipher.ENCRYPT_MODE, keyWrapper.getSecretKey());
        } else {
            key = keyWrapper.getKeyPair().getPrivate();
            cipher.init(Cipher.ENCRYPT_MODE, keyWrapper.getKeyPair().getPublic());
        }

        FileInputStream fis = new FileInputStream(path);
        byte[] sourceData = toByteArray(fis);
        fis.close();

        FileOutputStream fos = new FileOutputStream(path);
        CipherOutputStream cos = new CipherOutputStream(fos, cipher);

        try {
            cos.write(sourceData);
            cos.close();

            return key;
        } catch (Exception e) {
            // Datei retten
            fos.write(sourceData);
            fos.close();

            if (e instanceof ArrayIndexOutOfBoundsException && algo.getName().equals(Algo.RSA.getName())) {
                throw new RSAException(e);
            }

            // rethrow
            throw e;
        }
    }

    public static void decrypt(Key key, String path, Algo algo) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException, NoSuchProviderException {
        Cipher cipher = Cipher.getInstance(algo.getName(), BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(Cipher.DECRYPT_MODE, key);

        FileInputStream fis = new FileInputStream(path);
        CipherInputStream cis = new CipherInputStream(fis, cipher);
        byte[] decryptData = toByteArray(cis);
        cis.close();
        fis.close();

        FileOutputStream fos = new FileOutputStream(path);
        fos.write(decryptData);

        fos.close();
    }

    /**
     * Speichert den Key (egal ob symmetrisch - Secret Key oder asymmetrisch - Public Key/ Private Key)
     * in einem Keystore, d.h. einem Container speziell zum Speichern von Keys und Zertifikaten.
     * Ein Keystore schützt seine Einträge vor unerlaubten Zugriff und bietet somit ein hohes Maß an Sicherheit.
     * <p>
     * Wenn der alias schon vergeben ist, wird der alte Eintrag überschrieben.
     *
     * @param context wird benötigt um auf die Keystore-Datei zuzugreifen oder diese zu erstellen.
     * @param alias   wird benötigt, um auf den Key zuzugreifen
     * @param key     symmetrischer oder asymmetrischer Key
     * @return true wenn Key erfolgreich gespeichert, false wenn nicht.
     */
    public static boolean storeKey(Context context, String alias, Key key) {
        InputStream is = null;
        OutputStream os = null;

        try {
            // Es wird der Bouncy Castle KeyStore (BKS) verwendet,
            // da dieser die meisten Algorithmen anbietet und ab API 1 genutzt werden kann.
            KeyStore keyStore = KeyStore.getInstance(KS_TYPE_BOUNCYCASTLE, BouncyCastleProvider.PROVIDER_NAME);

            // Keystore wird innerhalb der App im Files Ordner gespeichert
            File bks = getKsFile(context);
            if (!bks.exists()) {
                keyStore.load(null);
            } else {
                is = new FileInputStream(bks);
                keyStore.load(is, null);
            }

            if (key instanceof PrivateKey) {
                // Bei Private Keys muss zusätzlich ein Zertifikat abgelegt werden.
                // Dies wird hier codeseitig erzeugt (s. generateCertificate).
                // Es muss nur einmalig erzeugt werden und kann danach immer wiederverwendet werden.
                if (!keyStore.containsAlias(KS_ALIAS_CERT)) {
                    keyStore.setCertificateEntry(KS_ALIAS_CERT, generateCertificate());
                    Log.i("demo", "Zertifikat erzeugt.");
                }

                Certificate cert = keyStore.getCertificate(KS_ALIAS_CERT);
                Log.i("demo", "cert != null? " + (cert != null));

                // Passwort wird nicht angegeben
                keyStore.setKeyEntry(alias, key, null, new Certificate[]{cert});
            } else {
                keyStore.setKeyEntry(alias, key, null, null);
            }

            os = new FileOutputStream(bks);
            keyStore.store(os, null);

            // rückwirkend prüfen, ob Eintrag vorhanden
            return keyStore.containsAlias(alias);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }

                if (os != null) {
                    os.flush();
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Key loadKey(Context context, String alias) {
        InputStream is = null;

        try {
            File bks = getKsFile(context);
            if (!bks.exists()) {
                return null;
            }

            KeyStore keyStore = KeyStore.getInstance(KS_TYPE_BOUNCYCASTLE, BouncyCastleProvider.PROVIDER_NAME);

            is = new FileInputStream(bks);
            keyStore.load(is, null);

            return keyStore.getKey(alias, null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @return true wenn Key erfolgreich gelöscht, ansonsten false
     */
    public static boolean deleteKey(Context context, String alias) {
        InputStream is = null;
        OutputStream os = null;

        try {
            File bks = getKsFile(context);
            if (!bks.exists()) {
                return false;
            }

            KeyStore keyStore = KeyStore.getInstance(KS_TYPE_BOUNCYCASTLE, BouncyCastleProvider.PROVIDER_NAME);

            is = new FileInputStream(bks);
            keyStore.load(is, null);

            keyStore.deleteEntry(alias);

            os = new FileOutputStream(bks);
            keyStore.store(os, null);

            // prüfen ob Eintrag gelöscht
            return !keyStore.containsAlias(alias);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }

                if (os != null) {
                    os.flush();
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isStored(Context context, String alias) {
        InputStream is = null;

        try {
            File bks = getKsFile(context);
            if (!bks.exists()) {
                return false;
            }

            KeyStore keyStore = KeyStore.getInstance(KS_TYPE_BOUNCYCASTLE, BouncyCastleProvider.PROVIDER_NAME);

            is = new FileInputStream(bks);
            keyStore.load(is, null);

            return keyStore.containsAlias(alias);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static KeyWrapper generateKey(Algo algo) throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyWrapper keyWrapper;

        if (algo.isSymmetric()) {
            KeyGenerator keyGen = KeyGenerator.getInstance(algo.getName(), BouncyCastleProvider.PROVIDER_NAME);
            keyWrapper = new KeyWrapper(keyGen.generateKey());
        } else {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(algo.getName(), BouncyCastleProvider.PROVIDER_NAME);
            keyWrapper = new KeyWrapper(keyPairGen.generateKeyPair());
        }

        return keyWrapper;
    }

    private static byte[] toByteArray(InputStream is) throws IOException {
        byte[] data = null;

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int i;
        byte[] b = new byte[1024];  // 1 kB Schritte einlesen
        while ((i = is.read(b)) != -1) {
            buffer.write(b, 0, i);
        }

        buffer.flush();
        data = buffer.toByteArray();
        buffer.close();

        return data;
    }

    /**
     * Generiert ein selbst-signiertes Zertifikat nach dem X.509 Standard (Version 3) mithilfe von Bouncy Castle.
     * Dazu wird die Spongy Castle-Bibliothek verwendet, da die von der Android Plattform ausgelieferten Bouncy Castle APIs
     * einige Probleme mit sich bringen und dort die Komponente zur Erstellung von Zertifikaten nicht(mehr) enthalten ist.
     */
    private static Certificate generateCertificate() throws IOException, CertificateException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, OperatorCreationException {
        // CN = Common Name
        // CA = Certificate Authority
        final String SELF_SIGNED_CERTIFICATE_ISSUER_AND_SUBJECT = "CN=CA";
        final String SIGN_ALGO_ID = "SHA1withRSA";

        Calendar notBefore = new GregorianCalendar();
        // Um sicher zu sein, dass das Zertifikat sofort gültig ist.
        notBefore.add(Calendar.SECOND, -1);

        Calendar notAfter = new GregorianCalendar();
        // 1 Jahr Gültigkeit
        notAfter.add(Calendar.YEAR, 1);

        // Da selbst-signiert sind beide gleich.
        X500Name issuerAndSubject = new X500Name(SELF_SIGNED_CERTIFICATE_ISSUER_AND_SUBJECT);

        // Zufällige Seriennummer
        BigInteger serial = BigInteger.valueOf(new Random().nextLong());

        // Asymmetrische Schlüssel für Zertifikat erzeugen
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        KeyPair keyPair = keyPairGen.generateKeyPair();

        RSAPublicKey rsaPubKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey rsaPrivKey = (RSAPrivateKey) keyPair.getPrivate();

        // Schlüssel in passende Spongy Castle Formate umwandeln
        RSAKeyParameters rsaPubKeyParameters = new RSAKeyParameters(false, rsaPubKey.getModulus(), rsaPubKey.getPublicExponent());
        RSAKeyParameters rsaPrivKeyParameters = new RSAKeyParameters(true, rsaPrivKey.getModulus(), rsaPrivKey.getPrivateExponent());

        X509v3CertificateBuilder certBuilder = new BcX509v3CertificateBuilder(
                issuerAndSubject,
                serial,
                notBefore.getTime(),
                notAfter.getTime(),
                issuerAndSubject,
                rsaPubKeyParameters
        );

        // Zertifikat selbst signieren
        AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(SIGN_ALGO_ID);
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

        ContentSigner signer = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(rsaPrivKeyParameters);

        // Zertifikat erstellen
        X509CertificateHolder holder = certBuilder.build(signer);

        // Spongy Castle Zertifikat (X509CertificateHolder) in java.security.cert.X509Certifcate umwandeln
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        converter.setProvider(BouncyCastleProvider.PROVIDER_NAME);

        X509Certificate cert = converter.getCertificate(holder);

        // Gültigkeit und Public Key überprüfen
        cert.checkValidity();
        cert.verify(keyPair.getPublic());

        return cert;
    }

    private static File getKsFile(Context context) {
        return new File(context.getFilesDir(), KS_FILENAME);
    }

}
