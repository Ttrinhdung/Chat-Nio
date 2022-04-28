import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.nio.charset.Charset;

/**
 * Client java NIO
 *
 * @author thomas.trinh dung
 */
public class Client {
    // On alloue a notre buffer 1024bytes
    static ByteBuffer buffer = ByteBuffer.allocate(1024);
    // Protocole utiliser avec le server pour gérer la création de pseudo
    private static String USER_CONTENT_SPILIT = "#@#";
    static private String name = "";
    static private Charset charset = Charset.forName("UTF-8");

    public static void main(String[] args) throws IOException {
        //  On ouvre le sélecteur
        Selector selector = Selector.open();
        //  On ouvre le canal client et on écoute sur le port 7000
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 7000));
        //  On passe en mode non bloquant
        socketChannel.configureBlocking(false);
        //  On enregistre le sélecteur OP_READ sur le canal client
        socketChannel.register(selector, SelectionKey.OP_READ);

        //  On crée un Thread qui lit les messages envoyer pour le serveur
        new Thread(() -> {
            SocketChannel client = null;
            while (true) {
                try {
                    // On attend qu'un channel soit prêt pour une opération
                    selector.select();
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    // On créer notre iterator
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    // On crée une boucle tant que l'iterateur a d'autre élément à itérer
                    while (iterator.hasNext()) {
                        // On sélectionne la prochaine clée dans l'iterateurr
                        SelectionKey key = iterator.next();
                        // On supprime l'élément courant de la collection
                        iterator.remove();

                        // On teste si le canal de cette clé est prêt pour lire
                        if (key.isReadable()) {
                            client = (SocketChannel) key.channel();
                            //  On crée une chaîne de caractères mutables
                            StringBuilder sb = new StringBuilder();
                            // On nettoie le buffer
                            buffer.clear();

                            //  On lit les bytes envoyés par le canal client et on les ajoute a la chaine de caractère sb
                            while ((client.read(buffer)) > 0) {
                                buffer.flip();
                                byte[] bytes = new byte[buffer.limit()];
                                buffer.get(bytes);
                                sb.append(new String(bytes));
                                buffer.clear();
                            }
                            // On crée un string qui prend la valeur du stringbuilder sb
                            String msg;
                            msg = "" + sb;

                            System.out.println(msg);
                            // On règle ce canal pour être prêt à recevoir des données la prochaine fois
                            key.interestOps(SelectionKey.OP_READ);
                        }
                    }
                    selectionKeys.clear();
                    // En cas d'erreur en ferme la connection et on affiche la stacktrace
                } catch (IOException e) {
                    if (client != null) {
                        try {
                            client.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }
            // On lance le thread
        }).start();

        //  Thread principale, utiliser pour lire et écrire les messages
        Scanner scanner = new Scanner(System.in);
        // On boucle tant que le scanner a une autre ligne
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            // On interdit les espaces
            if ("".equals(line)) continue;
            // Si la variable name est vide on la remplie avec line
            if ("".equals(name)) {
                name = line;
                line = name + USER_CONTENT_SPILIT;
            // Sinon on transforme le message écrit avec le nom d'utilisateur + le protocole + le message
            } else {
                line = name + USER_CONTENT_SPILIT + line;
            }
            // On écrit le message au serveur
            socketChannel.write(charset.encode(line));
        }

    }
}