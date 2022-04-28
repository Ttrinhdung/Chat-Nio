import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.lang.Math;


/**
 * Server java NIO
 *
 * @author thomas.trinh dung
 */
public class Server {
    // On alloue a notre buffer 2048bytes
    static ByteBuffer buffer = ByteBuffer.allocate(2048);
    static private Charset charset = Charset.forName("UTF-8");
    // Utiliser pour enregistrer le nombre de clients en ligne et les pseudos
    private static HashSet<String> users = new HashSet<String>();
    // Message d'erreur
    private static String USER_EXIST = "Le nom d'utilisateur est deja utiliser, veuillez taper un nouveau";
    // Protocole utiliser avec le client pour gérer la création de pseudo
    private static String USER_CONTENT_SPLIT = "#@#";

    public static void main(String[] args) throws IOException {


        //  On ouvre le sélecteur
        Selector selector = Selector.open();
        //  On ouvre le canal serveur
        ServerSocketChannel server = ServerSocketChannel.open();
        //  On écoute sur le port 7000
        server.bind(new InetSocketAddress("localhost", 7000));
        //  On passe en mode non bloquant
        server.configureBlocking(false);
        //  On enregistre le sélecteur OP_accept sur le canal serveur
        server.register(selector, SelectionKey.OP_ACCEPT);
        // On affiche un message indiquant l'ouverture du serveur
        System.out.println("Ouverture du serveur... Port : 7000");

        // On crée une boucle infinie pour que le serveur fonctionne en continu
        while (true) {
            // On attend qu'un channel soit prêt pour une opération
            selector.select();
            // On créer notre iterator
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            // On crée une boucle tant que l'iterateur a d'autre élément à itérer
            while (iterator.hasNext()) {
                // On sélectionne la prochaine clée dans l'iterateur
                SelectionKey key = iterator.next();
                // On supprime l'élément courant de la collection
                iterator.remove();

                // On saute toute les clées invalides
                if (!key.isValid()) {
                    continue;
                }

                // On teste si le canal de cette clé est prêt à accepter une nouvelle connexion socket
                if (key.isAcceptable()) {
                    //  On récupère le canal de la clé
                    server = (ServerSocketChannel) key.channel();
                    //  On accept la connexion
                    SocketChannel client = server.accept();
                    //  On passe en mode non bloquant
                    client.configureBlocking(false);
                    //  On enregistre le selectuer OP_READ sur le canal client
                    client.register(selector, SelectionKey.OP_READ);
                    // On envoie au client que la connexion a été acceptée
                    write("\nConnection acceptée, veuillez entrez votre pseudo ", client);
                    key.interestOps(SelectionKey.OP_ACCEPT);

                    // On teste si le canal de cette clé est prêt pour lire
                } else if (key.isReadable()) {
                    //  On récupère le canal de la clé
                    SocketChannel client = (SocketChannel) key.channel();
                    //  On crée une chaîne de caractères mutables
                    StringBuilder sb = new StringBuilder();
                    //  On nettoie le buffer
                    buffer.clear();
                    //  On lit ce que le client nous a envoyé et on le met dans le buffer
                    try {
                        while (client.read(buffer) > 0) {
                            buffer.flip();
                            sb.append(charset.decode(buffer));

                        }
                        // on affiche sur le server l'adresse du nouveau client et le contenu du message envoyé
                        System.out.println("Message reçu de : " + client.getRemoteAddress() + " avec comme valeur: " + sb);
                        // On règle ce canal pour être prêt à recevoir des données la prochaine fois
                        key.interestOps(SelectionKey.OP_READ);
                    // En cas d'erreur on annule la clé
                    } catch (IOException io) {
                        key.cancel();
                        if (key.channel() != null) {
                            key.channel().close();
                        }
                    }

                    //  Si on lit un message
                    if (sb.length() > 0) {
                        String[] arrayContent = sb.toString().split(USER_CONTENT_SPLIT);
                        // Parti qui permet d'enregistré un utilisateur
                        // Si arrayContent est de taille 1 c'est que l'utilisateur n'est pas enregistré
                        if (arrayContent != null && arrayContent.length == 1) {
                            String name = arrayContent[0];
                            // Si le nom envoyé et deja utilisé afficher message d'erreur
                            if (users.contains(name)) {
                                client.write(charset.encode(USER_EXIST));
                            // Sinon l'ajouter a la liste des users
                            } else {
                                users.add(name);
                                // On incrémente le nombre de clients connecté
                                int num = OnlineNum(selector);

                                // On crée une variable rand entre 1 et 5 qui permet d'afficher des messages différents lors de la connection
                                int max = 5;
                                int min = 1;
                                int range = max - min + 1;
                                int rand = (int)(Math.random() * range) + min;
                                if (rand==1){
                                    String message = name + ", Le roi du nord !Vient de se connecter. Membres en ligne :"+num;
                                    broadcast(message,selector,null);
                                }
                                else if ( rand ==2){
                                    String message = name + ", membre de la communauté de l'anneau ! Vient de se connecter. Membres en ligne :"+num;
                                    broadcast(message,selector,null);
                                }
                                else if ( rand ==3){
                                    String message = "C'est "+name + " l'élu ! Vient de se connecter a la matrix. Membres en ligne :"+num;
                                    broadcast(message,selector,null);
                                }
                                else if ( rand ==4){
                                    String message = "Seigneur Sith "+name + ". Vient de se connecter. Membres en ligne :"+num;
                                    broadcast(message,selector,null);
                                }
                                else if ( rand ==5){
                                    String message = name + ",maître jedi il est. Connecter il vient. Membres en ligne :"+num;
                                    broadcast(message,selector,null);
                                }

                            }
                        }
                        // Si l'utilisateur est deja enregistré, il veut donc envoyé un message
                        else if (arrayContent != null && arrayContent.length > 1) {
                            String name = arrayContent[0];
                            String message = sb.substring(name.length() + USER_CONTENT_SPLIT.length());
                            // Récupère la date actuelle
                            Date dateActuelle = new Date();
                            // On utilise seulement l'heure
                            SimpleDateFormat  dateFormat = new SimpleDateFormat("HH'h'mm");
                            String laDateFormatee = dateFormat.format(dateActuelle);
                            // On crée le message avec l'heure , le nom de l'expéditeur et le message
                            message = "["+laDateFormatee+"]"+ name + ": " + message;
                            if (users.contains(name)) {
                                // On broadcast a tous les clients sauf l'expéditeur
                                broadcast(message,selector,client);

                            }
                        }
                    }
                }
            }


        }
    }



    /**
     * Ecrit un message
     *
     * @param message
     * @param channel
     * @throws IOException
     */
    private static void write(String message, SocketChannel channel) throws IOException {
        //  On nettoie le buffer
        buffer.clear();
        //  On met dans le buffer notre message
        buffer.put(message.getBytes(StandardCharsets.UTF_8));
        buffer.flip();
        //  On écrit dans le channel le message de notre buffer
        channel.write(buffer);
    }

    /**
     * Envoie un message a tous les clients connectés
     *
     * @param msg
     * @param selector
     * @throws IOException
     */
    private static void broadcast(String msg, Selector selector , SocketChannel except) throws IOException {
        //Broadcast data to all SocketChannel
        for (SelectionKey key : selector.keys()) {
            Channel target = key.channel();
            //Broadcast a tout les utilisateurs sauf except.
            if (target.isOpen() && target instanceof SocketChannel && target!=except) {
                write(msg, (SocketChannel) target);
            }
        }
    }

    /**
     * Compte le nombre de clients connecté et retourne un entier
     *
     * @param selector
     */
    public static int OnlineNum(Selector selector) {
        int res = 0;
        // Pour chaque client connecté on incrémente de 1 res
        for(SelectionKey key : selector.keys())
        {
            Channel targetchannel = key.channel();

            if(targetchannel instanceof SocketChannel)
            {
                res++;
            }
        }
        return res;
    }
}