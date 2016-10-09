import org.apache.commons.cli.*;

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

    private static String VERSION_NUMBER = "0.0.1";
    private static String cryptoPass = "Moje haslo to brak hasla";
    private static String fileName = "";


    private static String decryptIt(String text) {

        try {
            DESKeySpec keySpec = new DESKeySpec(cryptoPass.getBytes("UTF8"));
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
            PrintWriter writer = new PrintWriter(fileName + "_dec", "UTF-8");

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

    private static void parseArguments(String[] args) {

        Options options = new Options();

        Option version = new Option("v", "version");
        version.setRequired(false);
        options.addOption(version);

        Option output = new Option("o", "output", true, "output file");
        output.setRequired(false);
        options.addOption(output);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
            return;
        }

        String inputFilePath = cmd.getOptionValue("input");
        String outputFilePath = cmd.getOptionValue("output");

        System.out.println(inputFilePath);
        System.out.println(outputFilePath);
    }

    public static void main(String[] args) {

        parseArguments(args);

        /*
        fileName = args[0];

        List<String> input;

        input = readFile(fileName);

        input = reformatInput(input);

        input = runDecrypter(input);

        writeDecodedToFile(input);
        */
    }
}

