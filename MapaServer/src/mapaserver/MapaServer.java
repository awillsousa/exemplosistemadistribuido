package mapaserver;

/**
 * 
 * @author AntonioSousa
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Um servidor para várias clientes que se movem de tempos em tempos e
 * informam a sua posicao para o servidor que por sua vez informa para os
 * demais clientes.
 * Esse código é baseado no jogo multi-player tic tac toe apresentado no livro
 * de Deitel and Deitel "Java How to * Program". 
 * As strings que são enviados são as seguintes: 
 *
 *  Client -> Server           Server -> Client
 *  ----------------           ----------------
 *  MOVE <n>  (0 <= n <= 8)    WELCOME <char>  (char in {X, O})
 *  QUIT                       VALID_MOVE
 *                             OTHER_PLAYER_MOVED <n>
 *                             VICTORY
 *                             DEFEAT
 *                             TIE
 *                             MESSAGE <text>
 * 
 */
public class MapaServer {
    private static final int NUM_TOTAL_CLIENTES = 3;
    private static int idCliente = 0;
    
    /**
     * Executa a aplicação e gerencia os clientes que se conectam 
     */
    public static void main(String[] args) throws Exception {
        int portaListener = 8901;    
        //ServerSocket listener = new ServerSocket(8901);
        Mapa mapa = new Mapa();
        System.out.println("Mapa Server executando...");        
        try {     
            ServerSocket listener = new ServerSocket(portaListener);
            while (true) {                
                System.out.println("Aguardando conexão...");
                idCliente++;
                Mapa.ClienteThread cliente = mapa.new ClienteThread(listener.accept(), "Cliente_" + Integer.toString(idCliente), idCliente);
                System.out.println("Criado cliente " + "Cliente_" + Integer.toString(idCliente));
                mapa.threadsClientes.add(cliente);                
                cliente.start();                
            }
        } finally {
             for (Mapa.ClienteThread c : mapa.threadsClientes)             
                c.socket.close();             
        }
    }
}