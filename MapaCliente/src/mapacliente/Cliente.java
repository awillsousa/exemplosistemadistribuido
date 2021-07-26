package mapacliente;

import java.awt.Color;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/** * 
 *
 *  Comunicação entre o cliente e o servidor:
 *  Cliente -> Servidor           Servidor -> Cliente
 *  ----------------           ----------------
 *  OLA <n>  (0 <= n <= 8)     WELCOME   <msg>
 *  MOVE                       INICIO
 *  QUIT <id>                  PODE_MOVER
 *                             PODE_MOVER
 *                             MOVE <id> <posXantiga> <posYantigo> <posX> <posY>
 *                             POSICIONA <id> <posX> <posY>
 *                             MESSAGEM <texto>
 *
 */
public class Cliente {

    private JFrame frame = new JFrame("Cliente");
    private JLabel messageLabel = new JLabel("");
    private ImageIcon icon;    
    
    private static final int TAM_MAPA = 10;
    private Square[][] board = new Square[TAM_MAPA][TAM_MAPA];
    private Square currentSquare;

    private static int PORT = 8901;
    private int id;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Timer timer;

    /**
     * Constructs the client by connecting to a server, laying out the
     * GUI and registering GUI listeners.
     */
    public Cliente(String serverAddress) throws Exception {

        // Configurando o acesso de rede
        socket = new Socket(serverAddress, PORT);
            in = new BufferedReader(new InputStreamReader(
            socket.getInputStream()));
           out = new PrintWriter(socket.getOutputStream(), true);

        // Layout
        messageLabel.setBackground(Color.lightGray);
        frame.getContentPane().add(messageLabel, "South");

        JPanel boardPanel = new JPanel();
        boardPanel.setBackground(Color.black);
        boardPanel.setLayout(new GridLayout(10, 10, 2, 2));
        for (int i = 0; i < TAM_MAPA; i++)
            for (int j = 0; j < TAM_MAPA; j++)
            {                
                board[i][j] = new Square();                 
                //board[i][j].setIcon(new ImageIcon(Cliente.class.getResource("icones/Icone_01.png")));
                boardPanel.add(board[i][j]);                
            }
        frame.getContentPane().add(boardPanel, "Center");
        
        // Configura o frame para enviar uma mensagem para o servidor informando que está encerrando]
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {            
                    //System.exit(0);             
                out.println("QUIT " + id);
                try { 
                    socket.close();
                } catch (IOException ex) {
                    Logger.getLogger(Cliente.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        
        out.println("OLA");
    }

    /**
     * A thread principal do cliente irá executar aguardando por mensagem vindas do servidor. 
     * Inicialmente o cliente comunica com o servidor, que lhe fornece um ID e um posicionamento
     * aleatório. Notificando aos outros clientes que um novo cliente deve ser posicionado nos seus
     * respectivos tabuleiros.
     */    
    public void play() throws Exception {
        String response;
        boolean iniciado = false;
        try {             
            while (true) {
                response = in.readLine();
                if (response.startsWith("WELCOME")) {                    
                    System.out.println(response); 
                } else if (response.startsWith("INICIO")) {                    
                    String[] campos = response.split(" ");
                      this.id = Integer.parseInt(campos[1]);
                     int posX = Integer.parseInt(campos[2]);
                     int posY = Integer.parseInt(campos[3]);                    
                    this.icon = new ImageIcon(Cliente.class.getResource("icones/Icone_0"+campos[1]+".png"));//new ImageIcon("icones/Icone_0"+String.valueOf(proxIcone++)+".png");                    
                    this.board[posX][posY].setIcon(this.icon);
                    this.frame.repaint();
                    frame.setTitle("Cliente " + campos[1]);
                    iniciado = true;
                } else if (response.startsWith("PODE_MOVER")) {
                    messageLabel.setText("Movimento possivel, aguarde...");                    
                } else if (response.startsWith("NAO_PODE_MOVER")) {
                    messageLabel.setText("Posicionamento inválido! Posição ocupada.");                    
                } else if ((response.startsWith("MOVE")) && iniciado) {    // não pode mover antes de inicializar o cliente                
                    String[] campos = response.split(" ");
                    int idCliente = Integer.parseInt(campos[1]);
                    int posXanterior = Integer.parseInt(campos[2]);
                    int posYanterior = Integer.parseInt(campos[3]); 
                    int posX = Integer.parseInt(campos[4]);
                    int posY = Integer.parseInt(campos[5]);
                    board[posXanterior][posYanterior].setBorder(BorderFactory.createLineBorder(Color.red));
                    board[posX][posY].setBorder(BorderFactory.createLineBorder(Color.green));
                    //board[posXanterior][posYanterior].setBackground(Color.red);
                    //board[posX][posY].setBackground(Color.green);
                    Thread.sleep(500);
                    board[posXanterior][posYanterior].setIcon(null);
                    board[posX][posY].setIcon(new ImageIcon(Cliente.class.getResource("icones/Icone_0"+idCliente+".png")));
                    board[posXanterior][posYanterior].setBorder(BorderFactory.createLineBorder(Color.white));
                    board[posX][posY].setBorder(BorderFactory.createLineBorder(Color.white));
                    //board[posXanterior][posYanterior].setBackground(Color.WHITE);
                    //board[posX][posY].setBackground(Color.WHITE);
                    board[posX][posY].repaint();
                    board[posXanterior][posYanterior].repaint();                                       
                    messageLabel.setText("Deslocou de (" + posXanterior+","+posYanterior+") para (" + posX + "," + posY + ")");                    
                } else if ((response.startsWith("POSICIONA")) && iniciado) {     // não pode atualizar posição antes de inicializar o cliente                    
                    String[] campos = response.split(" ");                      
                     int idCliente = Integer.parseInt(campos[1]);
                     int posX = Integer.parseInt(campos[2]);
                     int posY = Integer.parseInt(campos[3]);                    
                    ImageIcon icone = new ImageIcon(Cliente.class.getResource("icones/Icone_0"+idCliente+".png"));
                    this.board[posX][posY].setIcon(icone);
                    this.frame.repaint();
                } else if (response.startsWith("MENSAGEM")) {
                    messageLabel.setText(response.substring(9));
                } else if (response.startsWith("KILL")) {
                    String[] campos = response.split(" ");                      
                     int idCliente = Integer.parseInt(campos[1]);
                     int posX = Integer.parseInt(campos[2]);
                     int posY = Integer.parseInt(campos[3]);                                        
                    this.board[posX][posY].setIcon(null);
                    this.frame.repaint();    
                }
            }            
        }
        finally {
            this.out.println("QUIT " + this.id);
            socket.close();
        }
    }
    

    /**
     * Um JPanel contendo um JLabel representando uma
     * celula do mapa
     */
    static class Square extends JPanel {
        JLabel label = new JLabel((Icon)null);

        public Square() {
            setBackground(Color.white);
            add(label);
        }

        public void setIcon(Icon icon) {
            label.setIcon(icon);
        }
    }

    public void moveCliente()
    {        
        Random geraRandomicos = new Random();
        int novoX = geraRandomicos.nextInt(TAM_MAPA);
        int novoY = geraRandomicos.nextInt(TAM_MAPA);        
        out.println("MOVE" + " " + this.id + " " + novoX + " " + novoY);
    }
        
    public void configuraTime(int segundos)
    {
        timer = new Timer();
        timer.scheduleAtFixedRate(new AtualizaTask(), 0, segundos*1000);     
    }
    
    /**
     * Runs the client as an application.
     */
    public static void main(String[] args) throws Exception {
        while (true) {
            String serverAddress = null; // = "172.16.3.7";
				while (serverAddress == null || serverAddress.equals("")) {
					serverAddress = JOptionPane.showInputDialog("Digite o IP do Servidor");
					if (serverAddress == null || serverAddress.equals("")) {
						 serverAddress = "localhost";
					}
			}
//				            String serverAddress = (args.length == 0) ? "localhost" : args[1];
            final Cliente cliente = new Cliente(serverAddress);
            cliente.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            cliente.frame.setSize(1000, 750);
            cliente.frame.setVisible(true);
            cliente.frame.setResizable(true);
            cliente.configuraTime(4);            
            cliente.play();              
        }
    }
    
    class AtualizaTask extends TimerTask {
    @Override
    public void run() {
      System.out.println("Enviando comando para movimentacao.");
      moveCliente();      
    }
  }
    
}
