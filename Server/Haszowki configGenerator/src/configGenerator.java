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

    private static String VERSION_NUMBER = "0.7.9";


    private static boolean flagVersion = false;
    private static boolean flagConfigFile = false;
    private static String configFileName = "";
    private static boolean flagMaxVersion = false;
    private static String maxVersion = "";
    private static boolean flagMinVersion = false;
    private static String minVersion = "";

    private static String stringPublicKey;
    private static String stringPrivateKey;

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    private static boolean generatePrivateKeyFile() {

        try {
            Path pathToFile = Paths.get(configFileName);
            if (pathToFile.getParent() != null) {
                Files.createDirectories(pathToFile.getParent());
                Files.createFile(pathToFile);
            }
            PrintWriter writer = new PrintWriter(configFileName + ".privKey", "UTF-8");

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
            max.setValue(maxVersion);
            version.setAttributeNode(max);

            // shorten way
            // version.setAttribute("min", minVersion);

            Attr min = doc.createAttribute("min");
            min.setValue(minVersion);
            version.setAttributeNode(min);


            // key element
            Element key = doc.createElement("key");
            rootElement.appendChild(key);

            key.setAttribute("rsa", stringPublicKey);


            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);


            Path pathToFile = Paths.get(configFileName);
            if (pathToFile.getParent() != null) {
                Files.createDirectories(pathToFile.getParent());
                Files.createFile(pathToFile);
            }
            StreamResult result = new StreamResult(new File(configFileName + ".xml"));

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
                                flagVersion = true;
                                ++i;
                                break;

                            case 'h':
                                flagMaxVersion = true;
                                ++i;
                                if (i < args.length)
                                    maxVersion = args[i];
                                else {
                                    flagMaxVersion = false;
                                    throw new IllegalArgumentException("Give max acceptable version");
                                }
                                break;

                            case 'l':
                                flagMinVersion = true;
                                ++i;
                                if (i < args.length)
                                    minVersion = args[i];
                                else {
                                    flagMinVersion = false;
                                    throw new IllegalArgumentException("Give min acceptable version");
                                }
                                break;

                            case 'c':
                                flagConfigFile = true;
                                ++i;
                                if (i < args.length)
                                    configFileName = args[i];
                                else {
                                    flagConfigFile = false;
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

        if (flagVersion) {
            System.out.println("Version " + VERSION_NUMBER);
            return;
        }

        if (flagConfigFile && flagMaxVersion && flagMinVersion) {
            generateKeyPair();
            generateConfigFile();
            generatePrivateKeyFile();

        }
    }
}
