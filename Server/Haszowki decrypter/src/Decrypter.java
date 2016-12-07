import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by morri on 08.10.2016.
 * <p>
 * This class is used to decode files with RSA encrypted data.
 */

@SuppressWarnings("FieldCanBeLocal")
public class Decrypter {

    private static String VERSION_NUMBER = "0.9.1";

    private static boolean flagVersion = false;
    private static boolean flagInput = false;
    private static String inputFileName = "";
    private static boolean flagOutput = false;
    private static String outputFileName = "";
    private static boolean flagConfigFile = false;
    private static String configFileName = "";

    private static String privateKeyString = "";

    /**
     * Used to read private key from config file.
     * Config file consists only private key. Nothing else.
     *
     * @param configFileName name of config file
     * @return true if reading was correct, false if something went wrong
     */
    private static boolean readConfigFile(String configFileName) {

        try {
            privateKeyString = new Scanner(new FileInputStream(configFileName + ".privKey"), "UTF-8").useDelimiter("\\Z").next();

            return true;
        } catch (IOException e) {
            System.err.println("Problem with opening config file");
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //Here BouncyCastle library is being added to handle ciphering with RSA keys
    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    /**
     * Method that decrypts answer and returns it.
     * Input string is decoded using base64.
     * Then it is deciphered using RSA decryption.
     * Afterwards it is encoded to UTF-8 and returned.
     *
     * @param text base64 encoded, ciphered answer
     * @return UTF-8 encoded, deciphered answer
     */
    private static String decryptItRsa(String text) {

        String cipheredText;

        try {
            byte[] keyBytes = DatatypeConverter.parseBase64Binary(privateKeyString);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            PrivateKey key = keyFactory.generatePrivate(spec);
            Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);

            //version with base64 decoding
            byte[] decodedText = DatatypeConverter.parseBase64Binary(text);
            cipheredText = new String(cipher.doFinal(decodedText), "UTF-8");

            //version with ISO-8859-1 decoding
            //cipheredText = new String(cipher.doFinal(text.getBytes("ISO-8859-1")), "UTF-8");

        } catch (InvalidKeyException e) {
            System.err.println("Invalid key");
            e.printStackTrace();
            return "";
        } catch (InvalidKeySpecException e) {
            System.err.println("Invalid key spec");
            e.printStackTrace();
            return "";
        } catch (NoSuchProviderException e) {
            System.err.println("BouncyCastle provider not found");
            e.printStackTrace();
            return "";
        } catch (UnsupportedEncodingException e) {
            System.err.println("Text encoding is not supported");
            e.printStackTrace();
            return "";
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Unknown algorithm");
            e.printStackTrace();
            return "";
        } catch (BadPaddingException e) {
            System.err.println("Bad padding in decrypted text");
            e.printStackTrace();
            return "";
        } catch (NoSuchPaddingException e) {
            System.err.println("Unknown padding in decrypted text");
            e.printStackTrace();
            return "";
        } catch (IllegalBlockSizeException e) {
            System.err.println("Length of text to be decrypted is not valid");
            e.printStackTrace();
            return "";
        }

        return cipheredText;
    }

    /**
     * Method that reads formatted file and converts it into list of strings.
     *
     * @param filename name of file from which ciphered answers will be read.
     * @return list of strings containing header and answers in file
     */
    private static List<String> readFile(String filename) {
        List<String> records = new ArrayList<>();
        try {
            Scanner scan = new Scanner(new FileInputStream(filename));
            scan.useDelimiter(Pattern.compile("\n====\n"));
            while (scan.hasNext()) {
                String logicalLine = scan.next();
                records.add(logicalLine);
            }

            return records;
        } catch (Exception e) {
            System.err.format("Exception occurred trying to read '%s' in readFile()", filename);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method that reads standard input and saves it into list of strings.
     *
     * @return list of strings containing header and answers in file
     */
    private static List<String> readInput() {

        List<String> records = new ArrayList<>();
        try {
            Scanner scan = new Scanner(new BufferedInputStream(System.in));
            scan.useDelimiter(Pattern.compile("\n====\n"));
            while (scan.hasNext()) {
                String logicalLine = scan.next();
                records.add(logicalLine);
            }

            return records;
        } catch (Exception e) {
            System.err.format("Exception occurred trying to read input in readInput()");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method that runs decryption for list of strings.
     * It reads header when config file name was not specified,
     * and loads config file according to read value.
     *
     * @param cipheredLines list of strings containing header and answers
     * @return decoded text
     */
    private static List<String> runDecrypter(List<String> cipheredLines) {
        List<String> decodedText = new ArrayList<>();

        try {
            //If there was no config file set, we try to read config from header
            if (!flagConfigFile) readConfigFile(readHeader(cipheredLines.get(0)));
        } catch (IllegalArgumentException e) {
            System.err.println("Problem with reading config file name from header");
            e.printStackTrace();
            return decodedText;
        }

        for (int i = 1; i < cipheredLines.size(); ++i) {
            decodedText.add(decryptItRsa(cipheredLines.get(i)));
        }

        return decodedText;
    }

    /**
     * Method that is parsing header.
     * It seeks for the name of test in config file:
     * test_id=[name of test]
     *
     * @return name of test as string
     * @throws IllegalArgumentException that informs about invalid header
     */
    private static String readHeader(String header) throws IllegalArgumentException {

        Pattern pattern = Pattern.compile("test_id=([-_a-zA-Z0-9]+)");
        Matcher matcher = pattern.matcher(header);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Writes decoded answers to file.
     *
     * @param decodedText list with decoded answers
     * @return true if everything went well, false if something went wrong
     */
    private static boolean writeDecodedToFile(List<String> decodedText) {

        try {
            Path pathToFile = Paths.get(outputFileName);
            if (pathToFile.getParent() != null) {
                Files.createDirectories(pathToFile.getParent());
                Files.createFile(pathToFile);
            }
            PrintWriter writer = new PrintWriter(outputFileName, "UTF-8");

            for (String dtIter : decodedText) {
                writer.println(dtIter);
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
            System.err.println("Problem with file");
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Writes decoded answers to standard output.
     *
     * @param decodedText list with decoded answers
     * @return true if everything went well, false if something went wrong
     */
    private static boolean writeDecodedToOutput(List<String> decodedText) {

        try {
            OutputStreamWriter writer = new OutputStreamWriter(System.out);

            for (String dtIter : decodedText) {
                writer.write(dtIter);
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

    /**
     * Decodes command line parameters and sets up related flags.
     *
     * @param args array containing command line parameters
     * @throws IllegalArgumentException that says what went wrong with decoding command line parameters
     */
    private static void decodeArguments(String[] args) throws IllegalArgumentException {

        for (int i = 0; i < args.length; i++) {
            switch (args[i].charAt(0)) {
                case '-':

                    if (args[i].length() == 2) {
                        switch (args[i].charAt(1)) {
                            case 'v':
                                flagVersion = true;
                                ++i;
                                break;

                            case 'o':
                                flagOutput = true;
                                ++i;
                                if (i < args.length && args[i].length() > 2)
                                    outputFileName = args[i];
                                else {
                                    flagOutput = false;
                                    throw new IllegalArgumentException("Give output file name");
                                }
                                break;

                            case 'i':
                                flagInput = true;
                                ++i;
                                if (i < args.length && args[i].length() > 2)
                                    inputFileName = args[i];
                                else {
                                    flagInput = false;
                                    throw new IllegalArgumentException("Give input file name");
                                }
                                break;

                            case 'c':
                                flagConfigFile = true;
                                ++i;
                                if (i < args.length && args[i].length() > 2)
                                    configFileName = args[i];
                                else {
                                    flagConfigFile = false;
                                    throw new IllegalArgumentException("Give input config file name");
                                }
                                break;

                            default:
                                throw new IllegalArgumentException("Not recognised options");
                        }
                    } else
                        throw new IllegalArgumentException("Not recognised options");

                    break;

                default:
                    throw new IllegalArgumentException("Not recognised options");
            }
        }
    }

    /**
     * Used to run decode of command line parameters.
     * Made to handle exceptions that are thrown by decodeArguments().
     *
     * @param args array containing command line parameters
     * @return true went well, false if something went wrong
     */
    private static boolean parseArguments(String[] args) {

        try {
            decodeArguments(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Main method of class.
     *
     * @param args array containing command line parameters
     */
    public static void main(String[] args) {


        parseArguments(args);

        if (flagVersion) {
            System.out.println("Version " + VERSION_NUMBER);
            return;
        }

        if (flagInput) { //If there is input file we read from it.

            //If config file was set then we read config from it.
            if (flagConfigFile) readConfigFile(configFileName);

            List<String> input;
            input = readFile(inputFileName);
            input = runDecrypter(input);

            if (flagOutput) {
                writeDecodedToFile(input);
            } else {
                writeDecodedToOutput(input);
            }
        } else { //If there is no input file then we read from console

            //If config file was set then we read config from it.
            if (flagConfigFile) readConfigFile(configFileName);

            List<String> input;
            input = readInput();
            input = runDecrypter(input);

            if (flagOutput) {
                writeDecodedToFile(input);
            } else {
                writeDecodedToOutput(input);
            }
        }
    }
}

