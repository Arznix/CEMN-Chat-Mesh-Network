package cemn.cemnchat;


/**
 * Created by HLEW on 2017-09-22.
 */
import android.content.Context;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Xml;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

//import javax.crypto; // This package provides the classes and interfaces for cryptographic applications implementing algorithms for encryption, decryption, or key agreement.
//import javax.crypto.interfaces; //This package provides the interfaces needed to implement the key agreement algorithm.
//import javax.crypto.spec; //This package provides the classes and interfaces needed to specify keys and parameter for encryption.

//******************************************
//
//  NOTE:   Methods (member functions of a class) can only pass values in as parameters. You can
//          not pass references. So the concept of passing the address of a variable then getting
//          the results of the operations in the method through the reference does not exit.
//          The parameters variable remain the  same as only copies of the parameters
//          are operated on in the method.
//
//          So passing the variable a and b when a=2 and b=3 into a method that swaps the values of a and b
//          will not work. a will still 2 and b will still equal 3 even after swap as only copies of the values
//          of a and b are operated on in the function.
//
//          As a result is necessary in some cases to declare complex class that have mutliple variables
//          declared within them so that a method can return a variable of that complex class. Otherwise only
//          value cn be returned at a time.
//
//          So the case of the Crypto class it is necessary to return a Key Pair. So the new class MyKeyPair is defined
//          to have a PrivateKey and the PublicKey of type string so that a method generating a Key Pair can return a variable
//          MyKeyPair where MyKeyPair.ThePrivateKey and MyKeyPair.ThePublicKey can be returned indirectly.
//
//
//*******************************************

public class Crypto {

    private static String SALT = "some_salt_to_change!";    //This is some artibtrary and randomly generated string that is used to SALT
    //SALT the encryption algoritm to make it more difficult to break
    private final static String HEX = "0123456789ABCDEF";

    public String TAG = "Crypto Errors:";
    private Context mContext;
    //This variable are declared as "static" so they will be the small cross all instants of the class.
    //You should only generate the keys once for the whole Application. There should be no need
    //to genrate new keys except in the case where you want to replace the existing key.

    static KeyPair TheAsymmetricKeyPair;    //Asymmetric Key Storage for Public and Private Keys RSA 1024 or RSA 2018
    static String TheSymmetricKey;      //Symmetric Key storage - AES 256
    //Hash by MD5
    static SecretKey MySymmetricKey;

    //  ***** WARNING*********
    //  These constants below are used to define the file name for storing the Public Key
    //  and the Private Key for the Asymmetric Encryption and the key for the Symmetric Encryption
    //  persistently on the mobile devices storage memory so that it is not generated everytime the
    //  program is launched.
    //
    //  For clarity in this open source the file name have been made easy to identify.
    //
    //  In practical these should be written to an obscurely named files in an obscure place in the
    //  mobile devices so that you keys can not be easily found if someone gets access to your
    //  system. Keep in mind for the most part if you keep the files obsucrely name and in an obscure
    //  location in memory then someone not authorized to access you system need physical access
    //  to you device to get access to you keys.
    //
    //********************************************

    private final static String PUBLIC_KEY_FILE ="Public.key";
    private final static String PRIVATE_KEY_FILE ="Private.key";

    //===========================================
    //
    //  Function: Crypto()
    //  Parameters:
    //  Globals:
    //  Externals:
    //  Returns:
    //  Purpose: Constructor for class
    //
    //*******************************************

    public Crypto() {
    }

    //===========================================
    //
    // Function: public static String bytesToString(byte[] b)
    // Paramenters: byte[] b
    // Globals: none
    // Externals: nil
    // Returns: String
    //===========================================

    public static String bytesToString(byte[] b) {
        byte[] b2 = new byte[b.length + 1];
        b2[0] = 1;
        System.arraycopy(b, 0, b2, 1, b.length);
        return new BigInteger(b2).toString(36);
    }

    //===========================================
    //
    // Function: public static byte[] stringToBytes(String s)
    // Paramenters: String s
    // Globals: none
    // Externals: nil
    // Returns: byte[]
    //===========================================

    public static byte[] stringToBytes(String s) {
        byte[] b2 = new BigInteger(s, 36).toByteArray();
        return Arrays.copyOfRange(b2, 1, b2.length);
    }

    //===========================================
    //
    // Function: public void ListSupportedAlgorithms()
    // Paramenters: String results
    // Globals: none
    // Externals: nil
    // Returns:
    //      A String variable containing the list of support encryption algorithms.
    // Purpose: This function returns a list of the cryptography
    //          algorithms that can be supported on the device.
    //          The Android Cryptograph libarary suppports
    //          an order version of Bouncy Castle, an open source
    //          cryptograph library - http://www.bouncycastle.org/
    //
    //*******************************************

    public String ListSupportedAlgorithms() {
        String sResult = "";

        // get all the providers
        Provider[] providers = Security.getProviders();

        for (int p = 0; p < providers.length; p++) {
            // get all service types for a specific provider
            Set<Object> ks = providers[p].keySet();
            Set<String> servicetypes = new TreeSet<String>();
            for (Iterator<Object> it = ks.iterator(); it.hasNext(); ) {
                String k = it.next().toString();
                k = k.split(" ")[0];
                if (k.startsWith("Alg.Alias."))
                    k = k.substring(10);

                servicetypes.add(k.substring(0, k.indexOf('.')));
            }

            // get all algorithms for a specific service type
            int s = 1;
            for (Iterator<String> its = servicetypes.iterator(); its.hasNext(); ) {
                String stype = its.next();
                Set<String> algorithms = new TreeSet<String>();
                for (Iterator<Object> it = ks.iterator(); it.hasNext(); ) {
                    String k = it.next().toString();
                    k = k.split(" ")[0];
                    if (k.startsWith(stype + "."))
                        algorithms.add(k.substring(stype.length() + 1));
                    else if (k.startsWith("Alg.Alias." + stype + "."))
                        algorithms.add(k.substring(stype.length() + 11));
                }

                int a = 1;
                for (Iterator<String> ita = algorithms.iterator(); ita.hasNext(); ) {
                    sResult += ("[P#" + (p + 1) + ":" + providers[p].getName() + "]" +
                            "[S#" + s + ":" + stype + "]" +
                            "[A#" + a + ":" + ita.next() + "]\n");
                    a++;
                } //for

                s++;
            } //for
        } //for

        return (sResult); //to export the results out of this function

    } //end of function definition ListSupportedAlgorithms()

    //========================================
    //
    //  This next section of code implements Symmetric Encryption.
    //
    //  The function implement the AES 256 method of encoding and decoding text data.
    //========================================


    //=======================================
    //  Function: CreateSymmetryKey()
    //  Parameters:
    //              String Seed - seed (randomized) string to feed into the encryption
    //
    //  Globals: NIL
    //  Externals: NIL
    //  Returns:
    //      Returns the the symmetric key of type SecretKey
    //  Purpose:
    //  This function takes "seed", essentially a randomized string to help start the encryption process
    //
    //
    //*****************************************

    /*
    public static SecretKey CreateSymmetryKey() {

        SecretKey key;

        key = generateSymmetricKey();   //this ithe short form, a more elaborate form exist
        //refer to the description of GeneraeSymmetricKey()
        return key;
    }
    */


    public static String CreateSymmetryKey() {

        SecretKey key;

        key = generateSymmetricKey();   //this ithe short form, a more elaborate form exist
        //refer to the description of GeneraeSymmetricKey()
        byte[] byteaes=key.getEncoded();
        return (Base64.encodeToString(byteaes,Base64.NO_WRAP));
    }

    //===============================
    //
    //  Function: SymmetricEncrypt()
    //  Parameters:
    //      cleantext - raw, clean text to be encoded of type String
    //      AESKey - symmetric key to be used to encode the raw text
    //  Globals: nil
    //  Externals: nil
    //  Assumes:
    //  Returns:
    //      The encoded text using the symmetric key and the AES-256 algorithm is return in type String
    //  Purpose:
    //      This function ecrypts raw text in a String variable using an AES-256 symmetric key
    //      and then returns the encrypted String variable that contained the resulting encrypted text.
    //
    //*******************************
//public static String SymmetricEncrypt(String cleantext, SecretKey AESKey ) {
    public static String SymmetricEncrypt(String cleantext, String AESKey ) {
        //String seed = "sAJOZkKGArL9f98MmAGrQQ==";
        byte[] keyb = new byte[0];
        try {
            keyb = AESKey.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] rawKey = md.digest(keyb);
        //byte[] rawKey = AESKey.getEncoded();
        byte[] result = encryptSymmetric(rawKey, cleantext.getBytes());
        return toHex(result);

    }

    //=======================================
    //
    //  Function: SymmetricDecrypt()
    //  Parameters:
    //      encrypted - encrypted text to be decoded of type String
    //      AESKey - symmetric key for AES alogrithm to decoding the "encrypted" text
    //
    //***************************************

    //public static String SymmetricDecrypt(String encrypted, SecretKey AESKey) {
    public static String SymmetricDecrypt(String encrypted, String AESKey) {
        byte[] keyb = new byte[0];
        try {
            keyb = AESKey.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] rawKey = md.digest(keyb);
        //byte[] rawKey = AESKey.getEncoded();
        byte[] enc = toByte(encrypted);
        byte[] result = decryptSymmetric(rawKey, enc);
        return new String(result);

    }

    //============================================
    //
    //  Function: generateSymmetricKey()
    //  Parameters: nil
    //  Globals: nil
    //  Externals: nil
    //  Returns:
    //      Returns a symmetric key of type SecretKey
    // Purpose:
    //      This function is the simplest way to generate an AES symmetric key.
    //      In order to generaet the key you need a random number. Android suggest
    //      that you do not use the SecureRandom(0 function with a seed as it is not
    //      secure and the OpenSSL technique that overrides the SecureRandom() function
    //      no longer works.
    //
    //      Make this call on the start of the App when the system is most unpredictable
    //      as SecureRandom() draws its seed from the system state.
    //
    //      This symmetric encryption method uses AES (Advanced Encryption Standard).
    //
    //      AES is comprised of AES-128, AES-192 and AES-256. The key bit you choose encrypts
    //      and decrypts blocks in 128 bits, 192 bits and so on. There are different rounds
    //      for each bit key. A round is the process of turning plaintext into cipher text.
    // //   For 128-bit, there are 10 rounds; 192-bit has 12 rounds; and 256-bit has 14 rounds.
    //
    //      http://www.toptenreviews.com/software/articles/secure-encryption-methods/
    //
    //      If additional security is required then the link below provides a link to
    //      a method that uses a PassPhrase and a veriation of SHA1 - PBKDF2WithHmacSHA1
    //
    //  https://android-developers.googleblog.com/2013/02/using-cryptography-to-store-credentials.html
    //
    //*****************************************


    public static SecretKey generateSymmetricKey()  {

        String TAG = "Crypto Errors:";

        // Generate a 256-bit key
        final int outputKeyLength = 256; //256 bit is the high value for AES currently in common use

        SecureRandom secureRandom = new SecureRandom();
        // Do *not* seed secureRandom! Automatically seeded from system entropy if the SecureRandom()
        //call has no parameter.
        KeyGenerator keyGenerator;
        try {
            keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(outputKeyLength, secureRandom);
            SecretKey key = keyGenerator.generateKey();
            return key;
        }
        catch (NoSuchAlgorithmException e)
        {
            Log.i(TAG, "No Such Algorithm in generateSymmetricKey()");
            return null;
        }



    }

    //====================================
    //
    //  Function: ecryptSymmetric()
    //  Parameters:
    //      rawkey - symmetric key to be used for encryting the text
    //      clear - byte [] array containing the unecrypted text
    //  Globals: nil
    //  Externals: nil
    //  Returns:
    //      array of type byte [] that contains the encrypted text
    //  Assumes:
    //  Purpose:
    //      This is for symmetric AES-256 encryption. Note that in
    //      symmetric encryption the same key is used to encode and
    //      decode the message.
    //
    //*************************************

    private static byte[] encryptSymmetric(byte[] rawkey, byte[] clear)  {

        String TAG = "Crypto Errors:";

        SecretKeySpec skeySpec = new SecretKeySpec(rawkey, "AES");

        try {
            //initialize the cipler method to AES encryption
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            //execute the encryption using the doFinal member method
            byte[] encrypted = cipher.doFinal(clear);
            return encrypted;
        }
        catch(NoSuchAlgorithmException e) {
            Log.i(TAG, "No Such Algorithm in encryptSymmetric()");
            return null;
        }
        catch(NoSuchPaddingException e){
            Log.i(TAG, "No Such Padding in encryptSymmetric()");
            return null;
        }
        catch(InvalidKeyException e) {
            Log.i(TAG, "No Such Padding in encryptSymmetric()");
            return null;
        }
        catch(IllegalBlockSizeException e) {
            Log.i(TAG, "Illegal Block Size in encryptSymmetric()");
            return null;
        }
        catch(BadPaddingException e) {
            Log.i(TAG, "Bed Padding in encryptSymmetric()");
            return null;
        }

    }

    //================================
    //
    //  Function: decryptSymmetric()
    //  Parametners:
    //      byte[] symmetric_key;   character buffer containing the symmetric key
    //      byte[] encrypted_text  character buffer of the encrypted text
    //  Globals: nil
    //  Externals: nil
    //  Returns:
    //      byte[] buffer containing the decrypted text
    //  Purpose:
    //      This function takes 2 character buffers, one containing the symmetric key and
    //      one contained the encrypted text and it returns the decrypted text in a character
    //      buffer
    //
    //***********************************

    private static byte[] decryptSymmetric(byte[] symmetric_key, byte[] encrypted_text) {

        String TAG = "Crypto Errors:";

        SecretKeySpec skeySpec = new SecretKeySpec(symmetric_key, "AES");
        try {
            //initiatl cipher method to AES algorithm
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            //execute the decryption using the member doFinal() method
            byte[] decrypted = cipher.doFinal(encrypted_text);
            return decrypted;
        }
        catch(NoSuchAlgorithmException e){
            Log.i(TAG, "No Such Algorithm  in decryptSymmetric()");
            return null;
        }
        catch(NoSuchPaddingException e){
            Log.i(TAG, "No Such Padding  in decryptSymmetric()");
            return null;
        }
        catch(InvalidKeyException e) {
            Log.i(TAG, "Invalid Key in decryptSymmetric()");
            return null;
        }
        catch(IllegalBlockSizeException e){
            Log.i(TAG, "Illegal Block Size in decryptSymmetric()");
            return null;
        }
        catch(BadPaddingException e){
            Log.i(TAG, "Bad Padding in decryptSymmetric()");
            return null;
        }
    }

    //==================================
    //
    //  Funciton: toHex()
    //  Parameters:
    //      String txt; buffer of characters to convert to hext values
    //  Globals:
    //  Externals:
    //  Returns:
    //      returns a buffer to type String which contain the hex equivalent value
    //      of every character in the original string passed to the function as a parameter
    //
    //***********************************

    public static String toHex(String txt) {
        return toHex(txt.getBytes());
    }

    public static String fromHex(String hex) {
        return new String(toByte(hex));
    }

    //====================================
    //
    //  Function: toByte()
    //  Parametners:
    //      String hexString;   this is a buffer of hex values that need to be convert back
    //                          byte equivalents
    //  Globals:nil
    //  Externals: nil
    //  Returns:
    //          byte result;
    //  Purpose:
    //          This function takes a buffer of hex values and converts them back to
    //           their character (text) equivalents
    //
    //*****************************************

    public static byte[] toByte(String hexString) {
        int len = hexString.length() / 2;
        byte[] result = new byte[len];

        for (int i = 0; i < len; i++)
            result[i] = Integer.valueOf(hexString.
                    substring(2 * i, 2 * i + 2),16).byteValue();

        return result;
    }

    //======================================
    //
    //  Function toHex()
    //  Parameters:
    //                byte[] buf; character buff to be converted
    //  Globals: nil
    //  Externals: nil
    //  Returns:
    //      Returns a String of the converted charaters
    //  Purpose:
    //      This function takes a character buffer passed to it through the parameter
    //      and converts each byte in the buffer to their hex equivalent
    //
    //****************************************

    public static String toHex(byte[] buf) {
        if (buf == null)
            return "";
        //buffer to contain the the converted bytes of the input buffer in hex format
        StringBuffer result = new StringBuffer(2 * buf.length);

        for (int i = 0; i < buf.length; i++) {
            appendHex(result, buf[i]);
        }

        return result.toString();
    }

    //=============================================
    //
    //  Function: appendHex()
    //  Parameters:
    //                  StringBuffer sb;   string buffer to append characters to
    //                  byte b;             byte to convert into a hex value
    //  Global: nil
    //  External: nil
    //  Returns: nil
    //  Purpose:
    //      This function takes a byte value, converts it to its hex equivalent
    //      and append it to the char string that was passed to it as a parameter
    //
    //
    //-----------------------------------------------

    private static void appendHex(StringBuffer sb, byte b) {
        sb.append(HEX.charAt((b >> 4) & 0x0f)).append(HEX.charAt(b & 0x0f));
    }


    //========================================
    //
    // This section of code applied to the Asymmetric Encryption Method.
    //
    //  This functions implements either the p 1024 and p 2048 encryotion.
    //
    //****************************************

    //=======================================
    //
    //  Function: generateAsymmetricKeys()
    //  Parameters:
    //      MyPublicKey - pass in a variable to indirectly return the Public Key
    //      MyPrivateKey - pass in a variable to indirectly return the Private Key
    //  Globals: nil
    //  Externals: nil
    //  Returns:
    //      return 0 if there is an error
    //      retur 1 if the keys are generated properly
    //  Purpose:
    //      The function generates a set of keys for Asymmetric Encryption.
    //      The p 1024 or 2048 method of ecryption is used. 1024 and 2048 refers
    //      to the number of bits to be used in the encryption key. RSA 2048 will
    //      take longer than RSA 1024 to generate. If there is too much of the delay
    //      in the encryption process the user will experience will be degraded
    //      as the user will wonder why it is taking so long to send and receive a message.
    //
    //      "RSA is only able to encrypt data to a maximum amount of your key size
    //      (2048 bits = 256 bytes) minus padding / header data (11 bytes for PKCS#1 v1.5 padding).
    //
    //      As a result it is often not possible to encrypt files with RSA directly. RSA is
    //      also not meant for this purpose. If you want to encrypt more data, you can use something like:
    //
    //          Generate a 256-bit random keystring K
    //          Encrypt your data with AES-CBC with K
    //          Encrypt K with RSA
    //          Send both to the other side"
    //
    //          https://tls.mbed.org/kb/cryptography/rsa-encryption-maximum-data-size
    //
    //**************************************

    // Generate key pair for 1024-bit RSA encryption and decryption

    public static KeyPair generateAsymmetricKeys( ) {

        String TAG = "Crypto Error:";

        PublicKey MyPublicKey;
        PrivateKey MyPrivateKey;

        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);  //1024 bit or 2048 bits
            KeyPair kp = kpg.genKeyPair();
            MyPublicKey = kp.getPublic();
            MyPrivateKey = kp.getPrivate();
            //TheKeyPair.setPrivateKey() = MyPrivateKey;
            //TheKeyPair.setPublicKey() = MyPublicKey;
            return kp;
        } catch (Exception e) {
            //Log.e(TAG, "RSA key pair error");
            return null;
        }
    }


    //====================================
    //
    //  Function: AsymmetricEncrypt()
    //  Parameters:
    //      PublicKey - public key to use to encrypt the text data of type Key
    //      TextBuf - character buffer containing the text to be encrypted
    //      EncryptedText - character buf the encrrypted text is returned
    //  Globals: nil
    //  Externals: nil
    //  Returns:
    //        return 0 if an error occures
    //        return 1 if no error
    //  Purpose:
    //      The function takes a character buffer containing the text data
    //      to be encrypted and then encrypts it using the Public Key of the user.
    //      The data is encrypted in RSA 1024 or 2048 depending on the speed and
    //      level of security required. 1024 is still tne most common. 2048 is
    //      no anticipated to be cracked till 2030.
    //
    //      Usual TheKey is the Public Key except in the case of a Digital Signature
    //      when it will be the Private Key
    //
    //************************************


    public static byte[] AsymmetricEncrypt(PublicKey TheKey, String sTextBuf ) {

        String TAG = "Crypto Error:";
        String sEncryptedText = " ";
        byte []  bBuf = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            bBuf = sTextBuf.getBytes(StandardCharsets.US_ASCII);
        }

        // Encode the original data with RSA private key
        byte[] encodedBytes = null;
        try {
            //initialize cipher object to RSA algorithm with padding
            Cipher c = Cipher.getInstance("RSA/None/PKCS1Padding"); //+x+x+    ("/ECB/PKCS1Padding");
            c.init(Cipher.ENCRYPT_MODE, TheKey);
            encodedBytes = c.doFinal(sTextBuf.getBytes());
            //execute the encryption using the member doFinal() method
            return encodedBytes;
        } catch (Exception e) {
            //Log.e(TAG, "RSA encryption error");
            return null;
        }

    }
    //===============================
    //
    //  Function: AsymmetricEncrypt()
    //  Parameters:
    //      TheKey - PrivateKey - used for creating digital signatures only
    //      sTextBuf - buffer of text to be encrypted of type String
    //  Globals: nil
    //  Assumes:
    //  Returns:
    //      returns the encrypted data in an array of type "byte"
    //  Purpose:
    //      THis is a special case of AsymmetricEncrypt(). THis function usually uses
    //      a public key for encryption, but in the case of creating a digital signature
    //      the hash value of the text to be send is encrypted using the Private Key.
    //      The result is sent as part of the message as a signature. The signature is decoded
    //      by the recipient using the Public Key of the assumed sender. The hash of the decoded
    //      message is then calculated. The calculated hash should match the value of the decoded
    //      signature.
    //
    //      NOTE: This is currently NOT working. The coversion from String to byte []
    //      appears to create and issue. String uses 16 bit for a char. Converting to byte[]
    //      using the getByte[] method does not result in the same result as generated by
    //      the AsymmetricEncrypt() method.
    //
    //*******************************


    public static byte[] AsymmetricEncrypt(PrivateKey TheKey, String sTextBuf ){

        String TAG = "Crypto Error:";
        String sEncryptedText = " ";

        // Encode the original data with RSA private key
        byte[] encodedBytes = null;
        try {
            Cipher c = Cipher.getInstance("RSA/None/PKCS1Padding"); //("RSA/ECB/PKCS1Padding");
            c.init(Cipher.ENCRYPT_MODE, TheKey);
            encodedBytes = c.doFinal(sTextBuf.getBytes());
            return encodedBytes;
        } catch (Exception e) {
            //Log.e(TAG, "RSA encryption error");
            return null;
        }
        /*
        sEncryptedText = "[ENCODED]:\n";
        sEncryptedText += Base64.encodeToString(encodedBytes, Base64.DEFAULT);
        sEncryptedText += "\n";

        return sEncryptedText;
        */
    }

    //====================================
    // Function AsymmetricDecodePrivate()
    // Parameters:
    //  TheKey - Private Key used to decode the message for the asymmetric encryption
    //  bBuf - array of encrypted text for the message of type byte []
    //  Globals: nil
    //  Externals: nil
    //  Assumes: the encryption method is RSA/None/PKCS1Padding
    //  Returns:
    //      returns a byte [] array with the decoded text of 8 bit ascii
    //  Purpose:
    //      This function take the symmetrically encrypted message and coverts it
    //      back to the original text using the Private Key of the recipient.
    //
    //
    //*************************************

    public static byte[] AsymmetricDecodePrivate (PrivateKey TheKey, byte[] bBuf)

    {
        String TAG = "Crypto Error:";
        byte[] encodedBytes = null; //character array of encoded text
        byte[] decodedBytes = null; //character array of decode tet
        int len; //length of string
        int i; //index for "for" loop

        String sAlgorithm = new String();
        sAlgorithm = " ";

        try {
            //initialize the cipher object with the encryption/decrypton algorith,
            Cipher c = Cipher.getInstance("RSA/None/PKCS1Padding");
            c.init(Cipher.DECRYPT_MODE, TheKey);

            sAlgorithm = c.getAlgorithm();

            // decode the info byte[] array
            decodedBytes = c.doFinal(bBuf);
            //In order to convert Byte array into String format correctly,
            // we have to explicitly create a String object and assign the Byte array to it.
            String sDecodedText = new String(decodedBytes);
            return decodedBytes;
        } catch (Exception e)

        {
            //Log.e(TAG, "RSA decryption error");
            String nullString = "";
            return null; // (nullString);
        }
    }

    //====================================
    // Function AsymmetricDecodePrivate()
    // Parameters:
    //  TheKey - Private Key used to decode the message for the asymmetric encryption
    //  bBuf - array of encrypted text for the message of type byte []
    //  Globals: nil
    //  Externals: nil
    //  Assumes: the encryption method is RSA/None/PKCS1Padding
    //  Returns:
    //      returns a byte [] array with the decoded text of 8 bit ascii
    //  Purpose:
    //      This function take the symmetrically encrypted message and coverts it
    //      back to the original text using the Private Key of the recipient.
    //
    //
    //*************************************

    public static byte[] AsymmetricDecodePrivate (PrivateKey TheKey, String sEncryptedText, byte[] bBuf)

    {
        String TAG = "Crypto Error:";
        byte[] encodedBytes = null; //character array of encoded text
        byte[] decodedBytes = null; //character array of decode tet
        int len; //length of string
        int i; //index for "for" loop
        len = sEncryptedText.length();

        //make sure to specify the character set as the default bit size for "char" in Android is 16 bits not 8 bits
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            encodedBytes = sEncryptedText.getBytes(StandardCharsets.US_ASCII);
        }

        String sAlgorithm = new String();

        sAlgorithm = " ";

        try {
            //initialize the cipher object to RSA Algorithm with padding
            Cipher c = Cipher.getInstance("RSA/None/PKCS1Padding");
            c.init(Cipher.DECRYPT_MODE, TheKey);

            sAlgorithm = c.getAlgorithm();

            // Feed in direct output of AsymmetricEncrypt instead of stirng
            //execute the decrpytion using doFinal() method
            decodedBytes = c.doFinal(bBuf);
            //In order to convert Byte array into String format correctly,
            // we have to explicitly create a String object and assign the Byte array to it.
            String sDecodedText = new String(decodedBytes);
            return decodedBytes;
        } catch (Exception e)

        {
            //Log.e(TAG, "RSA decryption error");
            String nullString = "";
            return null; // (nullString);
        }
    }
    //==================================
    //
    //  Function: AsymmetricDecode()
    //  Paramaeters:
    //      PrivateKey - private key for the asymmetric encryption method
    //      sEncryptedText - text string of type String of the text to be decoded
    //      sDecodedText - text string of type string of the decode text
    //  Globals: nil
    //  Externals: nil
    //  Returns:
    //      the decoded text as a string
    //  Purpose:
    //      Decode the encoded data with RSA private key.
    //
    //      Usually the THeKey parameter is the Private Key except when it is a Digital Signature
    //      in which case it is the Public Key.
    //
    //      NOTE: •RSA encryption data size limitations are slightly less than the key
    //      modulus size, depending on the actual padding scheme used (e.g. with 1024 bit
    //      (128 byte) RSA key, the size limit is 117 bytes for PKCS#1 v 1.5 padding.
    //      Other padding schemes will have slightly different size limitations.
    //
    //      http://www.jensign.com/JavaScience/dotnet/RSAEncrypt/index.html
    //
    //***********************************

    public static byte[] AsymmetricDecodePrivate (PrivateKey TheKey, String sEncryptedText)

    {
        String TAG = "Crypto Error:";
        byte[] encodedBytes = null; //character array of encoded text
        byte[] decodedBytes = null; //character array of decode tet
        int len; //length of string
        int i; //index for "for" loop
        len = sEncryptedText.length();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            encodedBytes = sEncryptedText.getBytes(StandardCharsets.US_ASCII); //(Charset.forName("UTF-8"));
        }
        //****might be wrong here

        //byte [] bTextBuffer = toByte(sEncryptedText);

        String sAlgorithm = new String();

        sAlgorithm = " ";

        //byte [] ByteArray = toByte(sEncryptedText);

        try {
            Cipher c = Cipher.getInstance("RSA/None/PKCS1Padding");
            c.init(Cipher.DECRYPT_MODE, TheKey);

            sAlgorithm = c.getAlgorithm();

            decodedBytes = c.doFinal(encodedBytes);
            //In order to convert Byte array into String format correctly,
            // we have to explicitly create a String object and assign the Byte array to it.
            String sDecodedText = new String(decodedBytes);
            return decodedBytes;
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (IllegalBlockSizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (BadPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }catch (Exception e){
            //Log.e(TAG, "RSA decryption error");
            String nullString = "";
            return null; // (nullString);
        }
    }

    //====================================
    //
    //  Function: AsymmetricDecode()
    //  Paramters:
    //      TheyKey - Public Key from asymmetric key pair
    //      sEncryptedText - encrypted text to be decoded of type String
    //  Globals: nil
    //  Externals: nil
    //  Assumes:
    //  Purpose:
    //      This is special version of AsymmetricDecode() that uses the Public Key of the recipient
    //      rather than the private key. This is used to decode the signature part of the received message.
    //      The recipient can verify the assumed sender is the actually the sender by verifying the signature.
    //      Then sender calculates the HASH of the text of the message to be sent. The sender than encodes the
    //      HASH value their Private Key. The recipient decodes the whole message. They then calulaute the HASH
    //      of the text of teh message. The recipient then decodes the signature using teh Public Key of the assumed
    //      sender. The HASH calculate and the HASH contained in the decoded signature should match is the sender
    //      is who he says he is/
    //
    //*************************************

    public static byte[] AsymmetricDecodePublic (PublicKey TheKey, String sEncryptedText)

    {
        String TAG = "Crypto Error:";
        byte[] encodedBytes = null; //character array of encoded text
        byte[] decodedBytes = null; //character array of decode tet
        int len; //length of string
        int i; //index for "for" loop
        len = sEncryptedText.length();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            encodedBytes = sEncryptedText.getBytes(StandardCharsets.US_ASCII); //****might be wrong here
        }

        byte [] bTextBuffer = toByte(sEncryptedText);

        String sAlgorithm = new String();

        sAlgorithm = " ";

        try {
            Cipher c = Cipher.getInstance("RSA/None/PKCS1Padding"); //x+x+x+x+RSA/None/PKCS1Padding
            c.init(Cipher.DECRYPT_MODE, TheKey);

            sAlgorithm = c.getAlgorithm();

            decodedBytes = c.doFinal(bTextBuffer);
            //In order to convert Byte array into String format correctly,
            // we have to explicitly create a String object and assign the Byte array to it.
            String sDecodedText = new String(decodedBytes);
            return decodedBytes;
        } catch (Exception e)

        {
            //Log.e(TAG, "RSA decryption error");
            String nullString = "";
            return null; // (nullString);
        }
    }

    //====================================
    //
    //  Function: AsymmetricDecode()
    //  Paramters:
    //      TheyKey - Public Key from asymmetric key pair
    //      sEncryptedText - encrypted text to be decoded of type String
    //  Globals: nil
    //  Externals: nil
    //  Assumes:
    //  Purpose:
    //      This is special version of AsymmetricDecode() that uses the Public Key of the recipient
    //      rather than the private key. This is used to decode the signature part of the received message.
    //      The recipient can verify the assumed sender is the actually the sender by verifying the signature.
    //      Then sender calculates the HASH of the text of the message to be sent. The sender than encodes the
    //      HASH value their Private Key. The recipient decodes the whole message. They then calulaute the HASH
    //      of the text of teh message. The recipient then decodes the signature using teh Public Key of the assumed
    //      sender. The HASH calculate and the HASH contained in the decoded signature should match is the sender
    //      is who he says he is/
    //
    //*************************************

    public static byte[] AsymmetricDecodePublic (PublicKey TheKey, byte [ ] bEncryptedText)

    {
        String TAG = "Crypto Error:";
        byte[] encodedBytes = null; //character array of encoded text
        byte[] decodedBytes = null; //character array of decode tet
        int len; //length of string
        int i; //index for "for" loop
        // len = sEncryptedText.length();


        //encodedBytes = sEncryptedText.getBytes(StandardCharsets.US_ASCII); //****might be wrong here

        //byte [] bTextBuffer = toByte(sEncryptedText);

        String sAlgorithm = new String();

        sAlgorithm = " ";

        try {
            Cipher c = Cipher.getInstance("RSA/None/PKCS1Padding"); //x+x+x+x+RSA/None/PKCS1Padding
            c.init(Cipher.DECRYPT_MODE, TheKey);

            sAlgorithm = c.getAlgorithm();

            decodedBytes = c.doFinal(bEncryptedText);
            //In order to convert Byte array into String format correctly,
            // we have to explicitly create a String object and assign the Byte array to it.
            String sDecodedText = new String(decodedBytes);
            return decodedBytes;
        } catch (Exception e)

        {
            //Log.e(TAG, "RSA decryption error");
            String nullString = "";
            return null; // (nullString);
        }
    }



    //====================================
    //
    //  Function: HashMD5()
    //  Parameters:
    //      sText - text to be hashed of type String
    //  Globals:
    //  Externals:
    //  Returns:
    //  Purpose:
    //      Calculate the HASH of a function using the MD5 technique.
    //
    //      A HASH is a polynomial created by analyzing the text.
    //      This is one way process as the text is not encodes as opposed to used
    //      to calculate a unique value. That unique value that is used to verify
    //      the text.
    //
    //      Hashing to typically used to protect data without having access to the original date.
    //      For example you can caluclate the HASH of a pass word and then store the HASHED value
    //      in a database. If someone get unauthorized access to the database all they have is
    //      the HASH value not the actual pass word.
    //
    //      For the system to use the pass word it may receive. It calculates the HASH
    //      for the incoming pass word and then compares it to what is in the database.
    //
    //*************************************

    public static String HashMD5(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            String result = "";
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result += temp;
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    //========================================
    //
    //  Function: CreateSignature ()
    //  Parameters:
    //      PrivateKey - private key used to encrypted the HAASH value of the text to be ecnrypted
    //      sRawText - original text in a buff of type String
    //      sHashValue - result of this function which is the value of the HASH of the original text
    //  Globals:
    //  Externals:
    //  Returns:
    //          HASH value of type String - returns the HASH value fo teh text
    //          encrypted using the PRIVATE KEY of the sender.
    //  Purpose:
    //      This function calculates a digital signature that is sent as part of the text
    //      message. The HASH of the raw text is calculated. TThe HASH value is then encrypted
    //      with an asymmetric algorithm using the Private Key generated for the system ahead of time.
    //
    //      The hash value is added to the message between the <SIGNATURE> and <\SIGNATURE> delimiters.
    //      The whole message with the text to be sent if then encrypted with the PUBLIC KEY associated
    //      with the recipient.
    //
    //      The XML text should also contain the sender's USER_NAME bwtween the <FROM> and <\FROM> delimiters
    //      and recipient between the <TO> and <\TO> delimiters.
    //
    //      When the message is received by the recipient. He decodes the complete message using their
    //      PRIVATE KEY.
    //
    //      The text, sender USER_NAME, recipient USER_NAME and the signature part of the message is
    //      then extracted from the resulting <XML> document.
    //
    //      To verify the message came from the sender that has their user name in the message, the user
    //      takes the text part of the message and calculates the HASH using the pre-agreed upon method.
    //      In this case it is MD5.
    //
    //      The user then takes the SIGNATURE and decodes it using the PUBLIC KEY associated with the USER_NAME
    //      of the sender.  The decoded signature should match the HASH just calcualted.
    //

    //
    //
    //*********************************************

    public static byte[] CreateSignature(PrivateKey PrivateKey, String sRawText) {

        String sHashedText = "";
        byte[] bEncryptedHashedText = null;

        //create HASH of the raw text
        sHashedText = HashMD5(sRawText);

        //encrypted Hashed Text string using asymmetric encryption
        bEncryptedHashedText = AsymmetricEncrypt(PrivateKey, sHashedText);
        return bEncryptedHashedText;
    }

    //====================================
    //
    //  Function: VerifySignature()
    //  Parameters:
    //      PublicKey - Public Key of the sender in type String
    //      sDecodedText -  Text of the essae in type String
    //      sSignature - Digital Signature to verify in type String
    //  Globals: nil
    //  Externals: nil
    //  Returns:
    //          returns 0 if an error
    //          return 1 if no error
    //  Assumes:
    //      This function assumes the text part and the signature paort
    //      of the message has been extracted
    //      from teh XML format that is used to encapsulate the whole message which
    //      also contains the signature, and sender USER_NAME and recipient USER_NAME.
    //  Purpose:
    //      This function calculates the HASH using MD5 for the text message passed in
    //      as a parameter. It then takes the Digital Signature passed in as a parameter and
    //      decodes it using the asymmetric Public Key of the sender.
    //
    //      The decoded Digital Signature is then compared against the HASJ calculate for the text
    //      mesaage. If the two match then the message came from the sender whose USER_NAME is
    //      in the text mesage. If they do not match then the message is not from that sender or it has
    //      be alterred. The recipient should be notified there is a problem of with the identity of the
    //      sender.
    //
    //*************************************

    public static int VerifySignature(PublicKey PublicKey, String sDecodedText, byte [] bSignature) {

        String TAG = "Crypto Error:";
        String CurrentHash = "";

        byte[] bDecodedSignature = null;

        //calculate the HASH for the Decoded Text
        CurrentHash = HashMD5(sDecodedText);

        //decode the Digital Signature using the Public Key of the sender
        bDecodedSignature = AsymmetricDecodePublic(PublicKey, bSignature);

        String sDecodedSignature = new String(bDecodedSignature);
        //compare the HASH just calculated for the Decoded Text with the decoded Digital Signature
        if (!sDecodedSignature.equals(CurrentHash)) {
            //Log.e(TAG, "Signtures do not match");
            return 0;
        } else
            return 1;

    }


    //=====================================
    //
    //  Function: RemovePadding(bDecodedBuf)
    //  Parameters:
    //      bDecodedBuf -   character buffer of text with preceding padding of zeros to be removed
    //                      to extract the actual message
    //  Globals: nil
    //  Externals: nil
    //  Returns:
    //      A character buffer of type byte[] with the preceding zero padding removed
    //  Purpose:
    //      The RSA Asymmetric Encryption algorithm adds padding to the buffer when it is encrypted.
    //      The padding must be removed after decryption.
    //
    //**************************************

    public static byte [] RemovePadding(byte [] bTextBuf){

        byte [] bRawText = null;

        //remove the preceding padding from the decrypted char array

        return bRawText;
    }


    //=======================================
    //  Function: SaveKeyPair ()
    //  Parameters:
    //      ThePublicKey - Public Key from Asymmetric encryption pair to be save of type PublicKey
    //      ThePrivateKey - Private Key from Asymmetric encryption pair to be save of type PrivateKey
    //  Globals:
    //  Externals:
    //  Returns:
    //      0 if save failed
    //      1 if save succeeded
    //  Purpose:
    //      Save the KeyPair generated from an Asymmetric ALgorithm to a file and the system so
    //      it can be persistent.
    //
    //**************************************

    private void SaveKeyPair(PublicKey public_key, PrivateKey private_key){

        try {
            //pull out parameters that make key pair
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec RSAPubKeySpec = keyFactory.getKeySpec(public_key, RSAPublicKeySpec.class);
            RSAPrivateKeySpec RSAPrivKeySpec = keyFactory.getKeySpec(private_key, RSAPrivateKeySpec.class);

            //save public and private keys to files
            saveKeys(PUBLIC_KEY_FILE, RSAPubKeySpec.getModulus(), RSAPubKeySpec.getPublicExponent());
            saveKeys(PRIVATE_KEY_FILE, RSAPrivKeySpec.getModulus(), RSAPrivKeySpec.getPrivateExponent());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //====================================
    //
    //  Function: savekeys()
    //  Parameters:
    //  Globals:
    //  Externals:
    //  Assumes:
    //  Returns:
    //  Purpose:
    //      Save a variable of type PublicKey or PrivateKey to a file.
    //
    //      Public and Private keys are made up of a modulus and an exponents.
    //      These elements need to be extract from the PublicKey and PrivateKey class
    //      and the save in a format that can be easily read back.
    //
    //      Because char in Android in 16 bits it can mess up the data format if you
    //      do not convert before you save it to a file.
    //
    //*******************************************

    private void saveKeys (String file_name, BigInteger mod, BigInteger exp ) throws IOException {

        FileOutputStream fos = null;
        ObjectOutputStream oos = null;

        try {
            fos = new FileOutputStream (file_name);
            oos = new ObjectOutputStream(new BufferedOutputStream(fos));
            oos.writeObject(mod);
            oos.writeObject(exp);

        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally {
            if(oos != null){
                oos.close();
                if(fos != null){
                    fos.close();
                }
            }
        }
    } //saveKeys()

    //==================================
    //
    //  Function: readPublicKeyFromFile()
    //  Parameters:
    //          file_name - name of file to read of type String
    //  Globals:
    //  Externals:
    //  Assumes
    //  Returns:
    //  Purpose:
    //      Open the file lablled "file_name" and reads back the modulis and exponenet of the PublicKey.
    //
    //************************************

    public PublicKey readPublicKeyFromFile(String file_name){

        FileInputStream fis = null;
        ObjectInputStream ois = null;
        PublicKey public_key = null;

        try {
            fis = new FileInputStream(new File(file_name));
            ois = new ObjectInputStream(fis);

            BigInteger Modulus = (BigInteger) ois.readObject();
            BigInteger Exponent = (BigInteger) ois.readObject();

            //Get Public Key
            RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(Modulus, Exponent);
            KeyFactory fact = KeyFactory.getInstance("RSA");
            public_key = fact.generatePublic(rsaPublicKeySpec);
            return public_key;
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        catch(ClassNotFoundException e){
            e.printStackTrace();
        }
        catch(NoSuchAlgorithmException e ){
            e.printStackTrace();
        }
        finally{
            if(ois != null) {
                try {
                    ois.close();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
                if(fis != null){
                    try {
                        fis.close();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
            return public_key;
        }

    }

    //==================================
    //
    //  Function: readPrivateKeyFromFile()
    //  Parameters:
    //      file_name - name of file to read PrivateKey from of type String
    //  Globals:
    //  Externals:
    //  Assumes:
    //  Returns:
    //  Purpose:
    //      Open the file referred to in "file_name" and reads in the modulus
    //      and exponent of the PrivateKey.
    //
    //************************************

    public PrivateKey readPrivateKeyFromFile(String file_name){

        FileInputStream fis = null;
        ObjectInputStream ois = null;
        PrivateKey private_key = null;

        try {
            fis = new FileInputStream(new File(file_name));
            ois = new ObjectInputStream(fis);

            BigInteger Modulus = (BigInteger) ois.readObject();
            BigInteger Exponent = (BigInteger) ois.readObject();

            //Get Public Key
            RSAPrivateKeySpec rsaPrivateKeySpec = new RSAPrivateKeySpec(Modulus, Exponent);
            KeyFactory fact = KeyFactory.getInstance("RSA");
            private_key = fact.generatePrivate(rsaPrivateKeySpec);
            return private_key;
        }
        catch(IOException e)  {
            e.printStackTrace();
        }
        catch( ClassNotFoundException e){
            e.printStackTrace();
        }
        catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        finally{
            if(ois != null) {
                try {
                    ois.close();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                if(fis != null){
                    try {
                        fis.close();
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
            return private_key;
        }

    }

} // end of Crypto Class definition