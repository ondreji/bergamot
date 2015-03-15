package com.intrbiz.bergamot.crypto.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.CertificationRequest;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

@SuppressWarnings("deprecation")
public class PEMUtil
{
    static
    {
        // as much as I dislike this, we need the BC provider to load a PEM formatted cert reliably!
        Security.addProvider(new BouncyCastleProvider());
    }
    
    public static String saveCertificate(Certificate cert)
    {
        StringWriter sw = new StringWriter();
        try (PEMWriter pw = new PEMWriter(sw))
        {
            pw.writeObject(cert);
        }
        catch (IOException e)
        {
        }
        return sw.toString();
    }
    
    public static void saveCertificate(Certificate cert, Writer to) throws IOException
    {
        try (PEMWriter pw = new PEMWriter(to))
        {
            pw.writeObject(cert);
        }
    }
    
    public static void saveCertificate(Certificate cert, File to) throws IOException
    {
        try (PEMWriter pw = new PEMWriter(new FileWriter(to)))
        {
            pw.writeObject(cert);
        }
    }
    
    public static Certificate loadCertificate(File file) throws IOException
    {
        try (PEMReader pr = new PEMReader(new FileReader(file)))
        {
            return (Certificate) pr.readObject();
        }
    }
    
    public static Certificate loadCertificate(String data) throws IOException
    {
        try (PEMReader pr = new PEMReader(new StringReader(data)))
        {
            return (Certificate) pr.readObject();
        }
    }
    
    public static PrivateKey loadKey(File file) throws IOException
    {
        try
        {
            // need to use some BC classes to parse PEM files
            // fecking Java, POS at times
            try (PemReader pr = new PemReader(new FileReader(file)))
            {
                PemObject obj = pr.readPemObject();
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PrivateKey key = kf.generatePrivate(new PKCS8EncodedKeySpec(obj.getContent()));
                return key;
            }
        }
        catch (Exception e)
        {
            throw new IOException("Error loading key", e);
        }
    }
    
    public static PrivateKey loadKey(String data) throws IOException
    {
        try
        {
            // need to use some BC classes to parse PEM files
            // fecking Java, POS at times
            try (PemReader pr = new PemReader(new StringReader(data)))
            {
                PemObject obj = pr.readPemObject();
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PrivateKey key = kf.generatePrivate(new PKCS8EncodedKeySpec(obj.getContent()));
                return key;
            }
        }
        catch (Exception e)
        {
            throw new IOException("Error loading key", e);
        }
    }
    
    public static String saveKey(PrivateKey key)
    {
        StringWriter sw = new StringWriter();
        try (PEMWriter pw = new PEMWriter(sw))
        {
            pw.writeObject(key);
        }
        catch (IOException e)
        {
        }
        return sw.toString();
    }
    
    public static void saveKey(PrivateKey key, Writer to) throws IOException
    {
        try (PEMWriter pw = new PEMWriter(to))
        {
            pw.writeObject(key);
        }
    }
    
    public static void saveKey(PrivateKey key, File to) throws IOException
    {
        try (PEMWriter pw = new PEMWriter(new FileWriter(to)))
        {
            pw.writeObject(key);
        }
    }
    
    public static CertificateRequest loadCertificateRequest(Reader reader) throws IOException
    {
        try (PEMReader pr = new PEMReader(reader))
        {
            CertificationRequest req = (CertificationRequest) pr.readObject();
            // get the CN
            String cn = (String) ((X509Name) req.getCertificationRequestInfo().getSubject()).getValues(new DERObjectIdentifier("2.5.4.3")).get(0);
            // build the key
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey key = kf.generatePublic(new RSAPublicKeySpec(
                    ((ASN1Integer)((DERSequence) req.getCertificationRequestInfo().getSubjectPublicKeyInfo().getPublicKey()).getObjectAt(0)).getValue(), 
                    ((ASN1Integer)((DERSequence) req.getCertificationRequestInfo().getSubjectPublicKeyInfo().getPublicKey()).getObjectAt(1)).getValue()
            ));
            return new CertificateRequest(cn, key);
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException e)
        {
            throw new IOException("Failed to parse certificate request", e);
        }
    }
    
    public static CertificateRequest loadCertificateRequest(File file) throws IOException
    {
        return loadCertificateRequest(new FileReader(file));
    }
    
    public static CertificateRequest loadCertificateRequest(String data) throws IOException
    {
        return loadCertificateRequest(new StringReader(data));
    }
}
