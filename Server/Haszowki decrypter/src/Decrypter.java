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
import java.util.regex.Pattern;

/**
 * Created by morri on 08.10.2016.
 */


public class Decrypter {

    private static String VERSION_NUMBER = "0.7.9";

    private static boolean flagVersion = false;
    private static boolean flagInput = false;
    private static String inputFileName = "";
    private static boolean flagOutput = false;
    private static String outputFileName = "";
    private static boolean flagConfigFile = false;
    private static String configFileName = "";

    private static String privateKeyString = "";

    private static boolean readConfigFile(String configFileName) {

        try {
            privateKeyString = new Scanner(new FileInputStream(configFileName + ".privKey")).useDelimiter("\\Z").next();

            return true;
        } catch (IOException e) {
            System.err.println("Opening file problem");
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    private static String decryptItRsa(String text) {

        String cipheredText;

        try {
            byte[] keyBytes = DatatypeConverter.parseBase64Binary(privateKeyString);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            PrivateKey key = keyFactory.generatePrivate(spec);
            Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding");
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
        } catch (ClassCastException e) {
            e.printStackTrace();
            return "";
        }

        return cipheredText;
    }

    private static List<String> readFile(String filename) {
        List<String> records = new ArrayList<String>();
        try {
            Scanner scan = new Scanner(new FileInputStream(filename));
            scan.useDelimiter(Pattern.compile("\n====\n"));
            while (scan.hasNext()) {
                String logicalLine = scan.next();
                records.add(logicalLine);
            }

            return records;
        } catch (Exception e) {
            System.err.format("Exception occurred trying to read '%s'.", filename);
            e.printStackTrace();
            return null;
        }
    }

    private static List<String> runDecrypter(List<String> cipheredLines) {
        List<String> decodedText = new ArrayList<String>();

        for (int i = 1; i < cipheredLines.size(); ++i) {
            decodedText.add(decryptItRsa(cipheredLines.get(i)));
        }

        return decodedText;
    }

    private static boolean writeDecodedToFile(List<String> decodedText) {

        try {
            Path pathToFile = Paths.get(outputFileName);
            if (pathToFile.getParent() != null) {
                Files.createDirectories(pathToFile.getParent());
                Files.createFile(pathToFile);
            }
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
        } catch (IOException e) {
            System.err.println("Problem with file");
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

                            case 'c':
                                flagConfigFile = true;
                                ++i;
                                if (i < args.length)
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

        if (flagInput && flagConfigFile) {
            readConfigFile(configFileName);

            List<String> input;
            input = readFile(inputFileName);
            input = runDecrypter(input);

            if (flagOutput) {
                writeDecodedToFile(input);
            } else {
                writeDecodedToOutput(input);
            }
        }
    }
}

