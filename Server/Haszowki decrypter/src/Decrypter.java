import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Created by morri on 08.10.2016.
 */


public class Decrypter {

    private static String VERSION_NUMBER = "0.7.3";
    private static String cryptoPass = "my simple aes key";

    /*
    private static List<String> argsList = new ArrayList<>();
    private static List<Option> optsList = new ArrayList<>();
    private static List<String> doubleOptsList = new ArrayList<>();
*/

    private static boolean flagVersion = false;
    private static boolean flagInput = false;
    private static String inputFileName = "";
    private static boolean flagOutput = false;
    private static String outputFileName = "";

    private static String decryptIt(String text) {

        StringBuilder buildCrypto = new StringBuilder(cryptoPass);
        buildCrypto.replace(8, 9, "#");
        buildCrypto.replace(13, 15, "78");
        buildCrypto.replace(3, 4, "+");
        String enrichedCryptoPass = buildCrypto.toString();
        System.err.println(enrichedCryptoPass);
        String decrypedText;

        try {
            DESKeySpec keySpec = new DESKeySpec(enrichedCryptoPass.getBytes("UTF8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            byte[] encrypedPwdBytes = Base64.getDecoder().decode(text);
            // cipher is not thread safe
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypedValueBytes = (cipher.doFinal(encrypedPwdBytes));

            decrypedText = new String(decrypedValueBytes);

        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return "";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            return "";
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        } catch (BadPaddingException e) {
            e.printStackTrace();
            return "";
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            return "";
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
            return "";
        }

        return decrypedText;
    }

    static String privateKeyString = "MIIJJgIBAAKCAgBb5xkCfWc2eNXcXFhR3r2e2SAtyJsT1vXlP9Fu5d7W7Mf/uDTs" +
            "wf2ZWZsLu8wXX8RwTTZ4+yn8tHClUr9E8oVtbnJvut7z0/cPrrvMc1Al5VMrwl1N" +
            "BznEI3Ih+Dl2+c7c9WPlYOJaI8cqnSLPSLzD2WJ1h4xZo6sLB7fPj1zJIARwFQPj" +
            "as7bX0vO/MzJXHAP0CxmajVF69mcDUHVtbeAyPOMddv1Zw4twK+YS6gGjjiSXg73" +
            "PA9oLrPJCBiuy7P0PFbXArzQIY/Q65kOaOgbBGGiWDSklobxaqdu2kZBMQGVTJop" +
            "02ydkbOc1S6y/hSHCkpldVFeJOTm6shiAW6xxxOV0UilXSvZup+CwyLsaGWSP/3L" +
            "nHooAtp7ANixRBe258hJPUBEHfRgjHyjk6vSqdKHuCgeO3pMIQB8/UbjDaMAgY0U" +
            "8C16GcnpqIBQcsKmF9ng76+7Qs8TCQJY8VDTmneUHL8bi4OCR3g6udbvZ3qrWzJ1" +
            "B03fe794ktMEDtlpBFKnl5WQiySrBzrilb14hkyk2VWpty9pQ9YXbKV0iqWJJyE7" +
            "fFlttHJ7sbR1MIeLjeBYDbOMm0w/r1wReC1tbKJuaTgyz5sLO+T4iW+luDBo7xHF" +
            "eqLm5wx39+6ix7vYxnZqstE6c9/GdkffYzSSFJArmjxXs2wmA8wnxRT4gwIDAQAB" +
            "AoICAAXsuxcHAJ1pYtgm9+anRnA0LTfmY+D+jbGu0JCmrxwJ/cbFmFvfEbtOJIm4" +
            "HKsxGFfpEmbwQj+xXkW6NOx7+hAY+7WqRW9Qre/L4v2GPZeD1j3O9PbfTWEQq+32" +
            "s7Ww2x4xj7Qc79rBzbg4kyLr3Id/vzI2f9zTiVZXtAjkhCXPM5oKMMr7esR3u0pn" +
            "z8f3dp3+XK5pkG+micvequzdHyxlSBY5DuoeL7LRZkCaOAXcK1d8StizfYbI4/xE" +
            "0lqKdVp8fVi1K3j7gOsGFULxjm0XdjfiGdq2fZKYvpyN49OWFjUK0DF8GNd7qiml" +
            "MLKHYMln523tB0bbeApO/oYa84jctK1H7Ot9n46XJ+sd8J18qG8Z4ybmLSfTQcf6" +
            "zxKjHDO9Cp6ywO6jfO4+w0vAvyWaFIC6wBoUsa5ut++kQWtyo35ovICw88/bio1s" +
            "WGSqRJzTB4djbF5TBFoeKLF08j2um6HOG/b6L1Tae19hbVuPbCF4eMgXx4qWbGU/" +
            "sZhNJaXc0I5IPKUphyECmNkH36GkbLahHoDujw/PeNHuO5h1YYnpFE8z0LmoYKPU" +
            "LNzkLje717SvJA2ZGsvDHGRCwKFc1nJRFi6J0P+DXwjL6ghUcAbDYK6wKZ5KQlt3" +
            "Dc2mKaerlbnyueXIIc+MUjkLf7VrEwTBZoAxSyYCSP25NmORAoIBAQCd2TiuzA3q" +
            "JuUpTS68SjltNdL6wf8JQg0EEoFNAeiz0eXjK72rmqSRd4U+NG1/680n0x3MdgEk" +
            "wRWFI8LOjMTMTD1ItsuRK1CTj8zz0icuJCswrCfMg8TBPFsF1yYI/hcbYAJ9O0qj" +
            "fe+iyO3LZl7tZGf7Qz1Daf4tXvvgGRIONbzdU31NBdVAAN2y3U9rp20t3HFXp25y" +
            "AWop9e0mWQPwx0TfN7Qntn/ypTneifFr4uKRoz28+ijVo3op+Ka7Z5x1SB+1mGrM" +
            "oX5jD5Q7STNHxupqw40r/110xZxmifrXxO1sM98xLWA7tTEmAx67iOBGBPlbdly0" +
            "tOwIFAMncK4pAoIBAQCVDG+jp/M2fD9a8KNKDxWrJk83hkd+6ulYexH2yidB4HSp" +
            "VrLezXgEfERUbf09UnNYyfu/5e4FeCFRIVsaPklkm1l5vHLZUAi5CeIeqpXt9MHR" +
            "KZPgWpK/swGKLbTWhwyrH4q7OCG6A5IrfNYAgwusVhKfW9MWnrkDyh5KdR0JyrFl" +
            "Ufz7MNIgSKt5stmWv13ttvgThEyI+3DMpMLH31U3CxZBHNUu8+lEW+Clnqo9wmpz" +
            "dOA3mDOIEv81wvKTRiR+bFq6I0m9FH5X3HiLyEXQq7Vy8waCm6Hu0GrnLL9y8gWD" +
            "f7XVJRBTdRHh0uDoGDgm3Cg44WR/ZFGWeHXq467LAoIBAD7Qkp381gy4Lbmh3VdQ" +
            "skmjgbIIQVWN02ArfQkIGXJ1tOYSIgiIIbVBuuRmOK0PSTTv7ovO6eWWcNnqwTsx" +
            "CZ/DNyAYninG8unF7+mXV8Ak5IsZ9zyLs2CyhAZu99PcSZW7P0JWtf0ZwKMnEno+" +
            "4sfVjQuQVnDdXSjxA4rKb0T4XZA2CUb9az9tGMx1BYXxuqDleLVJC8qShYztMNJx" +
            "2f+XTPEHWcnz9ja5Sa4lds1YHJGYRJlPc7CQvay2JqOtN7X0XaoGXXnRSlpheLuf" +
            "Bakqn16dMzCvDqHJgdPMVOZIl7LXcZpAVGtuT4Cw/Snj7lvu3sxm7b17wfH1BMxN" +
            "KwECggEAGQ9lOeQELZYIZPbuzYXpw8QGL7TBEqLWpwzSQWdN4HKnys0L+BAd7Msk" +
            "BfoUSRoy0KvtSx+SvJKtL2HnWms8ldDU43X+7XDadpolzbgqyz6K0+sktOUlpVuo" +
            "l54FuMguJhuAjOfsK8Vr7ynnJWDjNo+mQ+sBe90mCHAUVbqJLltJJlr5qRZVTh5J" +
            "zoV2tjToyw4neciVwbZdCdtt8IMpZb7UeBAr++AAyYCVLeOWhhnJIi51gINzrp5b" +
            "EKP9eyug+SyouIE0ZbkrYQRttDrxGhu0v2YDIzSdrnSWdNX+PopYyPpRDUxVCWM2" +
            "pXx6WiuwTUBY9u9WoWCxoxYP5XVwrwKCAQBjY93rue7nx7LfdWpyMsd9xR+JLApJ" +
            "OOBGWf01ae3wCjyjJOldoSee9YTZ53PVm94sOUGaB6R24HWjo4U3D45S2Wci9KUn" +
            "Vsh39uTvbb4tPF/UgZPf/sq0pBGWaTieQ9cZaGiAealBFPvxmO39VidSfgfwMMZW" +
            "phG1NXphS9Ze9WysQ4qFECjmHqclmvkddihSUjfyCBNgpTYTpt5TKDYIDBgZND4V" +
            "cuKst1s76mlm5J7Z5xoHR/k9R6/DrAOOaCEQFmuhoOgW6hhNhrZ4CvnBGr6mB+Mx" +
            "0Qx53ukTfEMyWpx2ceAmomsyHlLycGttKvnrhAP7j1RWFIXjzo/7Q5jo";

    private static String decryptItRsa(String text) {

        String cipheredText;

        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyString.getBytes());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            PrivateKey key = keyFactory.generatePrivate(spec);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, key);

            cipheredText = new String(cipher.doFinal(text.getBytes("ISO-8859-1")), "ISO-8859-1");
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return "";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            return "";
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
            return "";
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        } catch (BadPaddingException e) {
            e.printStackTrace();
            return "";
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            return "";
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
            return "";
        }

        return cipheredText;
    }

    private static List<String> readFile(String filename) {
        List<String> records = new ArrayList<String>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = reader.readLine()) != null) {
                records.add(line);
            }
            reader.close();
            return records;
        } catch (Exception e) {
            System.err.format("Exception occurred trying to read '%s'.", filename);
            e.printStackTrace();
            return null;
        }
    }

    private static List<String> reformatInput(List<String> input) {
        List<String> assembledText = new ArrayList<String>();

        for (int i = 2; i < input.size(); ++i) {
            StringBuilder line = new StringBuilder();
            while (!input.get(i).equals("")) {
                line.append(input.get(i));
                ++i;
            }
            assembledText.add(line.toString());
        }

        return assembledText;
    }

    private static List<String> runDecrypter(List<String> cipheredLines) {
        List<String> decodedText = new ArrayList<String>();

        for (int i = 0; i < cipheredLines.size(); ++i) {
            decodedText.add(decryptItRsa(cipheredLines.get(i)));
        }

        return decodedText;
    }

    private static boolean writeDecodedToFile(List<String> decodedText) {

        try {
            PrintWriter writer = new PrintWriter(outputFileName, "UTF-8");

            for (int i = 0; i < decodedText.size(); ++i) {
                writer.println(decodedText.get(i));
                writer.flush();
            }
            writer.close();

        } catch (FileNotFoundException e) {
            System.err.println("File not found");
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            System.err.println("Unsupported encoding");
            e.printStackTrace();
        }

        return true;
    }

    private static boolean writeDecodedToOutput(List<String> decodedText) {

        try {
            OutputStreamWriter writer = new OutputStreamWriter(System.out);

            for (int i = 0; i < decodedText.size(); ++i) {
                writer.write(decodedText.get(i));
                writer.write(System.getProperty("line.separator"));
                writer.flush();
            }
            writer.close();

        } catch (FileNotFoundException e) {
            System.err.println("File not found");
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            System.err.println("Unsupported encoding");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO error");
            e.printStackTrace();
        }

        return true;
    }

    /*
    private static class Option {
        String flag, opt;

        public Option(String flag, String opt) {
            this.flag = flag;
            this.opt = opt;
        }
    }
    */

    private static void decodeArguments(String[] args) throws IllegalArgumentException {

        for (int i = 0; i < args.length; i++) {
            switch (args[i].charAt(0)) {
                case '-':
                    /*
                    if (args[i].length() < 2)
                        throw new IllegalArgumentException("Not a valid argument: " + args[i]);
                    if (args[i].charAt(1) == '-') {
                        if (args[i].length() < 3)
                            throw new IllegalArgumentException("Not a valid argument: " + args[i]);
                        // --opt
                        doubleOptsList.add(args[i].substring(2, args[i].length()));
                    } else {
                        if (args.length - 1 == i)
                            throw new IllegalArgumentException("Expected arg after: " + args[i]);
                        // -opt
                        optsList.add(new Option(args[i], args[i + 1]));
                        i++;
                    }
                    */

                    if (args[i].length() == 2) {
                        switch (args[i].charAt(1)) {
                            case 'v':
                                flagVersion = true;
                                ++i;
                                break;

                            case 'o':
                                flagOutput = true;
                                ++i;
                                if (i < args.length)
                                    outputFileName = args[i];
                                else {
                                    flagOutput = false;
                                    throw new IllegalArgumentException("Give output file name");
                                }
                                break;

                            case 'i':
                                flagInput = true;
                                ++i;
                                if (i < args.length)
                                    inputFileName = args[i];
                                else {
                                    flagInput = false;
                                    throw new IllegalArgumentException("Give input file name");
                                }
                                break;

                            default:
                                throw new IllegalArgumentException("Not recognised options");
                        }
                    } else
                        throw new IllegalArgumentException("Not recognised options");

                    break;

                default:
                    // arg
                    //argsList.add(args[i]);
                    throw new IllegalArgumentException("Not recognised options");
                    //break;
            }

        }

    }

    private static boolean parseArguments(String[] args) {

        try {
            decodeArguments(args);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return false;
        }

        return true;
    }

    public static void main(String[] args) {


        parseArguments(args);

        if (flagVersion) {
            System.out.println("Version " + VERSION_NUMBER);
            return;
        }

        if (flagInput) {
            List<String> input;

            input = readFile(inputFileName);

            //input = reformatInput(input);

            input = runDecrypter(input);

            if (flagOutput) {
                writeDecodedToFile(input);
            } else {
                writeDecodedToOutput(input);
            }
        }
    }
}

