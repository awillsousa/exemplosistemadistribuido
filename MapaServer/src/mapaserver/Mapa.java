/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mapaserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

/**
*
* @author AntonioSousa 
* Um mapa de 100 posições.  Cada cliente ocupará uma posição nesse tabuleiro.
* Inicialmente o cliente solicita a conexão ao servidor, o servidor envia ao cliente seu id e sua posição inicial
* Caso o cliente deseje se mover, ele comunica ao servidor a sua pretensão de deslocar para aquela
* posição. O servidor deverá avaliar se a posição está ocupada ou se dois clientes estão disputando
* a mesma posição. 
* O servidor irá comunicar aos outros server a posição de cada um dos clientes, para que eles atualizem o 
* posicionamento em suas estruturas. 
* 
*/
class Mapa {
        
    public ArrayList<ClienteThread> threadsClientes = new ArrayList<ClienteThread>();         // threadsClientes dos clientes
    int[][] clientes = new int[10][10];
    ClienteThread clienteAtual;           // cliente atual
    int nextId = 1;
   
    public Mapa()
    {
        super();
        inicializaPosicoes();
    }
    
    private void inicializaPosicoes()
    {
     for (int i=0; i < 10; i++)
         for (int j=0; j < 10; j++)
             clientes[i][j] = 0;
    }
    
    /* Seta o cliente atual */
    public void setClienteAtual(ClienteThread cliente)
    {        
        this.clienteAtual = cliente;
    }
    
    
    
    /**
     * Chamado pela thread cliente quando um cliente tenta se deslocar.
     * Verifica se a posição está ocupada e informa ao demais clientes.      
     */
    public synchronized boolean podeMover(int posX, int posY, ClienteThread cliente) {
        if (clientes[posX][posY] == 0) {            
            return true;
        }
        return false;
    }
   
    public synchronized void moveCliente(int posX, int posY, int posXanterior, int posYanterior, ClienteThread cliente) {
        if (podeMover(posX, posY, cliente))
        {
            clientes[posX][posY] = cliente.id;
            clientes[posXanterior][posYanterior] = 0;
            cliente.posX = posX;
            cliente.posY = posY;
        }        
    }  
    
    /**
     * Chamado pela thread cliente quando a solicitacao de atualizacao de mapa for recebida.
     */
    public void atualizaMapa()
    {
        for (ClienteThread c : this.threadsClientes )
        {
            c.output.println("ATUALIZA "+ c.id  +" "+ c.posX +" "+ c.posY);
        }
    }
    
    public void notificaClientes(String msg)
    {        
        for (ClienteThread c : this.threadsClientes )
        {
            c.output.println(msg);
        }
    }
        
    /**
     * Classe que irá auxiliar na comunicação entre as diversas threads 
     * criadas pelo sistema.
     * Cada cliente é identificado por uma marca e um nome. A posicao que
     * o cliente se encontra é definido por um par de números 
     */
    class ClienteThread extends Thread {
        String nome = "Cliente";        //nome do cliente
        int id = 0;              // marca do cliente que ira aparecer no mapa
        int posX = -1;                  // posicao no eixo X do cliente
        int posY = -1;                  // posicao no eixo Y do cliente        
        Socket socket;  
        BufferedReader input;
        PrintWriter output;

        /**
         * Cria uma thread para lidar com um socket e a marca de um cliente
         */
        public ClienteThread(Socket socket, String nome, int id) {
            this.nome = nome;
            this.socket = socket;
            this.id = id;
            try {                
                input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                output.println("WELCOME " + nome + " seu id: " + String.valueOf(id));                
            } catch (IOException e) {
                System.out.println("Cliente encerrado: " + e);
            }
        }
   
        /**
         * Metodo de execucao da thread.
         */
        @Override
        public void run() {
            Random geraRandomicos = new Random();
            try {
                // Repetidamente recebe os comandos do clientes e os processa
                while (true) {
                    String command = input.readLine();
                    if (command.startsWith("OLA"))  // Conexão inicial do cliente
                    {
                        this.posX = geraRandomicos.nextInt(9);
                        this.posY = geraRandomicos.nextInt(9);
                        if (podeMover(posX, posY, this)) {                            
                            output.println("INICIO " + id + " " +posX + " " + posY);                             
                            notificaClientes("POSICIONA " + id + " " +posX + " " + posY);
                        } else {
                            output.println("MESSAGE ?");
                        }                        
                    }
                    else if (command.startsWith("MOVE")) {
                        String[] campos = command.split(" ");
                        this.id = Integer.parseInt(campos[1]);
                        int posX = Integer.parseInt(campos[2]);
                        int posY = Integer.parseInt(campos[3]); 
                        
                        if (podeMover(posX, posY, this)) {
                            output.println("PODE_MOVER " + this.id + " " + posX + " " + posY); 
                            int xAnterior = this.posX;
                            int yAnterior = this.posY;
                            moveCliente(posX, posY, this.posX, this.posY, this);
                            String msg = "MOVE "+ this.id + " " + xAnterior + " " + yAnterior + " " + posX + " " + posY;
                            notificaClientes(msg);
                            output.println(msg);                            
                        } else {
                            output.println("NAO_PODE_MOVER");
                        }
                    } else if (command.startsWith("QUIT")) {
                        System.out.println(command);
                        int indice;
                        String[] campos = command.split(" ");
                        notificaClientes("KILL " + id + " " +posX + " " + posY);
                        for (ClienteThread c : threadsClientes)
                        {                            
                            if (c.id == Integer.parseInt(campos[1]))
                            {
                                indice = threadsClientes.indexOf(c);
                                clientes[c.posX][c.posY] = 0;
                                threadsClientes.remove(indice);
                            }
                        }
                        return;
                    }
                }
            } catch (IOException e) {
                System.out.println("Cliente encerrado: " + e);                
            } finally {
                try {socket.close();} catch (IOException e) {}
            }
        }
    }
}
