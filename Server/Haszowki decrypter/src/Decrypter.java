import java.io.*;
import java.util.Base64;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

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
		buildCryptoNano.replace(13, 15, "78");
		buildCrypto.replace(3, 4, "+");
		enrichedCryptoPass = buildCrypto.toString();

        try {
            DESKeySpec keySpec = new DESKeySpec(enrichedCryptoPass.getBytes("UTF8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            byte[] encrypedPwdBytes = Base64.getDecoder().decode(text);
            // cipher is not thread safe
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypedValueBytes = (cipher.doFinal(encrypedPwdBytes));

            String decrypedValue = new String(decrypedValueBytes);
            //System.err.println(decrypedValue);
            return decrypedValue;

        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return text;
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
            decodedText.add(decryptIt(cipheredLines.get(i)));
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

        if (flagInput){
            List<String> input;

            input = readFile(inputFileName);

            input = reformatInput(input);

            input = runDecrypter(input);

            if (flagOutput){
                writeDecodedToFile(input);
            } else {
		writeDecodedToOutput(input);
	    }
        }
    }
}

