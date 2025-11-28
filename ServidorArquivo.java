import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServidorArquivo {
    private static String diretorio;
    private static final String ipMulticast = "239.1.1.1";
    private static final int portaMulticast = 6000;
    private static final int portaTCP = 7000;

    private static void escutarMulticast(int portaTCP) {
        MulticastSocket socket = null;
        try {
            socket = new MulticastSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(portaMulticast));

            InetAddress group = InetAddress.getByName(ipMulticast);
            socket.joinGroup(new InetSocketAddress(group, portaMulticast), null);

            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String mensagem = new String(packet.getData(), 0, packet.getLength());
                if (mensagem.startsWith("Busca:")) {
                    String[] partes = mensagem.substring(6).split(":", 2);
                    String nomeArquivo = partes[0];
                    String idSolicitacao = partes[1];

                    Path caminho = Paths.get(diretorio, nomeArquivo);
                    if (Files.exists(caminho) && Files.isRegularFile(caminho)) {
                        responderQueTemArquivo(socket, group, idSolicitacao, portaTCP);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro no listener multicast: " + e.getMessage());
        }
    }


    private static void responderQueTemArquivo(MulticastSocket socket, InetAddress group, String idSolicitacao, int portaTCP) throws IOException {
        try {
            String ipLocal = InetAddress.getLocalHost().getHostAddress(); // Usa o IP local real (importante em simulação com várias instâncias na mesma máquina)
            String resposta = "Resposta:" + idSolicitacao + ":" + ipLocal + ":" + portaTCP;

            byte[] buffer = resposta.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, portaMulticast);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static class DownloadHandler implements Runnable {
        private final Socket socket;

        public DownloadHandler(Socket socket) {
            this.socket = socket;
        }
        @Override
        public void run() {
            try (socket;
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                String nomeArquivo = in.readLine();
                if (nomeArquivo == null) return;

                Path caminho = Paths.get(diretorio, nomeArquivo);
                if (!Files.exists(caminho)) {
                    dataOut.writeLong(-1); // sinaliza "não encontrado"
                    System.out.println("Cliente " + socket.getInetAddress() + " pediu '" + nomeArquivo + "' → NÃO ENCONTRADO");
                    return;
                }

                long tamanho = Files.size(caminho);
                dataOut.writeLong(tamanho); // envia tamanho primeiro
                System.out.println("Enviando '" + nomeArquivo + "' (" + tamanho + " bytes) para " + socket.getInetAddress());

                try (InputStream fileIn = Files.newInputStream(caminho)) {
                    byte[] buffer = new byte[8192];
                    int bytesLidos;
                    while ((bytesLidos = fileIn.read(buffer)) != -1) {
                        dataOut.write(buffer, 0, bytesLidos);
                    }
                }
                System.out.println("Download de '" + nomeArquivo + "' concluído para " + socket.getInetAddress());

            } catch (IOException e) {
                System.err.println("Erro no download: " + e.getMessage());
            }
        }
    }


    public static void main(String[] args) throws IOException {
        int PortaTCPAtual = portaTCP;
        if (args.length > 1) {
            PortaTCPAtual = Integer.parseInt(args[1]);
        } else {
            for (int tentativa = 0; tentativa < 5; tentativa++) {
                try (ServerSocket test = new ServerSocket(PortaTCPAtual)) {
                    break; //
                } catch (IOException e) {
                    PortaTCPAtual++;
                }
            }
        }
        final int PortaTCPFinal = PortaTCPAtual;

        String id = (args.length > 0) ? args[0] : String.valueOf(PortaTCPFinal);
        diretorio = "arquivos_servidor_" + id;
        Path dir = Paths.get(diretorio);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
            System.out.println("Pasta criada: " + dir.toAbsolutePath());
        }

        Files.createDirectories(Paths.get(diretorio));
        System.out.println("=== Servidor de Arquivo iniciado ===");
        System.out.println("Endereço : " + InetAddress.getLocalHost().getHostAddress() + ":" + PortaTCPAtual);
        System.out.println("Arquivos disponíveis:");
        Files.list(Paths.get(diretorio))
                .filter(Files::isRegularFile)
                .forEach(p -> System.out.println("  → " + p.getFileName()));
        System.out.println("=====================================\n");

        new Thread(() -> escutarMulticast(PortaTCPFinal)).start();
        try (ServerSocket serverSocket = new ServerSocket(PortaTCPAtual)) {
            while (true) {
                Socket clienteSocket = serverSocket.accept();
                new Thread(new DownloadHandler(clienteSocket)).start();
            }
        }
    }
}

