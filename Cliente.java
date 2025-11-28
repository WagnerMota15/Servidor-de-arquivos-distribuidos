import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Scanner;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {

    private static final String ipServidor = "127.0.0.1";
    private static final int portaServidor = 5000;
    private static final int timeout = 30000;

    private static void baixarArquivo(String ip, int porta, String nomeArquivo) {
        try (Socket socket = new Socket(ip, porta);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             DataInputStream dataIn = new DataInputStream(socket.getInputStream());
             FileOutputStream fos = new FileOutputStream("download_" + nomeArquivo)) {

            out.println(nomeArquivo);
            long tamanho = dataIn.readLong();

            if (tamanho == -1) {
                System.out.println("Arquivo não encontrado neste servidor.\n");
                return;
            }

            System.out.println("Baixando " + nomeArquivo + " (" + tamanho + " bytes) de " + ip + ":" + porta + "...");
            byte[] buffer = new byte[8192];
            int lidos;
            long total = 0;
            while (total < tamanho && (lidos = dataIn.read(buffer, 0, (int)Math.min(buffer.length, tamanho - total))) != -1) {
                fos.write(buffer, 0, lidos);
                total += lidos;
            }
            fos.flush();
            fos.getFD().sync();
            fos.close();

            System.out.println("Download concluído → download_" + nomeArquivo + "\n");
        } catch (Exception e) {
            System.out.println("Erro no download: " + e.getMessage() + "\n");
        }
    }


    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Cliente iniciado ===\n");

        while (true) {
            System.out.print("Digite o nome do arquivo (ou 'sair' para encerrar): ");
            String nomeArquivo = scanner.nextLine().trim();

            if (nomeArquivo.equalsIgnoreCase("sair") || nomeArquivo.equalsIgnoreCase("exit")) {
                System.out.println("Cliente encerrado.");
                break;
            }
            if (nomeArquivo.isEmpty()) {
                System.out.println("Nome inválido!\n");
                continue;
            }

            try (Socket socketPrincipal = new Socket()) {
                socketPrincipal.connect(new InetSocketAddress(ipServidor, portaServidor), timeout);
                socketPrincipal.setSoTimeout(timeout);

                PrintWriter out = new PrintWriter(socketPrincipal.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socketPrincipal.getInputStream()));

                out.println(nomeArquivo);
                System.out.println("Solicitação enviada para: " + nomeArquivo + "\n");

                String resposta = in.readLine();
                if (resposta == null || resposta.equals("Nenhum") || resposta.trim().isEmpty()) {
                    System.out.println("Nenhum servidor possui '" + nomeArquivo + "'.\n");
                    continue;
                }

                String[] servidores = resposta.split(",");
                System.out.println("Servidores encontrados (" + servidores.length + "):");
                for (int i = 0; i < servidores.length; i++) {
                    System.out.println((i + 1) + " → " + servidores[i]);
                }

                System.out.print("\nEscolha o servidor (1-" + servidores.length + "): ");
                String escolhaStr = scanner.nextLine().trim();
                int escolha = Integer.parseInt(escolhaStr) - 1;
                if (escolha < 0 || escolha >= servidores.length) {
                    System.out.println("Escolha inválida!\n");
                    continue;
                }

                String servidorEscolhido = servidores[escolha];
                String[] partes = servidorEscolhido.split(":");
                String ipArquivo = partes[0];
                int portaArquivo = Integer.parseInt(partes[1]);
                System.out.println("Baixando de: " + servidorEscolhido + "\n");

                baixarArquivo(ipArquivo, portaArquivo, nomeArquivo);
            } catch (Exception e) {
                System.out.println("Erro: " + e.getMessage() + "\n");
            }
            System.out.println("─".repeat(50) + "\n");
        }
        scanner.close();
    }
}
