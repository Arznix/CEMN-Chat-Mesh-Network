/**
 * Created by Harry Lew on 18/09/2017.
 */

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

//import javax.crypto; // This package provides the classes and interfaces for cryptographic applications implementing algorithms for encryption, decryption, or key agreement.
//import javax.crypto.interfaces; //This package provides the interfaces needed to implement the key agreement algorithm.
//import javax.crypto.spec; //This package provides the classes and interfaces needed to specify keys and parameter for encryption.



public class Crypto {

    private static String SALT = "some_salt_to_change!";    //This is some artibtrary and randomly generated string that is used to SALT
                                                            //SALT the encryption algoritm to make it more difficult to break
    private final static String HEX = "0123456789ABCDEF";

    public String TAG = "Crypto Errors:";

    //===========================================
    //
    // Function: public void ListSupportedAlgorithms()
    // Paramenters: String results
    // Globals: none
    // Externals: n
    // Returns: void
    // Purpose: This function returns a list of the cryptography
    //          algorithms that can be supported on the device.
    //          The Android Cryptograph libarary suppports
    //          an order version of Bouncy Castle, an open source
    //          cryptograph library - http://www.bouncycastle.org/
    //
    //*******************************************

    public void ListSupportedAlgorithms(String results) {
        String result = "";

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
                    result += ("[P#" + (p + 1) + ":" + providers[p].getName() + "]" +
                            "[S#" + s + ":" + stype + "]" +
                            "[A#" + a + ":" + ita.next() + "]\n");
                    a++;
                } //for

                s++;
            } //for
        } //for

        results = result; //to export the results out of this function

    } //end of function definition ListSupportedAlgorithms()

    //========================================
    //
    //  This next section of codee implements Symmetric Encryption.
    //
    //  The function implement the AES 256 method of encoding and decoding text data.
    //========================================


    //=======================================
    //  Function: CreateSymmetryKey()
    //  Parameters:
    //              String Seed - seed (randomized) string to feed into the encryption
    //              SecretKey Key - symmetry key that is returned for the function
    //  Globals: NIL
    //  Externals: NIL
    //  Returns:
    //  Purpose:
    //  This function takes "seed", essentially a randomized string to help start the encryption process
    //
    //
    //*****************************************

    public static SecretKey CreateSymmetryKey(String seed, SecretKey key)
        throws Exception {
        key = generateSymmetricKey();   //this ithe short form, a more elaborate form exist
                                        //refer to the descript of generaeSymmetricKey()
        return key;
    }

    public static String encrypt(String cleantext, SecretKey AESKey )
            throws Exception {
        byte[] rawKey = AESKey.getEncoded();
        byte[] result = encryptSymmetric(rawKey, cleantext.getBytes());
        return toHex(result);
    }

    public static String decrypt(String encrypted, SecretKey AESKey)
            throws Exception {
        byte[] rawKey = AESKey.getEncoded();
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


    public static SecretKey generateSymmetricKey() throws NoSuchAlgorithmException {
        // Generate a 256-bit key
        final int outputKeyLength = 256; //256 bit is the high value for AES currently in common use

        SecureRandom secureRandom = new SecureRandom();
        // Do *not* seed secureRandom! Automatically seeded from system entropy if the SecureRandom(0
        //call has no parameter.
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(outputKeyLength, secureRandom);
        SecretKey key = keyGenerator.generateKey();
        return key;
    }

    //This is for symmetric AES encryption
    private static byte[] encryptSymmetric(byte[] raw, byte[] clear) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(clear);
        return encrypted;
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

    private static byte[] decryptSymmetric(byte[] symmetric_key, byte[] encrypted_text)
            throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(symmetric_key, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] decrypted = cipher.doFinal(encrypted_text);
        return decrypted;
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
    //  This functions implements either the RSA 1024 and RSA 2048 encryotion.
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
    //      The RSA 1024 or 2048 method of ecryption is used. 1024 and 2048 refers
    //      to the number of bits to be used in the encryption key. RSA 2048 will
    //      take longer than RSA 1024 to generate. If there is too much of the delay
    //      in the encryption process the user will experience will be degraded
    //      as the user will wonder why it is taking so long to send and receive a message.
    //
    //**************************************

    // Generate key pair for 1024-bit RSA encryption and decryption

    public static int generateAsymmetricKeys(Key MyPublicKey, Key MyPrivateKey ) {

        String TAG = "Crypto Error:";

        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);  //1024 bit or 2048 bits
            KeyPair kp = kpg.genKeyPair();
            MyPublicKey = kp.getPublic();
            MyPrivateKey = kp.getPrivate();
            return 1;
        } catch (Exception e) {
            Log.e(TAG, "RSA key pair error");
            return 0;
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

    public static int AsymmetricEncrypt( Key TheKey, String sTextBuf, String sEncryptedText) {

        String TAG = "Crypto Error:";

        // Encode the original data with RSA private key
        byte[] encodedBytes = null;
        try {
            Cipher c = Cipher.getInstance("RSA");
            c.init(Cipher.ENCRYPT_MODE, TheKey);
            encodedBytes = c.doFinal(sTextBuf.getBytes());
            } catch (Exception e) {
            Log.e(TAG, "RSA encryption error");
            return 0;
        }

        sEncryptedText = "[ENCODED]:\n";
        sEncryptedText += Base64.encodeToString(encodedBytes, Base64.DEFAULT);
        sEncryptedText += "\n";

        return 1;
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
    //***********************************

    public static String AsymmetricDecode(Key TheKey, String sEncryptedText)

    {
        String TAG = "Crypto Error:";
        byte[] encodedBytes = null; //character array of encoded text
        byte[] decodedBytes = null; //character array of decode tet
        int len; //length of string
        int i; //index for "for" loop
        len = sEncryptedText.length();


        encodedBytes = sEncryptedText.getBytes();



        try {
            Cipher c = Cipher.getInstance("RSA");
            c.init(Cipher.DECRYPT_MODE, TheKey);
            decodedBytes = c.doFinal(encodedBytes);
            //In order to convert Byte array into String format correctly,
            // we have to explicitly create a String object and assign the Byte array to it.
            String sDecodedText = new String(decodedBytes);
            return sDecodedText;
        } catch (Exception e)

        {
            Log.e(TAG, "RSA decryption error");
            String nullString = "";
            return  (nullString);
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

    public static String CreateSignature(Key PrivateKey, String sRawText){

        String HashedText = "";
        String sEncryptedText = "";

        //create HASH of the raw text
        HashedText = HashMD5(sRawText);

        //encrypted Hashed Text string using asymmetric encryption
        AsymmetricEncrypt( PrivateKey, sRawText, sEncryptedText);
        return sEncryptedText;
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

    public static int VerifySignature(Key PublicKey, String sDecodedText, String sSignature){

        String TAG = "Crypto Error:";
        String CurrentHash = "";
        String DecodedSignature = "";

        //calculate the HASH for the Decoded Text
        CurrentHash = HashMD5(sDecodedText);

        //decode the Digital Signature using the Public Key of the sender
        DecodedSignature = AsymmetricDecode(PublicKey, sSignature);

        //compare the HASH just calculated for the Decoded Text with the decoded Digital Signature
        if(!DecodedSignature.equals(CurrentHash))
        {
            Log.e(TAG, "Signtures do not match");
            return 0;
        }
        else
            return 1;

    }
} //end of class definition
