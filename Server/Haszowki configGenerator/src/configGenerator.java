import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;

/**
 * Created by morri on 17.11.2016.
 */
public class configGenerator {

    private static String VERSION_NUMBER = "1.0.6";


    private static boolean flag_version = false;
    private static boolean flag_config_file = false;
    private static String ConfigFileName = "";
    private static boolean flag_max_version = false;
    private static String MaxVersion = "";
    private static boolean flag_min_version = false;
    private static String MinVersion = "";
    private static boolean flag_skip_validation = false;
    private static boolean flag_proceed_anyway = false;

    private static String stringPublicKey;
    private static String stringPrivateKey;

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    private static boolean generatePrivateKeyFile() {

        try {
            Path pathToFile = Paths.get(ConfigFileName);
            if (pathToFile.getParent() != null) {
                Files.createDirectories(pathToFile.getParent());
                Files.createFile(pathToFile);
            }
            PrintWriter writer = new PrintWriter(ConfigFileName + ".privKey", "UTF-8");

            writer.println(stringPrivateKey);
            writer.flush();
            writer.close();

            return true;

        } catch (FileNotFoundException e) {
            System.err.println("File not found");
            e.printStackTrace();
            return false;
        } catch (UnsupportedEncodingException e) {
            System.err.println("Unsupported encoding");
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            System.err.println("Creating file problem");
            e.printStackTrace();
            return false;
        }
    }

    private static boolean generateConfigFile() {

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root element
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("configuration");
            doc.appendChild(rootElement);

            // version element
            Element version = doc.createElement("version");
            rootElement.appendChild(version);

            // set attribute to version element
            Attr max = doc.createAttribute("max");
            max.setValue(MaxVersion);
            version.setAttributeNode(max);

            // shorten way
            // version.setAttribute("min", MinVersion);

            Attr min = doc.createAttribute("min");
            min.setValue(MinVersion);
            version.setAttributeNode(min);


            // key element
            Element key = doc.createElement("key");
            rootElement.appendChild(key);

            key.setAttribute("rsa", stringPublicKey);

            // flags element
            Element flags = doc.createElement("flags");
            rootElement.appendChild(flags);

            if (flag_skip_validation) flags.setAttribute("skip_validation", "true");
            else flags.setAttribute("skip_validation", "false");

            if (flag_proceed_anyway) flags.setAttribute("proceed_anyway", "true");
            else flags.setAttribute("proceed_anyway", "false");

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);


            Path pathToFile = Paths.get(ConfigFileName);
            if (pathToFile.getParent() != null) {
                Files.createDirectories(pathToFile.getParent());
                Files.createFile(pathToFile);
            }
            StreamResult result = new StreamResult(new File(ConfigFileName + ".xml"));

            // Output to console for testing
            // StreamResult result = new StreamResult(System.out);

            transformer.transform(source, result);

            return true;

        } catch (ParserConfigurationException e) {
            System.err.println("Problem with parser");
            e.printStackTrace();
            return false;
        } catch (TransformerException e) {
            System.err.println("Problem with transformer");
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            System.err.println("Creating file problem");
            e.printStackTrace();
            return false;
        }
    }


    private static void generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(4096);
            KeyPair kp = kpg.genKeyPair();
            Key publicKey = kp.getPublic();
            Key privateKey = kp.getPrivate();
            stringPublicKey = DatatypeConverter.printBase64Binary(publicKey.getEncoded());
            stringPrivateKey = DatatypeConverter.printBase64Binary(privateKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private static void decodeArguments(String[] args) throws IllegalArgumentException {

        for (int i = 0; i < args.length; i++) {
            switch (args[i].charAt(0)) {
                case '-':

                    if (args[i].length() == 2) {
                        switch (args[i].charAt(1)) {
                            case 'v':
                                flag_version = true;
                                break;

                            case 'p':
                                flag_proceed_anyway = true;
                                break;

                            case 's':
                                flag_skip_validation = true;
                                break;

                            case 'h':
                                flag_max_version = true;
                                ++i;
                                if (i < args.length)
                                    MaxVersion = args[i];
                                else {
                                    flag_max_version = false;
                                    throw new IllegalArgumentException("Give max acceptable version");
                                }
                                break;

                            case 'l':
                                flag_min_version = true;
                                ++i;
                                if (i < args.length)
                                    MinVersion = args[i];
                                else {
                                    flag_min_version = false;
                                    throw new IllegalArgumentException("Give min acceptable version");
                                }
                                break;

                            case 'c':
                                flag_config_file = true;
                                ++i;
                                if (i < args.length)
                                    ConfigFileName = args[i];
                                else {
                                    flag_config_file = false;
                                    throw new IllegalArgumentException("Give output config file name");
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

        if (flag_version) {
            System.out.println("Version " + VERSION_NUMBER);
            return;
        }

        if (flag_config_file && flag_max_version && flag_min_version) {
            generateKeyPair();
            generateConfigFile();
            generatePrivateKeyFile();

        }
    }
}
