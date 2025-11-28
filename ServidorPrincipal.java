import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServidorPrincipal {
    private static final int porta = 5000;
    private static final String ipMulticast = "239.1.1.1";
    private static final int portaMulticast = 6000;
    private static final int timeout = 5000;

    // armazena temporariamente as respostas dos servidores de arquivos por solicitação
    private static final Map<String, CopyOnWriteArrayList<String>> respostasPorArquivo =
            new ConcurrentHashMap<>();

    private static class ClienteHandler implements Runnable{
        private final Socket socket;
        public ClienteHandler(Socket socket) {
            this.socket = socket;
        }
        @Override
        public void run() {
            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                String nomeArquivo = in.readLine();
                if (nomeArquivo == null || nomeArquivo.trim().isEmpty()) {
                    out.println("Nenhum");
                    return;
                }
                nomeArquivo = nomeArquivo.trim();
                System.out.println("Cliente " + socket.getInetAddress() + " solicitou: " + nomeArquivo);

                // Gera um ID único para essa solicitação
                String idSolicitacao = nomeArquivo + "_" + System.nanoTime() + "_" + new java.util.Random().nextInt(1000);
                respostasPorArquivo.put(idSolicitacao, new CopyOnWriteArrayList<>());

                enviarPerguntaMulticast(nomeArquivo, idSolicitacao);

                CopyOnWriteArrayList<String> servidoresComArquivo = respostasPorArquivo.get(idSolicitacao);
                long inicio = System.currentTimeMillis();
                while (System.currentTimeMillis() - inicio < timeout) {
                    Thread.sleep(100);
                }

                if (servidoresComArquivo.isEmpty()) {
                    out.println("Nenhum");
                    System.out.println("Nenhum servidor possui '" + nomeArquivo + "'");
                } else {
                    String resposta = String.join(",", servidoresComArquivo);
                    out.println(resposta);
                    System.out.println("Enviada lista com " + servidoresComArquivo.size() + " servidor(es) para o cliente.");
                }
                respostasPorArquivo.remove(idSolicitacao);
            } catch (Exception e) {
                System.err.println("Erro ao tratar cliente: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }
    }


    private static void enviarPerguntaMulticast(String nomeArquivo, String idSolicitacao) {
        new Thread(() -> {
            try (MulticastSocket socket = new MulticastSocket()) {
                String mensagem = "Busca:" + nomeArquivo + ":" + idSolicitacao;
                byte[] buffer = mensagem.getBytes();
                InetAddress group = InetAddress.getByName(ipMulticast);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, portaMulticast);
                socket.send(packet);
            } catch (IOException e) {
                System.err.println("Erro ao enviar multicast: " + e.getMessage());
            }
        }).start();
    }


    private static void escutarRespostasMulticast() {
        try (MulticastSocket socket = new MulticastSocket(portaMulticast)) {
            InetAddress group = InetAddress.getByName(ipMulticast);
            socket.joinGroup(group);

            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String resposta = new String(packet.getData(), 0, packet.getLength());
                if (resposta.startsWith("Resposta:")) {
                    String[] partes = resposta.substring(9).split(":", 3);
                    if (partes.length == 3) {
                        String idSolicitacao = partes[0];
                        String ip = partes[1];
                        String porta = partes[2];

                        // Reconstrói a lista correta usando o ID da solicitação
                        respostasPorArquivo.forEach((id, lista) -> {
                            if (id.equals(idSolicitacao)) {
                                String endereco = ip + ":" + porta;
                                if (!lista.contains(endereco)) {
                                    lista.add(endereco);
                                    System.out.println("Servidor " + endereco + " confirmou ter o arquivo (ID: " + idSolicitacao + ")");
                                }
                            }
                        });
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro no receptor multicast: " + e.getMessage());
        }
    }


    public static void main(String[] args) {
        new Thread(ServidorPrincipal::escutarRespostasMulticast).start();
        System.out.println("=== Servidor Principal Iniciado ===");
        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            while (true) {
                Socket clienteSocket = serverSocket.accept();
                new Thread(new ClienteHandler(clienteSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor principal: " + e.getMessage());
        }
    }
}
